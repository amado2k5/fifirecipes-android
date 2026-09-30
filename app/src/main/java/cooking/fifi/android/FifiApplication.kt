package cooking.fifi.android

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import cooking.fifi.android.data.ApiClient
import cooking.fifi.android.data.UiStrings
import okio.Path.Companion.toOkioPath

/** Process-wide singletons: API client (shared OkHttp + disk cache), strings, image loader. */
class FifiApplication : Application(), SingletonImageLoader.Factory {
    val api: ApiClient by lazy { ApiClient.create(this) }
    val strings: UiStrings by lazy { UiStrings.load(this) }

    override fun onCreate() {
        super.onCreate()
        // chrome://inspect for the video player WebView — debug builds only.
        if (BuildConfig.DEBUG) android.webkit.WebView.setWebContentsDebuggingEnabled(true)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { api.http })) }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.20).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images").toOkioPath())
                    .maxSizeBytes(256L shl 20)
                    .build()
            }
            .crossfade(true)
            .build()
}
