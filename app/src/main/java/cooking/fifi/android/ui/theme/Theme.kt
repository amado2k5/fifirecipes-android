package cooking.fifi.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection

/**
 * Brand theme. FiFi's identity is the warm cream "fresh market" page, so the
 * app is light-only by design (like the iOS apps and fifi.cooking); all text
 * colour pairs are ≥4.5:1. Material components pick up the brand colours.
 */
@Composable
fun FifiTheme(lang: String, rtl: Boolean, content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Palette.leafDeep,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = Palette.leafSoft,
        onPrimaryContainer = Palette.leafDeep,
        secondary = Palette.tomato,
        onSecondary = androidx.compose.ui.graphics.Color.White,
        secondaryContainer = Palette.leafSoft,
        onSecondaryContainer = Palette.leafDeep,
        tertiary = Palette.berry,
        background = Palette.paper,
        onBackground = Palette.ink,
        surface = Palette.paper,
        onSurface = Palette.ink,
        surfaceVariant = Palette.paperDeep,
        onSurfaceVariant = Palette.inkDim,
        surfaceContainer = Palette.paperDeep,
        surfaceContainerLow = Palette.paper,
        surfaceContainerHigh = Palette.card,
        surfaceContainerHighest = Palette.card,
        outline = Palette.inkDim,
        outlineVariant = Palette.cardBorder,
    )
    CompositionLocalProvider(
        LocalFifiLang provides lang,
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        val family = FifiFonts.family(lang, kids = false)
        val base = Typography()
        fun TextStyle.f() = copy(fontFamily = family)
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(
                displayLarge = base.displayLarge.f(), displayMedium = base.displayMedium.f(), displaySmall = base.displaySmall.f(),
                headlineLarge = base.headlineLarge.f(), headlineMedium = base.headlineMedium.f(), headlineSmall = base.headlineSmall.f(),
                titleLarge = base.titleLarge.f(), titleMedium = base.titleMedium.f(), titleSmall = base.titleSmall.f(),
                bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f(),
                labelLarge = base.labelLarge.f(), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f(),
            ),
            content = content,
        )
    }
}
