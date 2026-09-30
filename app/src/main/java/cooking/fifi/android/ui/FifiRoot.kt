package cooking.fifi.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cooking.fifi.android.AppViewModel
import cooking.fifi.android.Phase
import cooking.fifi.android.R
import cooking.fifi.android.data.ApiClient
import cooking.fifi.android.data.Route
import cooking.fifi.android.data.S
import cooking.fifi.android.data.Section
import cooking.fifi.android.ui.components.ErrorView
import cooking.fifi.android.ui.components.PaperBackground
import cooking.fifi.android.ui.kids.KidsDoneScreen
import cooking.fifi.android.ui.kids.KidsReadyScreen
import cooking.fifi.android.ui.kids.KidsScreen
import cooking.fifi.android.ui.kids.KidsStepsScreen
import cooking.fifi.android.ui.screens.ChapterDetailScreen
import cooking.fifi.android.ui.screens.ChaptersScreen
import cooking.fifi.android.ui.screens.HomeScreen
import cooking.fifi.android.ui.screens.LanguagePickerScreen
import cooking.fifi.android.ui.screens.RecipeDetailScreen
import cooking.fifi.android.ui.screens.SearchScreen
import cooking.fifi.android.ui.screens.SettingsScreen
import cooking.fifi.android.ui.theme.FifiTheme
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

@Composable
fun FifiRoot(vm: AppViewModel) {
    FifiTheme(lang = vm.lang, rtl = vm.isRtl) {
        CompositionLocalProvider(LocalApp provides vm, LocalStrings provides vm.strings) {
            // Test tags double as resource ids: UI Automator, Play's pre-launch
            // robo crawler and accessibility scanners can address every control.
            BoxWithConstraints(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                CompositionLocalProvider(LocalWidthClass provides WidthClass.of(maxWidth), LocalCompactHeight provides (maxHeight < 480.dp)) {
                    PaperBackground()
                    when (vm.phase) {
                        Phase.Loading -> SplashView()
                        Phase.Failed -> ErrorView(onRetry = vm::retry, modifier = Modifier.safeDrawingPadding())
                        Phase.PickingLanguage -> LanguagePickerScreen(onPicked = vm::setLanguage, modifier = Modifier.safeDrawingPadding())
                        Phase.Ready -> MainShell(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashView() {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.emblem), null, Modifier.size(120.dp))
        Text(s[S.appName], style = fifi(TS.LargeTitle, W.Heavy), color = Palette.leafDeep)
        Text(s[S.tagline], style = fifi(TS.Title3), color = Palette.inkDim, textAlign = TextAlign.Center)
        CircularProgressIndicator(color = Palette.leafDeep, modifier = Modifier.padding(top = 8.dp))
    }
}

private data class NavItem(val section: Section, val label: S, val icon: ImageVector, val selectedIcon: ImageVector)

private val navItems = listOf(
    NavItem(Section.Home, S.home, Icons.Outlined.Home, Icons.Filled.Home),
    NavItem(Section.Chapters, S.chapters, Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    NavItem(Section.Search, S.search, Icons.Filled.Search, Icons.Filled.Search),
    NavItem(Section.Kids, S.kids, Icons.Outlined.SentimentSatisfied, Icons.Filled.SentimentSatisfied),
    NavItem(Section.Settings, S.settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)

/**
 * Adaptive shell (Material 3 canonical navigation):
 *   compact  (< 600dp: phones, folded)        → bottom navigation bar
 *   medium   (600–840dp: unfolded, small tabs) → navigation rail
 *   expanded (≥ 840dp: tablets, ChromeOS)      → permanent branded drawer
 * Short windows (landscape phones) use the rail even when wide.
 * The class is recomputed on every resize — fold/unfold, rotation,
 * multi-window and freeform windows all reflow live without recreation.
 */
@Composable
private fun MainShell(vm: AppViewModel) {
    val s = LocalStrings.current
    // A landscape phone is "expanded" by width but too short for a drawer — use the rail.
    val wc = LocalWidthClass.current.let { if (it == WidthClass.Expanded && LocalCompactHeight.current) WidthClass.Medium else it }
    BackHandler(enabled = vm.currentStack.isNotEmpty() || vm.section != Section.Home) { vm.back() }

    when (wc) {
        WidthClass.Compact -> Scaffold(
            containerColor = Color.Transparent,
            topBar = { ShellTopBar(vm) },
            bottomBar = {
                NavigationBar(containerColor = Palette.card, modifier = Modifier.testTag("bottomNav")) {
                    navItems.forEach { item ->
                        val selected = vm.section == item.section
                        NavigationBarItem(
                            selected = selected,
                            onClick = { vm.select(item.section) },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, null) },
                            label = { Text(s[item.label], style = fifi(TS.Caption, W.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.leafDeep, selectedTextColor = Palette.leafDeep,
                                indicatorColor = Palette.leafSoft, unselectedIconColor = Palette.ink, unselectedTextColor = Palette.ink,
                            ),
                            modifier = Modifier.testTag("nav-${item.section.name.lowercase()}"),
                        )
                    }
                }
            },
        ) { pad -> SectionContent(vm, pad) }

        WidthClass.Medium -> Row(Modifier.fillMaxSize()) {
            val short = LocalCompactHeight.current
            NavigationRail(
                containerColor = Palette.card,
                header = if (short) null else ({ Image(painterResource(R.drawable.emblem), null, Modifier.padding(top = 12.dp).size(52.dp)) }),
                modifier = Modifier.testTag("navRail"),
            ) {
                if (!short) Spacer(Modifier.height(12.dp))
                navItems.forEach { item ->
                    val selected = vm.section == item.section
                    NavigationRailItem(
                        selected = selected,
                        onClick = { vm.select(item.section) },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, null) },
                        label = { Text(s[item.label], style = fifi(TS.Caption, W.Bold), maxLines = 1) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Palette.leafDeep, selectedTextColor = Palette.leafDeep,
                            indicatorColor = Palette.leafSoft, unselectedIconColor = Palette.ink, unselectedTextColor = Palette.ink,
                        ),
                        modifier = Modifier.padding(vertical = if (short) 0.dp else 4.dp).testTag("nav-${item.section.name.lowercase()}"),
                    )
                }
            }
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                topBar = { ShellTopBar(vm) },
            ) { pad -> SectionContent(vm, pad) }
        }

        WidthClass.Expanded -> Row(Modifier.fillMaxSize()) {
            PermanentDrawerSheet(
                drawerContainerColor = Palette.card,
                modifier = Modifier.width(280.dp).fillMaxHeight().testTag("navDrawer"),
            ) {
                Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                    DrawerHeader()
                    navItems.forEach { item ->
                        val selected = vm.section == item.section
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = { vm.select(item.section) },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, null) },
                            label = { Text(s[item.label], style = fifi(TS.Headline, W.Medium)) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Palette.leafSoft, selectedIconColor = Palette.leafDeep, selectedTextColor = Palette.leafDeep,
                                unselectedIconColor = Palette.ink, unselectedTextColor = Palette.ink,
                            ),
                            modifier = Modifier.padding(vertical = 2.dp).testTag("nav-${item.section.name.lowercase()}"),
                        )
                    }
                    vm.manifest?.version?.let {
                        Text("fifi.cooking · $it", style = fifi(TS.Caption), color = Palette.inkDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
                    }
                }
            }
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                topBar = { ShellTopBar(vm) },
            ) { pad -> SectionContent(vm, pad) }
        }
    }
}

@Composable
private fun DrawerHeader() {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Image(painterResource(R.drawable.emblem), null, Modifier.size(80.dp).clip(RoundedCornerShape(18.dp)))
        Text(s[S.appName], style = fifi(TS.Title2, W.Heavy), color = Palette.leafDeep)
        Text(s[S.tagline], style = fifi(TS.Footnote), color = Palette.inkDim, textAlign = TextAlign.Center)
    }
}

/** Title for the visible screen — known from cached index data, so no flash while a recipe loads. */
private fun titleFor(vm: AppViewModel, route: Route?): String {
    val s = vm.strings
    return when (route) {
        null -> when (vm.section) {
            Section.Home -> s[S.appName]
            Section.Chapters -> s[S.chapters]
            Section.Search -> s[S.search]
            Section.Kids -> s[S.kids]
            Section.Settings -> s[S.settings]
        }
        is Route.Recipe -> vm.index[route.id]?.title ?: ""
        is Route.ChapterDetail -> vm.chapters.firstOrNull { it.id == route.id }?.name ?: s[S.chapters]
        is Route.KidsReady -> vm.kids[route.id]?.title ?: s[S.kids]
        is Route.KidsSteps -> vm.kids[route.id]?.title ?: s[S.kids]
        is Route.KidsDone -> route.title
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopBar(vm: AppViewModel) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val route = vm.currentStack.lastOrNull()
    val compactHome = route == null && vm.section == Section.Home && LocalWidthClass.current == WidthClass.Compact
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (compactHome) {
                    Image(painterResource(R.drawable.emblem), null, Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    titleFor(vm, route),
                    style = fifi(if (route == null) TS.Title2 else TS.Title3, if (route == null) W.Heavy else W.Bold),
                    color = if (compactHome) Palette.leafDeep else Palette.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
        navigationIcon = {
            if (route != null) {
                IconButton(onClick = { vm.back() }, modifier = Modifier.testTag("backButton")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, s[S.back], tint = Palette.ink)
                }
            }
        },
        actions = {
            if (route is Route.Recipe) {
                IconButton(onClick = { shareLink(context, titleFor(vm, route), ApiClient.shareUrl(route.id)) }, modifier = Modifier.testTag("shareButton")) {
                    Icon(Icons.Filled.Share, s[S.share], tint = Palette.ink)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.paper, scrolledContainerColor = Palette.paper),
    )
}

/**
 * Current screen of the selected section. Each (section, depth, route) keeps
 * its rememberSaveable state — scroll positions, ticked ingredients — so Back
 * returns exactly where you were, across rotation and process death too.
 */
@Composable
private fun SectionContent(vm: AppViewModel, pad: PaddingValues) {
    val holder = rememberSaveableStateHolder()
    val stack = vm.currentStack
    val key = "${vm.section.name}/${stack.size}/${stack.lastOrNull()?.encode() ?: "root"}"
    AnimatedContent(
        targetState = key,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
        modifier = Modifier.fillMaxSize(),
    ) { k ->
        val parts = k.split('/', limit = 3)
        val route = parts[2].takeIf { it != "root" }?.let { Route.decode(it) }
        val section = Section.valueOf(parts[0])
        holder.SaveableStateProvider(k) {
            Box(Modifier.fillMaxSize()) {
                when (route) {
                    null -> when (section) {
                        Section.Home -> HomeScreen(pad)
                        Section.Chapters -> ChaptersScreen(pad)
                        Section.Search -> SearchScreen(pad)
                        Section.Kids -> KidsScreen(pad)
                        Section.Settings -> SettingsScreen(pad)
                    }
                    is Route.Recipe -> RecipeDetailScreen(route.id, pad)
                    is Route.ChapterDetail -> ChapterDetailScreen(route.id, pad)
                    is Route.KidsReady -> KidsReadyScreen(route.id, pad)
                    is Route.KidsSteps -> KidsStepsScreen(route.id, pad)
                    is Route.KidsDone -> KidsDoneScreen(route.title, pad)
                }
            }
        }
    }
}
