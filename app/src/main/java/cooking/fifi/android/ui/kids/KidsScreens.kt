package cooking.fifi.android.ui.kids

import androidx.activity.compose.LocalActivity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cooking.fifi.android.data.KidsRecipeDetail
import cooking.fifi.android.data.KidsStep
import cooking.fifi.android.data.Route
import cooking.fifi.android.data.S
import cooking.fifi.android.data.UiStrings
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.LocalWidthClass
import cooking.fifi.android.ui.WidthClass
import cooking.fifi.android.ui.components.ErrorView
import cooking.fifi.android.ui.components.KidsArt
import cooking.fifi.android.ui.components.KidsCardView
import cooking.fifi.android.ui.components.LoadingView
import cooking.fifi.android.ui.screens.contentWidth
import cooking.fifi.android.ui.screens.plus
import cooking.fifi.android.ui.theme.KidsGroupStyle
import cooking.fifi.android.ui.theme.KidsPalette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GROUPS = listOf("breakfast", "snack", "savoury", "sweet", "drink")

private fun groupLabel(s: cooking.fifi.android.data.Strings, g: String) = when (g) {
    "breakfast" -> s[S.groupBreakfast]
    "snack" -> s[S.groupSnack]
    "savoury" -> s[S.groupSavoury]
    "sweet" -> s[S.groupSweet]
    "drink" -> s[S.groupDrink]
    else -> g
}

/** Kids home: rainbow strip, chef header, group filter chips, card grid. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KidsScreen(pad: PaddingValues) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val wide = LocalWidthClass.current != WidthClass.Compact
    var group by rememberSaveable { mutableStateOf("all") }
    var noCookOnly by rememberSaveable { mutableStateOf(false) }
    val cards = remember(app.kids, group, noCookOnly) {
        app.kids.values.filter { (group == "all" || it.group == group) && (!noCookOnly || it.noCook) }.sortedBy { it.id }
    }
    KidsScope {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(if (wide) 200.dp else 160.dp),
            modifier = Modifier.fillMaxSize().testTag("kidsScreen"),
            contentPadding = pad.plus(horizontal = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RainbowBar()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        KidsArt("chef", Modifier.size(64.dp))
                        Column {
                            Text(s[S.kids], style = fifi(TS.LargeTitle, W.Heavy), color = KidsPalette.ink, modifier = Modifier.semantics { heading() })
                            Text(s[S.tagline], style = fifi(TS.Subheadline, W.Medium), color = KidsPalette.dim)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KidsChip("all", "🌈 ${s[S.all]}", group == "all" && !noCookOnly, KidsPalette.purple) { group = "all"; noCookOnly = false }
                        GROUPS.forEach { g ->
                            val st = KidsGroupStyle.forGroup(g)
                            KidsChip(g, "${st.emoji} ${groupLabel(s, g)}", group == g && !noCookOnly, st.chip) { group = g; noCookOnly = false }
                        }
                        KidsChip("nocook", "❄️ ${s[S.noCook]}", noCookOnly, Color(0xFF0277BD)) { noCookOnly = !noCookOnly }
                    }
                }
            }
            items(cards, key = { it.id }) { card ->
                KidsCardView(card, onClick = { app.push(Route.KidsReady(card.id)) })
            }
        }
    }
}

@Composable
private fun KidsChip(id: String, label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        label,
        style = fifi(TS.Headline, W.Bold),
        color = if (active) Color.White else KidsPalette.chipText,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) color else Color.White.copy(alpha = 0.85f))
            .border(3.dp, if (active) color else KidsPalette.chipIdle, CircleShape)
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("kidsFilter-$id"),
    )
}

private sealed interface KLoad {
    data object Loading : KLoad
    data object Failed : KLoad
    data class Done(val r: KidsRecipeDetail) : KLoad
}

@Composable
private fun rememberKidsRecipe(id: String): Pair<KLoad, () -> Unit> {
    val app = LocalApp.current
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    val state by produceState<KLoad>(KLoad.Loading, id, app.lang, attempt) {
        value = KLoad.Loading
        value = runCatching { app.api.kidsRecipe(app.lang, id) }.fold({ KLoad.Done(it) }, { KLoad.Failed })
    }
    return state to { attempt++ }
}

/** "Get ready": hero, ready checklist, tickable ingredients, tools, allergens, steps preview, Let's cook. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KidsReadyScreen(id: String, pad: PaddingValues) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val (state, retry) = rememberKidsRecipe(id)
    var ticked by rememberSaveable(id) { mutableStateOf(emptySet<Int>()) }
    KidsScope {
        when (state) {
            KLoad.Loading -> LoadingView(Modifier.padding(pad))
            KLoad.Failed -> ErrorView(retry, Modifier.padding(pad))
            is KLoad.Done -> {
                val r = state.r
                val style = KidsGroupStyle.forGroup(r.group)
                val letsCook = @Composable {
                    KidsButton("🍳 ${s[S.letsCook]}", onClick = { app.push(Route.KidsSteps(r.id)) }, modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp), tag = "kidsStartCooking")
                }
                val hero = @Composable {
                    val shape = RoundedCornerShape(24.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(shape).background(style.card.copy(alpha = 0.75f)).border(3.dp, style.border, shape).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            KidsArt(r.cover, Modifier.size(104.dp).background(Color.White.copy(alpha = 0.9f), CircleShape).padding(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(s[S.getReady], style = fifi(TS.Subheadline, W.Bold), color = KidsPalette.dim)
                                Text(r.title, style = fifi(TS.Title, W.Heavy), color = KidsPalette.ink, modifier = Modifier.semantics { heading() })
                                r.intro?.let { Text(it, style = fifi(TS.Callout), color = KidsPalette.body) }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            KidsMeta("${s[S.ages]} ${r.ages}")
                            KidsMeta("⏱ ${r.minutes} ${s[S.minutesShort]}")
                            r.servings?.let { KidsMeta("🍽 $it ${s[S.servings]}") }
                            if (r.noCook) KidsMeta("❄ ${s[S.noCook]}", KidsPalette.frostBg, KidsPalette.frostText)
                            else KidsMeta("👨‍👧 ${s[S.grownUp]}", KidsPalette.hotBg, KidsPalette.hotText)
                        }
                        r.allergens?.takeIf { it.isNotEmpty() }?.let { al ->
                            Text(
                                "⚠ ${s[S.contains]}: " + al.joinToString(", ") { s.allergen(it) },
                                style = fifi(TS.Subheadline, W.Bold), color = KidsPalette.warnText,
                                modifier = Modifier.background(KidsPalette.warnBg, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp).testTag("allergenBanner"),
                            )
                        }
                    }
                }
                val ready = @Composable {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReadyPill("wash-hands", s[S.washHands])
                        ReadyPill("apron", s[S.wearApron])
                        ReadyPill("grown-up", s[S.grownUp])
                    }
                }
                val ingredients = @Composable {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s[S.ingredients], style = fifi(TS.Title2, W.Heavy), color = KidsPalette.ink, modifier = Modifier.semantics { heading() })
                        Text(s[S.tickHint], style = fifi(TS.Footnote, W.Medium), color = KidsPalette.dim)
                        r.ingredients.forEachIndexed { i, ing ->
                            val on = i in ticked
                            val shape = RoundedCornerShape(14.dp)
                            Row(
                                Modifier.fillMaxWidth().clip(shape)
                                    .background(if (on) KidsPalette.checkSoft else Color.White.copy(alpha = 0.88f))
                                    .border(3.dp, if (on) KidsPalette.checkGreen else KidsPalette.cardBorder, shape)
                                    .toggleable(on, role = Role.Checkbox) { ticked = if (on) ticked - i else ticked + i }
                                    .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(if (on) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null, tint = if (on) KidsPalette.checkBorder else KidsPalette.chipIdle, modifier = Modifier.size(30.dp))
                                KidsArt(ing.art, Modifier.size(36.dp))
                                Text(ing.text, style = fifi(TS.Body, W.Medium).copy(textDecoration = if (on) TextDecoration.LineThrough else null), color = KidsPalette.ink.copy(alpha = if (on) 0.6f else 1f), modifier = Modifier.weight(1f))
                            }
                        }
                        r.tools?.takeIf { it.isNotEmpty() }?.let { tools ->
                            Column(Modifier.fillMaxWidth().whiteCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(s[S.tools], style = fifi(TS.Headline, W.Bold), color = KidsPalette.dim)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) { tools.forEach { KidsArt(it, Modifier.size(46.dp)) } }
                            }
                        }
                    }
                }
                val stepsPreview = @Composable {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s[S.steps], style = fifi(TS.Title2, W.Heavy), color = KidsPalette.ink, modifier = Modifier.semantics { heading() })
                        r.steps.forEachIndexed { i, st ->
                            Row(Modifier.fillMaxWidth().whiteCard().padding(12.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(32.dp).background(KidsPalette.stepOrangeDeep, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = fifi(TS.Headline, W.Heavy), color = Color.White)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(st.text, style = fifi(TS.Body, W.Medium), color = KidsPalette.ink)
                                    StepBadges(st, small = true)
                                }
                            }
                        }
                        r.tip?.let {
                            Text("⭐ ${s[S.tip]}: $it", style = fifi(TS.Callout, W.Bold), color = KidsPalette.tipText, modifier = Modifier.fillMaxWidth().background(KidsPalette.tipBg, RoundedCornerShape(16.dp)).padding(14.dp))
                        }
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 840.dp
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad).padding(16.dp).testTag("kidsReady"),
                    ) {
                        Column(Modifier.contentWidth(1300.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            RainbowBar()
                            if (wide) {
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(18.dp)) { hero(); ready(); ingredients() }
                                    Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { stepsPreview(); letsCook() }
                                }
                            } else {
                                hero(); ready(); ingredients(); stepsPreview()
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { letsCook() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KidsMeta(text: String, bg: Color = KidsPalette.meta, fg: Color = KidsPalette.ink) {
    Text(text, style = fifi(TS.Subheadline, W.Bold), color = fg, modifier = Modifier.background(bg, CircleShape).padding(horizontal = 14.dp, vertical = 7.dp))
}

@Composable
private fun ReadyPill(art: String, label: String) {
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.88f)).border(3.dp, KidsPalette.cardBorder, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KidsArt(art, Modifier.size(30.dp))
        Text(label, style = fifi(TS.Subheadline, W.Bold), color = KidsPalette.ink)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepBadges(st: KidsStep, small: Boolean) {
    val s = LocalStrings.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        if (st.adult != null) {
            Row(
                Modifier.background(KidsPalette.warnBg, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                KidsArt("grown-up", Modifier.size(if (small) 20.dp else 26.dp))
                Text(s[S.grownUp], style = fifi(if (small) TS.Footnote else TS.Subheadline, W.Bold), color = KidsPalette.warnText)
            }
        }
        st.timer?.let {
            Text("⏱ $it ${s[S.minutesShort]}", style = fifi(if (small) TS.Footnote else TS.Subheadline, W.Bold), color = KidsPalette.frostText, modifier = Modifier.background(KidsPalette.frostBg, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
        }
        st.items?.take(if (small) 5 else 6)?.forEach { KidsArt(it, Modifier.size(if (small) 28.dp else 40.dp)) }
    }
}

private fun Modifier.whiteCard(): Modifier {
    val shape = RoundedCornerShape(16.dp)
    return clip(shape).background(Color.White.copy(alpha = 0.88f)).border(3.dp, KidsPalette.cardBorder, shape)
}

/** Keeps the display on while a child follows the steps (messy hands can't tap to wake). */
@Composable
private fun KeepScreenOn() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/**
 * Step-by-step cooking: one big card per step. Swipe (pager), tap Next/Back,
 * or use ←/→, Page Up/Down, Space on a keyboard. Progress dots on top.
 */
@Composable
fun KidsStepsScreen(id: String, pad: PaddingValues) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val (state, retry) = rememberKidsRecipe(id)
    KeepScreenOn()
    KidsScope {
        when (state) {
            KLoad.Loading -> LoadingView(Modifier.padding(pad))
            KLoad.Failed -> ErrorView(retry, Modifier.padding(pad))
            is KLoad.Done -> {
                val r = state.r
                val total = r.steps.size.coerceAtLeast(1)
                val pager = rememberPagerState { total }
                val scope = rememberCoroutineScope()
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                val focus = remember { FocusRequester() }
                val last = pager.currentPage == total - 1
                fun go(d: Int) = scope.launch { pager.animateScrollToPage((pager.currentPage + d).coerceIn(0, total - 1)) }
                fun next() { if (last) app.push(Route.KidsDone(r.id, r.title)) else go(1) }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

                Column(
                    Modifier.fillMaxSize().padding(pad).padding(16.dp).testTag("kidsSteps")
                        .focusRequester(focus).focusable()
                        .onPreviewKeyEvent {
                            if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val fwd = if (rtl) Key.DirectionLeft else Key.DirectionRight
                            val bwd = if (rtl) Key.DirectionRight else Key.DirectionLeft
                            when (it.key) {
                                fwd, Key.PageDown, Key.Spacebar -> { next(); true }
                                bwd, Key.PageUp -> { go(-1); true }
                                else -> false
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RainbowBar()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(r.title, style = fifi(TS.Title2, W.Heavy), color = KidsPalette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(total) { i ->
                                val cur = pager.currentPage
                                Box(
                                    Modifier.height(10.dp).width(if (i == cur) 26.dp else 10.dp).background(
                                        when { i == cur -> KidsPalette.stepOrange; i < cur -> KidsPalette.checkGreen; else -> KidsPalette.chipIdle }, CircleShape,
                                    ),
                                )
                            }
                        }
                    }
                    HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), pageSpacing = 16.dp, verticalAlignment = Alignment.Top) { page ->
                        val st = r.steps.getOrNull(page) ?: return@HorizontalPager
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Column(
                                Modifier.widthIn(max = 880.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                                    .clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.92f))
                                    .border(3.dp, KidsPalette.cardBorder, RoundedCornerShape(26.dp)).padding(20.dp)
                                    .semantics(mergeDescendants = true) { stateDescription = UiStrings.fill(s[S.stepOf], "n" to page + 1, "t" to total) }
                                    .testTag("kidsStep-$page"),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(Modifier.size(64.dp).background(KidsPalette.stepOrangeDeep, CircleShape), contentAlignment = Alignment.Center) {
                                        Text("${page + 1}", style = fifi(TS.LargeTitle, W.Heavy), color = Color.White)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(UiStrings.fill(s[S.stepOf], "n" to page + 1, "t" to total), style = fifi(TS.Headline, W.Bold), color = KidsPalette.dim)
                                        Text(st.text, style = fifi(TS.Title3, W.Medium), color = KidsPalette.ink)
                                    }
                                }
                                StepBadges(st, small = false)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
                        if (pager.currentPage > 0) {
                            KidsButton(
                                "◀ ${s[S.prev]}".mirrorArrows(rtl), onClick = { go(-1) },
                                brush = Brush.linearGradient(listOf(Color.White, Color.White)), border = KidsPalette.chipIdle, foreground = KidsPalette.chipText,
                                tag = "kidsStepPrev",
                            )
                        }
                        KidsButton(
                            if (last) "🎉 ${s[S.finish]}" else "${s[S.next]} ▶".mirrorArrows(rtl),
                            onClick = ::next,
                            brush = if (last) KidsPalette.done else KidsPalette.go,
                            border = if (last) Color(0xFFAD1457) else KidsPalette.checkBorder,
                            tag = "kidsStepNext",
                        )
                    }
                }
            }
        }
    }
}

/** ◀/▶ are drawn glyphs, not mirrored by the bidi algorithm — flip them for RTL. */
private fun String.mirrorArrows(rtl: Boolean) = if (!rtl) this else map { when (it) { '◀' -> '▶'; '▶' -> '◀'; else -> it } }.joinToString("")

/** Celebration: kids-art confetti falls over a pop-in card (instant when animations are off). */
@Composable
fun KidsDoneScreen(title: String, pad: PaddingValues) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val pop = remember { Animatable(0.8f) }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
        launch { fall.animateTo(1f, tween(3200)) }
    }
    KidsScope {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight
            listOf("star", "bell-pepper", "cherry-tomatoes", "sweet-potato", "egg-cracked", "star").forEachIndexed { i, art ->
                val progress = ((fall.value * 1.6f) - i * 0.1f).coerceIn(0f, 1f)
                KidsArt(
                    art,
                    Modifier.size(44.dp)
                        .offset(x = maxWidth * (0.06f + i * 0.16f), y = (-60).dp + (h + 120.dp) * progress)
                        .graphicsLayer { rotationZ = progress * 360f * (if (i % 2 == 0) 1 else -1) },
                )
            }
            Column(
                Modifier.align(Alignment.Center).padding(pad).padding(24.dp).widthIn(max = 560.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.96f))
                    .border(3.dp, Color(0xFFFFD54F), RoundedCornerShape(30.dp)).padding(28.dp)
                    .testTag("kidsDone"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KidsArt("chef", Modifier.size(110.dp))
                Text(s[S.doneTitle], style = fifi(TS.LargeTitle, W.Heavy), color = KidsPalette.ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                Text(title, style = fifi(TS.Title3, W.Bold), color = KidsPalette.dim, textAlign = TextAlign.Center)
                Text(s[S.doneBody], style = fifi(TS.Body, W.Medium), color = KidsPalette.body, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                KidsButton("🌈 ${s[S.cookAgain]}", onClick = { app.popToRoot() }, tag = "kidsCookAgain")
            }
        }
    }
}
