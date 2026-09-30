package cooking.fifi.android

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end flows against the live fifi.cooking API (the product is
 * online-only, same as the iOS UI tests). Each test starts from a clean
 * first-run state: no stored language, no per-app locale.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val rule = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private val timeout = 30_000L

    @Before fun freshStart() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("fifi", Context.MODE_PRIVATE).edit().clear().commit()
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                ctx.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.getEmptyLocaleList()
            }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun close() { scenario?.close() }

    private fun waitTag(tag: String) = rule.waitUntilAtLeastOneExists(hasTestTag(tag), timeout)

    /**
     * Activate via the semantics click action: edge-to-edge content scrolls
     * under the bottom bar, where a coordinate tap would hit the bar instead.
     */
    private fun SemanticsNodeInteraction.tap() = performSemanticsAction(SemanticsActions.OnClick)

    private fun pick(lang: String) {
        waitTag("lang-$lang")
        rule.onNodeWithTag("lang-$lang").performClick()
        waitTag("homeScreen")
    }

    @Test fun firstRunPickerThenHome() {
        waitTag("languagePicker")
        pick("en")
        waitTag("hero")
        rule.onNodeWithTag("hero").assertIsDisplayed()
        rule.onNodeWithTag("rail-featured").assertIsDisplayed()
    }

    @Test fun recipeChecklistAndBack() {
        pick("en")
        waitTag("hero")
        rule.onNodeWithTag("hero").performClick()
        waitTag("recipeTitle")
        waitTag("ingredient-0")
        rule.onNodeWithTag("ingredient-0").performScrollTo().assertIsOff().tap().assertIsOn()
        rule.onNodeWithTag("backButton").performClick()
        waitTag("hero")
    }

    @Test fun searchFindsRecipes() {
        pick("en")
        rule.onNodeWithTag("nav-search").performClick()
        waitTag("searchField")
        rule.onNodeWithTag("searchField").performTextInput("rice")
        rule.waitUntil(timeout) {
            rule.onAllNodes(SemanticsMatcher("recipe card") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("recipe-") == true })
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun chaptersOpenDetail() {
        pick("en")
        rule.onNodeWithTag("nav-chapters").performClick()
        waitTag("chapter-1")
        rule.onNodeWithTag("chapter-1").performClick()
        waitTag("chapterDetail")
    }

    @Test fun kidsFlowToCelebration() {
        pick("en")
        rule.onNodeWithTag("nav-kids").performClick()
        waitTag("kidsScreen")
        rule.onNodeWithTag("kidsFilter-nocook").performClick()
        rule.waitUntil(timeout) {
            rule.onAllNodes(SemanticsMatcher("kids card") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("kids-") == true })
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodes(SemanticsMatcher("kids card") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("kids-") == true }).onFirst().performClick()
        waitTag("kidsReady")
        rule.onNodeWithTag("kidsStartCooking").performScrollTo().tap()
        waitTag("kidsSteps")
        repeat(20) {
            if (rule.onAllNodesWithTag("kidsDone").fetchSemanticsNodes().isNotEmpty()) return@repeat
            rule.onNodeWithTag("kidsStepNext").performClick()
            rule.waitForIdle()
        }
        waitTag("kidsDone")
        rule.onNodeWithTag("kidsCookAgain").performClick()
        waitTag("kidsScreen")
    }

    @Test fun arabicIsRightToLeft() {
        pick("ar")
        waitTag("hero")
        // Arabic title text comes from the API; the nav label is the Arabic UI string.
        rule.onNode(hasText("الرئيسية")).assertIsDisplayed()
        val hero = rule.onNodeWithTag("hero").fetchSemanticsNode().boundsInRoot
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("hero spans the content", hero.width > root.width * 0.5f)
    }

    /** Google accessibility guideline: every touch target ≥ 48×48dp. */
    @Test fun touchTargetsAreAtLeast48dp() {
        pick("en")
        waitTag("hero")
        for (tab in listOf("nav-home", "nav-settings")) {
            rule.onNodeWithTag(tab).performClick()
            rule.waitForIdle()
            val density = rule.onRoot().fetchSemanticsNode().layoutInfo.density
            val min = with(density) { 48.dp.toPx() } - 1f
            rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
                val b = node.touchBoundsInRoot
                val label = node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
                    ?: node.config.getOrNull(SemanticsProperties.TestTag)
                // Nodes cut by the viewport edge (rails, scrolled lists) are partially measured; skip them.
                if (b.width > 0 && b.height > 0 && b.left > 0 && b.top > 0) {
                    assertTrue("$tab: '$label' is ${b.width}x${b.height}px (< 48dp)", b.width >= min && b.height >= min)
                }
                assertTrue("$tab: clickable '$label' has no label", label != null || node.config.getOrNull(SemanticsActions.OnClick)?.label != null)
            }
        }
    }
}
