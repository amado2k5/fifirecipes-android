package cooking.fifi.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Fresh-market palette — identical hex values to the Fire TV / iOS apps. */
object Palette {
    val paper = Color(0xFFFDF4E3)
    val paperDeep = Color(0xFFF6E9D2)
    val card = Color(0xFFFFFDF8)
    val cardHover = Color(0xFFFDEED6)
    val cardBorder = Color(0xFFF0E2C8)
    val ink = Color(0xFF43311F)
    val inkDim = Color(0xFF6F5B42) // TV #7d6a52 darkened for ≥4.5:1 on paper
    val leaf = Color(0xFF4D9426)
    val leafDeep = Color(0xFF3A7A1E)
    val leafSoft = Color(0xFFE6F3D8)
    val tomato = Color(0xFFE8590C)
    val tomatoDeep = Color(0xFFB8440A) // tomato text on light chips (≥4.5:1)
    val tomatoSoft = Color(0xFFFDE4D3)
    val sun = Color(0xFFFFC53D)
    val sunSoft = Color(0xFFFFF3C4)
    val berry = Color(0xFFD6336C)
    val berryDeep = Color(0xFFB02457)
    val sky = Color(0xFF74C0FC)
}

/** Kids mode palette — the TV kids screens / website Cooking with Kids. */
object KidsPalette {
    val ink = Color(0xFF4A3426)
    val dim = Color(0xFF7A5F44) // TV #8a6f52 darkened for ≥4.5:1 on the kids canvas
    val body = Color(0xFF7A5C3E)
    val canvas = Color(0xFFFFF8E7)
    val dot = Color(0xFFF5D9A8)
    val cardBorder = Color(0xFFF0E2C8)
    val checkGreen = Color(0xFF7CB342)
    val checkBorder = Color(0xFF558B2F)
    val checkSoft = Color(0xFFF1F8E9)
    val stepOrange = Color(0xFFFF8A65)
    val stepOrangeDeep = Color(0xFFD9531E)
    val chipIdle = Color(0xFFE8D9BD)
    val chipText = Color(0xFF6B5335)
    val warnBg = Color(0x40FF8A80)
    val warnText = Color(0xFFA13333)
    val frostBg = Color(0xFFB3E5FC)
    val frostText = Color(0xFF14455E)
    val hotBg = Color(0xFFFFCCBC)
    val hotText = Color(0xFF7C2D12)
    val tipBg = Color(0xFFFFF3C4)
    val tipText = Color(0xFF5D4317)
    val purple = Color(0xFF8E6FC0)
    val meta = Color(0xFFFFD93B)

    val rainbow = Brush.horizontalGradient(
        listOf(Color(0xFFFF8A80), Color(0xFFFFD54F), Color(0xFFAED581), Color(0xFF4FC3F7), Color(0xFFBA68C8), Color(0xFFFF8A80)),
    )
    val go = Brush.linearGradient(listOf(Color(0xFF7CB342), Color(0xFF43A047)))
    val done = Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFFD6336C)))
}

/** Per-group kids styling — same hues as the TV app's KIDS_GROUP_STYLE. */
data class KidsGroupStyle(val card: Color, val border: Color, val chip: Color, val emoji: String) {
    companion object {
        fun forGroup(group: String?): KidsGroupStyle = when (group) {
            "snack" -> KidsGroupStyle(Color(0xFFECFCCB), Color(0xFFBEF264), Color(0xFF65A30D), "🍎")
            "savoury" -> KidsGroupStyle(Color(0xFFFFE4E6), Color(0xFFFDA4AF), Color(0xFFE11D48), "🍕")
            "sweet" -> KidsGroupStyle(Color(0xFFFCE7F3), Color(0xFFF9A8D4), Color(0xFFDB2777), "🧁")
            "drink" -> KidsGroupStyle(Color(0xFFE0F2FE), Color(0xFF7DD3FC), Color(0xFF0284C7), "🥤")
            else -> KidsGroupStyle(Color(0xFFFEF3C7), Color(0xFFFCD34D), Color(0xFFB45309), if (group == "breakfast") "🥞" else "🍽️")
        }
    }
}
