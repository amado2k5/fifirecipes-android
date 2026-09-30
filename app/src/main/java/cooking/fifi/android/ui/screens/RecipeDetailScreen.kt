package cooking.fifi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cooking.fifi.android.data.ApiClient
import cooking.fifi.android.data.RecipeFile
import cooking.fifi.android.data.RecipeLocalization
import cooking.fifi.android.data.S
import cooking.fifi.android.data.VideoItem
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.components.ChipTone
import cooking.fifi.android.ui.components.ErrorView
import cooking.fifi.android.ui.components.LoadingView
import cooking.fifi.android.ui.components.MetaChip
import cooking.fifi.android.ui.components.RemoteImage
import cooking.fifi.android.ui.components.SectionTitle
import cooking.fifi.android.ui.components.VideoCardView
import cooking.fifi.android.ui.components.VideoDialog
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

private sealed interface Load {
    data object Loading : Load
    data object Failed : Load
    data class Done(val file: RecipeFile) : Load
}

/**
 * Full recipe — hero, meta chips, nutrition, tappable ingredient checklist,
 * numbered steps, alternative methods, tips, "From Fatma's notebook" and
 * videos. Hero beside the title from 600dp; ingredients beside steps from
 * 840dp (tablets, unfolded foldables, ChromeOS); one column otherwise.
 */
@Composable
fun RecipeDetailScreen(id: String, pad: PaddingValues) {
    val app = LocalApp.current
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    val state by produceState<Load>(Load.Loading, id, attempt) {
        value = Load.Loading
        value = runCatching { app.api.recipe(id) }.fold({ Load.Done(it) }, { Load.Failed })
    }
    val videos by produceState<List<VideoItem>>(emptyList(), id, app.lang) {
        value = runCatching { ApiClient.pickVideos(app.api.videos(id), app.lang) }.getOrDefault(emptyList())
    }
    when (val st = state) {
        Load.Loading -> LoadingView(Modifier.padding(pad))
        Load.Failed -> ErrorView(onRetry = { attempt++ }, modifier = Modifier.padding(pad))
        is Load.Done -> RecipeContent(id, st.file, videos, pad)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeContent(id: String, file: RecipeFile, videos: List<VideoItem>, pad: PaddingValues) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val loc = remember(file, app.lang) { RecipeLocalization.localize(file, app.lang) }
    var ticked by rememberSaveable(id) { mutableStateOf(emptySet<Int>()) }
    var shownVideo by remember { mutableStateOf<VideoItem?>(null) }
    var zoomed by rememberSaveable { mutableStateOf(false) }
    val info = app.images[id]
    val heroUrl = app.assetUrl(info?.full2x ?: info?.full ?: app.index[id]?.image)

    val hero = @Composable { height: androidx.compose.ui.unit.Dp, mod: Modifier ->
        Box(
            mod.height(height).clip(RoundedCornerShape(20.dp))
                .clickable(role = Role.Image, onClickLabel = loc.title) { zoomed = true }
                .semantics { contentDescription = loc.title }
                .testTag("recipeHero"),
        ) {
            RemoteImage(heroUrl, Modifier.fillMaxSize())
            Icon(
                Icons.Filled.OpenInFull, null, tint = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(34.dp).background(Palette.ink.copy(alpha = 0.5f), CircleShape).padding(8.dp),
            )
        }
    }

    val meta = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            loc.chapter?.let { Text(it, style = fifi(TS.Subheadline, W.Bold), color = Palette.leafDeep) }
            Text(loc.title, style = fifi(TS.LargeTitle, W.Heavy), color = Palette.ink, modifier = Modifier.testTag("recipeTitle"))
            loc.subtitle?.let { Text(it, style = fifi(TS.Title3).copy(fontStyle = FontStyle.Italic), color = Palette.inkDim) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                loc.prepTime?.let { MetaChip(it, tone = ChipTone.Leaf, label = s[S.prep]) }
                loc.cookTime?.let { MetaChip(it, tone = ChipTone.Tomato, label = s[S.cook]) }
                loc.servings?.let { MetaChip(it, tone = ChipTone.Sun, label = s[S.servings]) }
                loc.category?.let { MetaChip(it, tone = ChipTone.Leaf) }
                loc.cookingMethod?.let { MetaChip(it, tone = ChipTone.Tomato) }
            }
            file.estimate?.let { e ->
                val parts = listOfNotNull(
                    e.kcal?.let { "$it ${s[S.kcal]}" }, e.protein?.let { "${it}g ${s[S.protein]}" },
                    e.carbs?.let { "${it}g ${s[S.carbs]}" }, e.fat?.let { "${it}g ${s[S.fat]}" },
                )
                if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = fifi(TS.Footnote), color = Palette.inkDim)
            }
        }
    }

    val ingredients = @Composable { mod: Modifier ->
        Column(mod, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(s[S.ingredients], Palette.leaf)
            Text(s[S.tickHint], style = fifi(TS.Footnote), color = Palette.inkDim)
            val shape = RoundedCornerShape(18.dp)
            Column(Modifier.fillMaxWidth().clip(shape).background(Palette.card).border(1.5.dp, Palette.cardBorder, shape)) {
                loc.ingredients.forEachIndexed { i, ing ->
                    val on = i in ticked
                    Row(
                        Modifier.fillMaxWidth()
                            .toggleable(value = on, role = Role.Checkbox) { ticked = if (on) ticked - i else ticked + i }
                            .heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("ingredient-$i"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(if (on) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null, tint = if (on) Palette.leaf else Palette.inkDim.copy(alpha = 0.5f), modifier = Modifier.size(26.dp))
                        Text(
                            ing.name, style = fifi(TS.Body, W.Medium).copy(textDecoration = if (on) TextDecoration.LineThrough else null),
                            color = Palette.ink.copy(alpha = if (on) 0.6f else 1f), modifier = Modifier.weight(1f),
                        )
                        // Name and amount share the row — neither may starve the other.
                        ing.amount?.let {
                            Text(it, style = fifi(TS.Body, W.Bold), color = Palette.leafDeep, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f, fill = false))
                        }
                    }
                    if (i < loc.ingredients.lastIndex) HorizontalDivider(color = Palette.cardBorder, modifier = Modifier.padding(start = 14.dp))
                }
            }
        }
    }

    val steps = @Composable { mod: Modifier ->
        Column(mod, verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(s[S.steps], Palette.tomato)
                loc.coreSteps.forEach { st ->
                    Row(
                        Modifier.fillMaxWidth().card().padding(14.dp).semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(34.dp).background(Palette.leafDeep, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${st.n}", style = fifi(TS.Headline, W.Heavy), color = Color.White)
                        }
                        Text(st.text, style = fifi(TS.Body), color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (loc.alternativeGroups.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(s[S.alternativeMethods], Palette.berry)
                    loc.alternativeGroups.forEach { (label, group) ->
                        group.forEach { st ->
                            val shape = RoundedCornerShape(18.dp)
                            Column(
                                Modifier.fillMaxWidth().clip(shape).background(Palette.tomatoSoft.copy(alpha = 0.5f))
                                    .border(1.5.dp, Palette.berry.copy(alpha = 0.3f), shape).padding(14.dp)
                                    .semantics(mergeDescendants = true) {},
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(label, style = fifi(TS.Footnote, W.Bold), color = Palette.berryDeep, modifier = Modifier.background(Palette.berry.copy(alpha = 0.15f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp))
                                Text(st.text, style = fifi(TS.Body), color = Palette.ink)
                            }
                        }
                    }
                }
            }
            if (loc.tips.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(s[S.tips], Palette.sun)
                    loc.tips.forEach { st ->
                        Text("💡 ${st.text}", style = fifi(TS.Body, W.Medium), color = Palette.ink, modifier = Modifier.fillMaxWidth().card(Palette.sunSoft).padding(14.dp))
                    }
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Side-by-side hero from 600dp; two body columns only from 840dp so
        // the ingredients column never drops below a readable ~320dp.
        val wideHeader = maxWidth >= 600.dp
        val wide = maxWidth >= 840.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad).padding(top = 8.dp, bottom = 32.dp).testTag("recipeDetail"),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.contentWidth(1240.dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (wideHeader) {
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        hero(340.dp, Modifier.weight(0.45f))
                        Box(Modifier.weight(0.55f).padding(top = 6.dp)) { meta() }
                    }
                } else {
                    hero(240.dp, Modifier.fillMaxWidth())
                    meta()
                }
                loc.culturalNotes?.let { notes ->
                    Row(Modifier.fillMaxWidth().card(Palette.sunSoft)) {
                        Spacer(Modifier.width(5.dp).heightIn(min = 60.dp).background(Palette.sun))
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(s[S.culturalNotes], style = fifi(TS.Headline, W.Bold), color = Palette.leafDeep)
                            Text(notes, style = fifi(TS.Body), color = Palette.ink)
                        }
                    }
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        ingredients(Modifier.weight(0.4f).widthIn(min = 280.dp))
                        steps(Modifier.weight(0.6f))
                    }
                } else {
                    ingredients(Modifier.fillMaxWidth())
                    steps(Modifier.fillMaxWidth())
                }
            }
            if (videos.isNotEmpty()) {
                Column(Modifier.contentWidth(1240.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(s[S.videos], Palette.tomato, Modifier.padding(horizontal = 16.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(videos, key = { it.id }) { v -> VideoCardView(v, onClick = { shownVideo = v }) }
                    }
                }
            }
        }
    }

    shownVideo?.let { VideoDialog(it, onDismiss = { shownVideo = null }) }
    if (zoomed) ZoomImageDialog(heroUrl, loc.title, onDismiss = { zoomed = false })
}

private fun Modifier.card(bg: Color = Palette.card): Modifier {
    val shape = RoundedCornerShape(18.dp)
    return clip(shape).background(bg).border(1.5.dp, Palette.cardBorder, shape)
}

/** Full-screen photo: pinch/trackpad zoom, drag to pan, double-tap to toggle 2.5×. */
@Composable
private fun ZoomImageDialog(url: String?, title: String, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transform = rememberTransformableState { zoom, pan, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            offset = if (scale == 1f) Offset.Zero else offset + pan
        }
        Box(Modifier.fillMaxSize().background(Color.Black).testTag("zoomDialog")) {
            RemoteImage(
                url,
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f })
                    }
                    .transformable(transform)
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                    .semantics { contentDescription = title },
                contentScale = ContentScale.Fit,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.safeDrawingPadding().padding(8.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) { Icon(Icons.Filled.Close, s[S.close], tint = Color.White) }
        }
    }
}
