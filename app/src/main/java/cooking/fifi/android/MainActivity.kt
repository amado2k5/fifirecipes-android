package cooking.fifi.android

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cooking.fifi.android.data.S
import cooking.fifi.android.data.Section
import cooking.fifi.android.ui.FifiRoot

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash (Android 12+ API, backported) stays up only until the
        // manifest request settles, capped by the app's own splash after that.
        installSplashScreen().setKeepOnScreenCondition { vm.phase == Phase.Loading && vm.manifest == null && !splashTimedOut() }
        // Edge-to-edge (mandatory at targetSdk 35+): light cream bars, dark icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        vm.start()
        setContent { FifiRoot(vm) }
    }

    private val created = System.currentTimeMillis()
    private fun splashTimedOut() = System.currentTimeMillis() - created > 1500

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.syncSystemLanguage()
    }

    /** App Links: https://fifi.cooking/recipe/{id}/, /chapter/{n}, /kids/{id}. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) vm.handleLink(intent.data?.path)
    }

    /**
     * Hardware keyboard (tablets, ChromeOS, Bluetooth keyboards):
     *   Ctrl+1…5 switch sections · Ctrl+F search · Esc back.
     * Arrow keys/Tab/Enter move focus and activate natively in Compose.
     */
    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        if (vm.phase != Phase.Ready || !event.isCtrlPressed) return super.onKeyShortcut(keyCode, event)
        val section = when (keyCode) {
            KeyEvent.KEYCODE_1 -> Section.Home
            KeyEvent.KEYCODE_2 -> Section.Chapters
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_F -> Section.Search
            KeyEvent.KEYCODE_4 -> Section.Kids
            KeyEvent.KEYCODE_5 -> Section.Settings
            else -> return super.onKeyShortcut(keyCode, event)
        }
        if (vm.section == section && keyCode == KeyEvent.KEYCODE_F) vm.popToRoot() else if (vm.section != section) vm.select(section)
        return true
    }

    /** Esc = Back on hardware keyboards (reaches here only if no view consumed it). */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && vm.phase == Phase.Ready && vm.back()) return true
        return super.onKeyUp(keyCode, event)
    }

    /** Lists the shortcuts in the system Keyboard Shortcuts helper (Meta + /). */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        val s = vm.strings
        data += KeyboardShortcutGroup(
            s[S.appName],
            listOf(
                KeyboardShortcutInfo(s[S.home], KeyEvent.KEYCODE_1, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo(s[S.chapters], KeyEvent.KEYCODE_2, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo(s[S.search], KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo(s[S.kids], KeyEvent.KEYCODE_4, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo(s[S.settings], KeyEvent.KEYCODE_5, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo(s[S.back], KeyEvent.KEYCODE_ESCAPE, 0),
            ),
        )
    }
}
