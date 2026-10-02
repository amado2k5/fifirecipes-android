package cooking.fifi.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import cooking.fifi.android.ui.theme.Palette

/**
 * Remote photo over the branded produce placeholder (the TV app's Img): the
 * tomato shows until Coil's crossfade covers it, and stays on failure.
 * Decorative — the surrounding card carries the accessible label.
 */
@Composable
fun RemoteImage(url: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    // When the URL upgrades (800px card -> full-size once images.json arrives),
    // keep the image already on screen until the sharper one is ready.
    var shown by remember { mutableStateOf<String?>(null) }
    Box(modifier.background(Palette.leafSoft), contentAlignment = Alignment.Center) {
        TomatoMark(Modifier.size(44.dp))
        if (url != null) {
            val request = remember(url, shown) {
                ImageRequest.Builder(context)
                    .data(url)
                    .placeholderMemoryCacheKey(shown?.takeIf { it != url })
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { shown = url },
            )
        }
    }
}

/** Little tomato glyph matching the TV app's placeholder SVG. */
@Composable
fun TomatoMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.30f
        drawCircle(Color(0xFFFF5A4E), r, Offset(w / 2, h * 0.56f), alpha = 0.85f)
        val leaf = Path().apply {
            moveTo(w * 0.37f, h * 0.30f); lineTo(w * 0.45f, h * 0.32f); lineTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.55f, h * 0.32f); lineTo(w * 0.63f, h * 0.30f); lineTo(w * 0.57f, h * 0.38f)
            quadraticTo(w * 0.50f, h * 0.42f, w * 0.43f, h * 0.38f); close()
        }
        drawPath(leaf, Color(0xFF4CAF50), alpha = 0.85f)
    }
}

/** Warm paper backdrop used under every screen. */
@Composable
fun PaperBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Palette.paper, Palette.paperDeep)))
        val r = 420.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(Color(0x80FFCD82), Color.Transparent), Offset(size.width, 0f), r), r, Offset(size.width, 0f))
        drawCircle(Brush.radialGradient(listOf(Color(0x73C4E6A0), Color.Transparent), Offset(0f, size.height), r), r, Offset(0f, size.height))
    }
}

/** Kids-mode drawing from assets/kids-art (pre-rendered from the TV SVG library); unknown ids → star. */
@Composable
fun KidsArt(id: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val known = remember { KidsArtIndex.ids(context) }
    val resolved = if (id in known) id else "star"
    AsyncImage(
        model = "file:///android_asset/kids-art/$resolved.webp",
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

private object KidsArtIndex {
    @Volatile private var cache: Set<String>? = null
    fun ids(context: android.content.Context): Set<String> = cache ?: (
        context.assets.list("kids-art").orEmpty().map { it.removeSuffix(".webp") }.toSet().also { cache = it }
    )
}
