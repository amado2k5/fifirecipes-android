package cooking.fifi.android.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cooking.fifi.android.AppViewModel
import cooking.fifi.android.data.Strings

val LocalApp = staticCompositionLocalOf<AppViewModel> { error("AppViewModel not provided") }
val LocalStrings = staticCompositionLocalOf<Strings> { error("Strings not provided") }

/** Material window-width classes (compact < 600dp ≤ medium < 840dp ≤ expanded). */
enum class WidthClass { Compact, Medium, Expanded;
    companion object {
        fun of(width: Dp) = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}

val LocalWidthClass = staticCompositionLocalOf { WidthClass.Compact }

/** Height < 480dp (phones in landscape, short freeform windows): prefer the rail, shorter heroes. */
val LocalCompactHeight = staticCompositionLocalOf { false }

/** Android sharesheet with the recipe's public (App Link) URL. */
fun shareLink(context: Context, title: String, url: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    }
    context.startActivity(Intent.createChooser(send, title))
}
