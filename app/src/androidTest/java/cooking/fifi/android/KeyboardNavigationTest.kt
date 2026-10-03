package cooking.fifi.android

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Keyboard / D-pad navigation (ChromeOS, tablets and phones with keyboards).
 * Regression: pages used to lay out under the bars, so arrow keys hopped
 * bar-to-bar and could never reach the page content.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class KeyboardNavigationTest {
    @get:Rule val rule = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun launch() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        // Skip the first-run picker. The per-app locale is deliberately left
        // alone: changing it recreates the activity right after launch and
        // strands injected key events. Tags make the test language-agnostic.
        ctx.getSharedPreferences("fifi", Context.MODE_PRIVATE).edit().putString("language", "en").commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntilAtLeastOneExists(hasTestTag("hero"), 30_000)
        // Key events only reach Compose once the window has input focus.
        rule.waitUntil(10_000) {
            var focused = false
            scenario?.onActivity { focused = it.hasWindowFocus() }
            focused
        }
    }

    @After fun close() { scenario?.close() }

    private fun focused(): String? =
        rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.TestTag) ?: "untagged#${it.id}:${it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() ?: it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text ?: ""}" }
            .let { all -> all.firstOrNull { !it.startsWith("untagged") } ?: all.firstOrNull() }

    /**
     * Real key events through the system input pipeline (like a hardware
     * keyboard), not Compose's synthetic injection — that skips the touch-mode
     * exit a first key press performs and leaves nothing focused.
     */
    private fun press(keyCode: Int, times: Int = 1) = repeat(times) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
        rule.waitForIdle()
    }

    @Test fun arrowsMoveBetweenBarsAndPage() {
        // Tab → hero, Enter opens the recipe, Tab → top bar Back.
        press(KeyEvent.KEYCODE_TAB)
        assertEquals("hero", focused())
        press(KeyEvent.KEYCODE_ENTER)
        rule.waitUntilAtLeastOneExists(hasTestTag("ingredient-0"), 30_000)
        press(KeyEvent.KEYCODE_TAB)
        // Where that Tab lands after the screen change varies on CI emulators
        // (sometimes the bottom bar's Home, from which Down goes nowhere).
        // What's under test is arrowing from the top bar into the page, so
        // climb to the top bar's Back button with real Up presses first.
        // (A semantics requestFocus here raced Compose's layout on CI.)
        repeat(40) { if (focused() != "backButton") press(KeyEvent.KEYCODE_DPAD_UP) }
        assertEquals("backButton", focused())
        val visited = (1..4).map { press(KeyEvent.KEYCODE_DPAD_DOWN); focused() }
        assertTrue("Down from the top bar never entered the recipe: $visited", visited.any { it?.startsWith("ingredient-") == true })

        // Bottom bar → Up returns into the page.
        press(KeyEvent.KEYCODE_DPAD_DOWN, 15)
        assertTrue("expected the bottom bar, got ${focused()}", focused()?.startsWith("nav-") == true)
        press(KeyEvent.KEYCODE_DPAD_UP)
        assertTrue("Up from the bottom bar did not re-enter the page: ${focused()}", focused()?.startsWith("nav-") == false)
    }
}
