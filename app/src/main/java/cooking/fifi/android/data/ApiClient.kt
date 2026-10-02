package cooking.fifi.android.data

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class HttpException(val code: Int, val url: String) : IOException("HTTP $code for $url")

/**
 * Retries GETs that fail transiently — dropped mobile connections, timeouts,
 * 5xx — twice with backoff. Shared by the JSON API and the Coil image loader,
 * so a flaky cellular hiccup no longer leaves a recipe photo stuck on the
 * placeholder (the iOS apps fixed the same symptom). Cancelled calls (screen
 * scrolled away) are never retried.
 */
class RetryInterceptor(private val retries: Int = 2, private val baseDelayMs: Long = 400) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)
        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(request)
                if (response.code < 500 || attempt >= retries) return response
                response.close()
            } catch (e: IOException) {
                if (chain.call().isCanceled() || attempt >= retries) throw e
            }
            Thread.sleep(baseDelayMs * (1L shl attempt)) // 400ms, 800ms
            attempt++
        }
    }
}

/**
 * Static JSON client for https://fifi.cooking/data/ — mirrors the TV/iOS
 * clients: endpoint templates come from the manifest (with defaults), every
 * data request is versioned with ?v=<manifest.version>, and bodies flow through
 * a disk HTTP cache plus in-flight de-duplication.
 */
class ApiClient(
    val http: OkHttpClient,
    val origin: String = DEFAULT_ORIGIN,
) {
    @Volatile var manifest: TvManifest? = null
        private set
    private val endpoints get() = manifest?.endpoints ?: EndpointTemplates()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inflightLock = Mutex()
    private val inflight = mutableMapOf<String, Deferred<String>>()

    suspend fun loadManifest(): TvManifest {
        val m = decode(TvManifest.serializer(), fetch("$origin/data/tv/manifest.json", dedupe = false))
        manifest = m
        return m
    }

    fun versioned(path: String): String = versionedPath(path, manifest?.version)

    fun assetUrl(path: String?): String? = resolveAsset(path, manifest?.version, origin)

    suspend fun index(lang: String) =
        get(ListSerializer(RecipeCard.serializer()), endpoints.index, "lang" to lang)

    suspend fun feed(lang: String) = get(Feed.serializer(), endpoints.feed, "lang" to lang)

    suspend fun chapters(lang: String) =
        get(ListSerializer(Chapter.serializer()), endpoints.chapters, "lang" to lang)

    /** Kids catalogue — a 404 means "no kids content in this language": use English. */
    suspend fun kids(lang: String): List<KidsCard> = try {
        get(ListSerializer(KidsCard.serializer()), endpoints.kids, "lang" to lang)
    } catch (e: HttpException) {
        if (e.code != 404) throw e
        get(ListSerializer(KidsCard.serializer()), endpoints.kids, "lang" to "en")
    }

    suspend fun kidsRecipe(lang: String, id: String): KidsRecipeDetail = try {
        get(KidsRecipeDetail.serializer(), endpoints.kidsRecipe, "lang" to lang, "id" to id)
    } catch (e: HttpException) {
        if (e.code != 404) throw e
        get(KidsRecipeDetail.serializer(), endpoints.kidsRecipe, "lang" to "en", "id" to id)
    }

    suspend fun recipe(id: String) = get(RecipeFile.serializer(), endpoints.recipe, "id" to id)

    suspend fun videos(id: String): Map<String, List<VideoItem>> =
        get(MapSerializer(String.serializer(), ListSerializer(VideoItem.serializer())), endpoints.videos, "id" to id)

    suspend fun search(lang: String): Map<String, String> =
        get(MapSerializer(String.serializer(), String.serializer()), endpoints.search, "lang" to lang)

    suspend fun images(): Map<String, ImageInfo> =
        get(MapSerializer(String.serializer(), ImageInfo.serializer()), endpoints.images)

    fun clearCache() {
        runCatching { http.cache?.evictAll() }
    }

    private suspend fun <T> get(
        strategy: DeserializationStrategy<T>,
        template: String,
        vararg vars: Pair<String, String>,
    ): T = decode(strategy, fetch(origin + versioned(fillTemplate(template, vars.toMap()))))

    private suspend fun fetch(url: String, dedupe: Boolean = true): String {
        if (!dedupe) return download(url)
        val job = inflightLock.withLock {
            inflight[url] ?: scope.async { download(url) }.also { inflight[url] = it }
        }
        return try {
            job.await()
        } finally {
            inflightLock.withLock { if (inflight[url] === job) inflight.remove(url) }
        }
    }

    private suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpException(resp.code, url)
            resp.body.string()
        }
    }

    private fun <T> decode(strategy: DeserializationStrategy<T>, body: String): T = json.decodeFromString(strategy, body)

    companion object {
        const val DEFAULT_ORIGIN = "https://fifi.cooking"

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
            isLenient = true
        }

        fun create(context: Context, origin: String = DEFAULT_ORIGIN): ApiClient {
            val http = OkHttpClient.Builder()
                .cache(Cache(File(context.cacheDir, "http"), 128L shl 20))
                .addInterceptor(RetryInterceptor())
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            return ApiClient(http, origin)
        }

        /** `{lang}`/`{id}` substitution with path-segment encoding. */
        fun fillTemplate(template: String, vars: Map<String, String>): String =
            vars.entries.fold(template) { t, (k, v) ->
                t.replace("{$k}", URLEncoder.encode(v, "UTF-8").replace("+", "%20"))
            }

        fun versionedPath(path: String, version: String?): String =
            if (version.isNullOrEmpty()) path else "$path?v=${URLEncoder.encode(version, "UTF-8")}"

        /** Site-relative asset path (/recipe-images/x.jpg) or absolute URL → absolute, versioned URL. */
        fun resolveAsset(path: String?, version: String?, origin: String = DEFAULT_ORIGIN): String? {
            if (path.isNullOrEmpty()) return null
            if (path.startsWith("http://") || path.startsWith("https://")) return path
            return origin + versionedPath(path, version)
        }

        /** The language bucket the TV app uses: requested, else ar, else en. */
        fun pickVideos(file: Map<String, List<VideoItem>>, lang: String): List<VideoItem> =
            file[lang] ?: file["ar"] ?: file["en"] ?: emptyList()

        /** Public recipe URL — the path App Links / Universal Links claim. */
        fun shareUrl(recipeId: String) = "$DEFAULT_ORIGIN/recipe/${URLEncoder.encode(recipeId, "UTF-8")}/"
    }
}
