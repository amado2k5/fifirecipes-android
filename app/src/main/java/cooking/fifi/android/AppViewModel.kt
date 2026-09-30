package cooking.fifi.android

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cooking.fifi.android.data.ApiClient
import cooking.fifi.android.data.Chapter
import cooking.fifi.android.data.DeepLinks
import cooking.fifi.android.data.Feed
import cooking.fifi.android.data.ImageInfo
import cooking.fifi.android.data.KidsCard
import cooking.fifi.android.data.LanguageInfo
import cooking.fifi.android.data.RecipeCard
import cooking.fifi.android.data.Route
import cooking.fifi.android.data.Section
import cooking.fifi.android.data.Strings
import cooking.fifi.android.data.TvManifest
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

enum class Phase { Loading, PickingLanguage, Ready, Failed }

/**
 * App-wide state — the Android twin of the iOS AppState: manifest → language
 * picker (first run) → sections, each with its own back stack so switching
 * sections never loses your place. Back stacks survive rotation, folding and
 * resizing (ViewModel) and process death (SavedStateHandle).
 */
class AppViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val fifi = app as FifiApplication
    val api: ApiClient = fifi.api
    private val prefs = app.getSharedPreferences("fifi", Context.MODE_PRIVATE)

    var phase by mutableStateOf(Phase.Loading); private set
    var manifest by mutableStateOf<TvManifest?>(null); private set
    var lang by mutableStateOf(prefs.getString(KEY_LANG, null) ?: "en"); private set
    var index by mutableStateOf<Map<String, RecipeCard>>(emptyMap()); private set
    var feed by mutableStateOf(Feed()); private set
    var chapters by mutableStateOf<List<Chapter>>(emptyList()); private set
    var kids by mutableStateOf<Map<String, KidsCard>>(emptyMap()); private set
    var images by mutableStateOf<Map<String, ImageInfo>>(emptyMap()); private set

    var section by mutableStateOf(saved.get<String>(KEY_SECTION)?.let { runCatching { Section.valueOf(it) }.getOrNull() } ?: Section.Home)
        private set
    private val stacks: Map<Section, SnapshotStateList<Route>> = Section.entries.associateWith { s ->
        mutableStateListOf<Route>().apply {
            saved.get<ArrayList<String>>("stack_${s.name}")?.mapNotNullTo(this) { Route.decode(it) }
        }
    }

    /** Language being loaded (setLanguage in flight) — so a resume mid-load doesn't start it again. */
    private var requestedLang: String? = null

    /** Deep link that arrived before data was ready — applied once Ready. */
    private var pendingLink: String? = null

    val strings: Strings get() = fifi.strings.forLang(lang)
    val langInfo: LanguageInfo? get() = manifest?.languages?.firstOrNull { it.code == lang }
    val isRtl: Boolean get() = langInfo?.isRtl ?: (lang in RTL_FALLBACK)

    fun stack(s: Section): List<Route> = stacks.getValue(s)
    val currentStack: List<Route> get() = stacks.getValue(section)

    fun assetUrl(path: String?): String? = ApiClient.resolveAsset(path, manifest?.version, api.origin)

    // region Boot + language

    fun start() {
        if (phase == Phase.Loading && manifest == null) viewModelScope.launch { bootstrap() }
    }

    fun retry() {
        phase = Phase.Loading
        api.clearCache()
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        try {
            val m = manifest ?: api.loadManifest().also { manifest = it }
            val stored = systemAppLanguage() ?: prefs.getString(KEY_LANG, null)
            if (stored == null || m.languages.none { it.code == stored }) {
                phase = Phase.PickingLanguage
                return
            }
            loadLanguage(stored)
            phase = Phase.Ready
            applyPendingLink()
        } catch (e: Exception) {
            phase = Phase.Failed
        }
    }

    fun setLanguage(code: String) {
        prefs.edit { putString(KEY_LANG, code) }
        // Mirror into Android 13+ per-app language so Settings › App languages agrees.
        if (Build.VERSION.SDK_INT >= 33) {
            val lm = getApplication<Application>().getSystemService(LocaleManager::class.java)
            if (lm?.applicationLocales?.toLanguageTags() != code) {
                lm?.applicationLocales = LocaleList.forLanguageTags(code)
            }
        }
        if (code == lang && phase == Phase.Ready) return
        if (code == requestedLang) return
        requestedLang = code
        phase = Phase.Loading
        viewModelScope.launch {
            try {
                loadLanguage(code)
                phase = Phase.Ready
                applyPendingLink()
            } catch (e: Exception) {
                phase = Phase.Failed
            } finally {
                requestedLang = null
            }
        }
    }

    /** Called when the per-app language changes from system Settings. */
    fun syncSystemLanguage() {
        val sys = systemAppLanguage() ?: return
        if (sys != (requestedLang ?: lang) && manifest?.languages?.any { it.code == sys } == true) setLanguage(sys)
    }

    private fun systemAppLanguage(): String? {
        if (Build.VERSION.SDK_INT < 33) return null
        val lm = getApplication<Application>().getSystemService(LocaleManager::class.java) ?: return null
        val loc = lm.applicationLocales.takeIf { !it.isEmpty }?.get(0) ?: return null
        return normalizeLanguage(loc.language)
    }

    private suspend fun loadLanguage(code: String) {
        val i = viewModelScope.async { api.index(code) }
        val f = viewModelScope.async { api.feed(code) }
        val c = viewModelScope.async { api.chapters(code) }
        val k = viewModelScope.async { api.kids(code) }
        val im = viewModelScope.async { runCatching { api.images() }.getOrNull() }
        val newIndex = i.await().associateBy { it.id }
        val newFeed = f.await()
        val newChapters = c.await()
        val newKids = k.await().associateBy { it.id }
        lang = code
        index = newIndex
        feed = newFeed
        chapters = newChapters
        kids = newKids
        im.await()?.let { images = it }
    }

    // endregion

    // region Navigation

    fun select(s: Section) {
        // Re-selecting the current section pops it to its root (Material convention).
        if (s == section) stacks.getValue(s).clear() else section = s
        persist()
    }

    fun push(route: Route) {
        stacks.getValue(section).add(route)
        persist()
    }

    /** @return false when there is nothing left to pop (let the system finish the activity). */
    fun back(): Boolean {
        val st = stacks.getValue(section)
        when {
            st.isNotEmpty() -> st.removeAt(st.lastIndex)
            section != Section.Home -> section = Section.Home
            else -> return false
        }
        persist()
        return true
    }

    fun popToRoot() {
        stacks.getValue(section).clear()
        persist()
    }

    fun handleLink(path: String?) {
        pendingLink = path
        if (phase == Phase.Ready) applyPendingLink()
    }

    private fun applyPendingLink() {
        val link = pendingLink ?: return
        pendingLink = null
        val (s, route) = DeepLinks.parse(link) ?: return
        section = s
        stacks.getValue(s).apply { clear(); add(route) }
        persist()
    }

    private fun persist() {
        saved[KEY_SECTION] = section.name
        for ((s, st) in stacks) saved["stack_${s.name}"] = ArrayList(st.map { it.encode() })
    }

    // endregion

    companion object {
        private const val KEY_LANG = "language"
        private const val KEY_SECTION = "section"
        val RTL_FALLBACK = setOf("ar", "ur", "fa", "ps", "he")

        /** Java's legacy ISO codes → the codes the manifest uses. */
        fun normalizeLanguage(code: String): String = when (code) {
            "iw" -> "he"
            "in" -> "id"
            else -> code
        }
    }
}
