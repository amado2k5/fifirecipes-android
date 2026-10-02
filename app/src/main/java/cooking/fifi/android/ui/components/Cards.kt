package cooking.fifi.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cooking.fifi.android.data.ApiClient
import cooking.fifi.android.data.Chapter
import cooking.fifi.android.data.KidsCard
import cooking.fifi.android.data.RecipeCard
import cooking.fifi.android.data.S
import cooking.fifi.android.data.VideoItem
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.shareLink
import cooking.fifi.android.ui.theme.KidsGroupStyle
import cooking.fifi.android.ui.theme.KidsPalette
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

/**
 * Tappable card surface with the large-screen affordances Google's
 * large-screen quality guidelines ask for:
 *   * touch ripple + 48dp+ targets
 *   * mouse/trackpad hover lift, keyboard/D-pad focus ring (tomato outline)
 *   * context menu from long-press, right-click, or Shift+F10 / Menu key,
 *     with the system share sheet
 *   * TalkBack: one merged node with the given label, "double-tap to open",
 *     and a Share custom action
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    share: Pair<String, String>? = null, // title to URL
    borderColor: Color = Palette.cardBorder,
    borderWidth: Dp = 1.5.dp,
    background: Color = Palette.card,
    shadowColor: Color = Palette.ink,
    testTag: String? = null,
    content: @Composable () -> Unit,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    var menu by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (hovered) 1.03f else 1f, label = "hover")
    val doShare = { share?.let { (t, u) -> shareLink(context, t, u) } }

    Box(
        modifier
            .graphicsLayer { scaleX = lift; scaleY = lift }
            .shadow(if (hovered) 12.dp else 5.dp, shape, ambientColor = shadowColor.copy(alpha = 0.25f), spotColor = shadowColor.copy(alpha = 0.25f))
            .clip(shape)
            .background(background)
            .border(if (focused) 3.dp else borderWidth, if (focused) Palette.tomato else borderColor, shape)
            .pointerInput(share) {
                if (share == null) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type == PointerEventType.Press && e.buttons.isSecondaryPressed) {
                            e.changes.forEach { it.consume() }
                            menu = true
                        }
                    }
                }
            }
            .onPreviewKeyEvent {
                val isMenuKey = it.key == Key.Menu || (it.key == Key.F10 && it.isShiftPressed)
                if (share != null && isMenuKey && it.type == KeyEventType.KeyUp) { menu = true; true } else false
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
                onLongClick = if (share != null) ({ menu = true }) else null,
            )
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick(s[S.open]) { onClick(); true }
                if (share != null) {
                    onLongClick(s[S.share]) { menu = true; true }
                    customActions = listOf(CustomAccessibilityAction(s[S.share]) { doShare(); true })
                }
                if (testTag != null) this[androidx.compose.ui.semantics.SemanticsProperties.TestTag] = testTag
            },
    ) {
        content()
        if (share != null) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(s[S.open], style = fifi(TS.Body, W.Medium)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                    onClick = { menu = false; onClick() },
                )
                DropdownMenuItem(
                    text = { Text(s[S.share], style = fifi(TS.Body, W.Medium)) },
                    leadingIcon = { Icon(Icons.Filled.Share, null) },
                    onClick = { menu = false; doShare() },
                )
            }
        }
    }
}

/**
 * Tomato focus ring for rows and chips reached by keyboard/D-pad (matches the
 * TV apps). Place before the clickable/toggleable modifier so it sees focus.
 */
fun Modifier.focusRing(shape: Shape, width: Dp = 3.dp): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.hasFocus }
        .border(if (focused) width else 0.dp, if (focused) Palette.tomato else Color.Transparent, shape)
}

enum class ChipTone(val bg: Color, val fg: Color) {
    Leaf(Palette.leafSoft, Palette.leafDeep),
    Tomato(Palette.tomatoSoft, Palette.tomatoDeep),
    Sun(Palette.sunSoft, Palette.ink),
}

@Composable
fun MetaChip(text: String, modifier: Modifier = Modifier, tone: ChipTone = ChipTone.Leaf, label: String? = null) {
    Row(
        modifier
            .background(tone.bg, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label != null) Text(label, style = fifi(TS.Subheadline, W.Medium), color = tone.fg.copy(alpha = 0.75f))
        Text(text, style = fifi(TS.Subheadline, W.Medium), color = tone.fg)
    }
}

/** Recipe card used in rails, grids and search results. */
@Composable
fun RecipeCardView(card: RecipeCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    // card2x beats the 800px thumbnail on high-density screens when present.
    val url = app.assetUrl(app.images[card.id]?.card2x ?: card.image)
    val meta = card.cookingMethod ?: card.category
    InteractiveCard(
        label = listOfNotNull(card.title, meta).joinToString(", "),
        onClick = onClick,
        share = card.title to ApiClient.shareUrl(card.id),
        modifier = modifier,
        testTag = "recipe-${card.id}",
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                RemoteImage(url, Modifier.fillMaxSize())
                if (card.hasVideo == true) {
                    Icon(
                        Icons.Filled.PlayArrow, null, tint = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp)
                            .background(Palette.tomato, CircleShape).padding(5.dp),
                    )
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(card.title, style = fifi(TS.Headline, W.Bold), color = Palette.ink, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                Text(meta ?: " ", style = fifi(TS.Footnote), color = Palette.inkDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ChapterCardView(chapter: Chapter, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    InteractiveCard(label = "${chapter.name}, ${chapter.recipeCount}", onClick = onClick, modifier = modifier, testTag = "chapter-${chapter.id}") {
        Column {
            RemoteImage(app.assetUrl(chapter.coverImage), Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(chapter.name, style = fifi(TS.Headline, W.Bold), color = Palette.ink, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${chapter.recipeCount}", style = fifi(TS.Footnote, W.Medium), color = Palette.leafDeep,
                    modifier = Modifier.background(Palette.leafSoft, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun KidsCardView(card: KidsCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val style = KidsGroupStyle.forGroup(card.group)
    val meta = buildString {
        append("${card.ages} · ${card.minutes} ${s[S.minutesShort]}")
        if (card.noCook) append(" · ❄")
    }
    InteractiveCard(
        label = "${card.title}, $meta",
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        borderColor = style.border,
        borderWidth = 3.dp,
        background = style.card,
        shadowColor = KidsPalette.ink,
        modifier = modifier,
        testTag = "kids-${card.id}",
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp, start = 8.dp, end = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            KidsArt(card.cover, Modifier.size(108.dp).background(Color.White.copy(alpha = 0.9f), CircleShape).padding(10.dp))
            Spacer(Modifier.size(8.dp))
            Text(card.title, style = fifi(TS.Callout, W.Bold), color = KidsPalette.ink, textAlign = TextAlign.Center, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Text(meta, style = fifi(TS.Footnote, W.Medium), color = KidsPalette.dim, maxLines = 1)
        }
    }
}

@Composable
fun VideoCardView(video: VideoItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    InteractiveCard(
        label = video.title, onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.width(240.dp),
        share = video.title to "https://www.youtube.com/watch?v=${video.id}",
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                RemoteImage("https://i.ytimg.com/vi/${video.id}/hqdefault.jpg", Modifier.fillMaxSize())
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(52.dp).background(Palette.tomato.copy(alpha = 0.92f), CircleShape).padding(10.dp))
                video.duration?.let {
                    Text(
                        it, style = fifi(TS.Caption, W.Bold), color = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(video.title, style = fifi(TS.Subheadline, W.Medium), color = Palette.ink, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                Text(video.channel ?: " ", style = fifi(TS.Footnote), color = Palette.inkDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
