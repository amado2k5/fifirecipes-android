package cooking.fifi.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cooking.fifi.android.R

/**
 * Per-language typefaces, mirroring the TV/iOS font stack:
 *   Latin → Plus Jakarta Sans · ar/ps/ku RTL → Tajawal · fa → Vazirmatn
 *   ur → Noto Nastaliq Urdu (tall line height) · he → Heebo
 *   kids → Baloo 2 / Baloo Bhaijaan 2 (RTL) / Heebo (he)
 * Scripts a face lacks (CJK, Cyrillic, Greek, Devanagari) fall back per glyph
 * to the system fonts. All sizes are `sp`, so they follow the user's font
 * size setting (Android's Dynamic Type).
 */
object FifiFonts {
    private val jakarta = FontFamily(
        Font(R.font.plusjakartasans_400, FontWeight.Normal),
        Font(R.font.plusjakartasans_500, FontWeight.Medium),
        Font(R.font.plusjakartasans_700, FontWeight.Bold),
        Font(R.font.plusjakartasans_800, FontWeight.ExtraBold),
    )
    private val tajawal = FontFamily(
        Font(R.font.tajawal_400, FontWeight.Normal),
        Font(R.font.tajawal_500, FontWeight.Medium),
        Font(R.font.tajawal_700, FontWeight.Bold),
        Font(R.font.tajawal_800, FontWeight.ExtraBold),
    )
    private val vazirmatn = FontFamily(
        Font(R.font.vazirmatn_400, FontWeight.Normal),
        Font(R.font.vazirmatn_500, FontWeight.Medium),
        Font(R.font.vazirmatn_700, FontWeight.Bold),
    )
    private val heebo = FontFamily(
        Font(R.font.heebo_400, FontWeight.Normal),
        Font(R.font.heebo_500, FontWeight.Medium),
        Font(R.font.heebo_700, FontWeight.Bold),
    )
    private val nastaliq = FontFamily(
        Font(R.font.notonastaliqurdu_400, FontWeight.Normal),
        Font(R.font.notonastaliqurdu_700, FontWeight.Bold),
    )
    private val baloo = FontFamily(
        Font(R.font.baloo2_400, FontWeight.Normal),
        Font(R.font.baloo2_600, FontWeight.SemiBold),
        Font(R.font.baloo2_800, FontWeight.ExtraBold),
    )
    private val balooArabic = FontFamily(
        Font(R.font.baloobhaijaan2_400, FontWeight.Normal),
        Font(R.font.baloobhaijaan2_600, FontWeight.SemiBold),
        Font(R.font.baloobhaijaan2_800, FontWeight.ExtraBold),
    )

    private val rtl = setOf("ar", "ur", "fa", "ps", "he", "ku")

    fun family(lang: String, kids: Boolean): FontFamily = when {
        kids && lang == "he" -> heebo
        kids -> if (lang in rtl) balooArabic else baloo
        lang == "ur" -> nastaliq
        lang == "fa" -> vazirmatn
        lang == "he" -> heebo
        lang in rtl -> tajawal
        else -> jakarta
    }
}

/** Text roles — iOS text-style names, Android-sized. */
enum class TS(val size: TextUnit) {
    LargeTitle(30.sp), Title(26.sp), Title2(22.sp), Title3(19.sp), Headline(16.sp),
    Body(16.sp), Callout(15.sp), Subheadline(14.sp), Footnote(13.sp), Caption(12.sp),
}

enum class W(val weight: FontWeight) { Regular(FontWeight.Normal), Medium(FontWeight.Medium), Bold(FontWeight.Bold), Heavy(FontWeight.ExtraBold) }

val LocalFifiLang = staticCompositionLocalOf { "en" }
val LocalKidsFont = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun fifi(style: TS = TS.Body, weight: W = W.Regular): TextStyle {
    val lang = LocalFifiLang.current
    val kids = LocalKidsFont.current
    // Baloo ships 400/600/800 — map Medium/Bold onto the faces it has.
    val w = if (kids) when (weight) {
        W.Regular -> FontWeight.Normal
        W.Medium -> FontWeight.SemiBold
        W.Bold, W.Heavy -> FontWeight.ExtraBold
    } else weight.weight
    return TextStyle(
        fontFamily = FifiFonts.family(lang, kids),
        fontWeight = w,
        fontSize = style.size,
        // Nastaliq needs generous leading (TV CSS line-height 2.1).
        lineHeight = if (lang == "ur" && !kids) 2.1.em else TextUnit.Unspecified,
    )
}
