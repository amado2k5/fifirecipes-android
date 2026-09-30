package cooking.fifi.android.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** UI string keys — identical names to the TV app's UIStrings (src/i18n/strings.ts). */
enum class S {
    appName, tagline, home, chapters, search, kids, settings,
    chooseLanguage, loading, errorTitle, errorBody, retry,
    searchTitle, searchHint, noResults,
    ingredients, steps, alternativeMethods, culturalNotes, tips, videos,
    openInYouTube, close, prep, cook, servings, ages, minutesShort,
    grownUp, noCook, tip, tools, language, about, aboutText, version,
    kcal, protein, carbs, fat, playPause, resultsFor,
    all, groupBreakfast, groupSnack, groupSavoury, groupSweet, groupDrink,
    letsCook, getReady, washHands, wearApron, stepOf, doneTitle, doneBody,
    cookAgain, contains, tickHint, next, prev, finish,
    // Android-only (assets/ui-strings-android.json)
    share, open, back, clearSearch, privacy,
}

/**
 * Runtime string table. ui-strings.json is shared verbatim with the iOS/iPad
 * apps; ui-strings-android.json layers Android wording on top. Missing keys fall
 * back to English — the TV app's `{ ...EN, ...STRINGS[lang] }` merge.
 */
class UiStrings(
    private val tables: Map<String, Map<String, String>>,
    private val allergens: Map<String, Map<String, String>>,
) {
    fun get(lang: String, key: S): String =
        tables[lang]?.get(key.name) ?: tables["en"]?.get(key.name) ?: key.name

    fun allergen(lang: String, code: String): String =
        allergens[lang]?.get(code) ?: allergens["en"]?.get(code) ?: code

    fun forLang(lang: String) = Strings(this, lang)

    @Serializable
    private data class File(
        val strings: Map<String, Map<String, String>> = emptyMap(),
        val allergens: Map<String, Map<String, String>> = emptyMap(),
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(base: String, overrides: String?): UiStrings {
            val b = json.decodeFromString(File.serializer(), base)
            val o = overrides?.let { json.decodeFromString(File.serializer(), it) }
            val merged = b.strings.mapValues { (lang, table) -> table + (o?.strings?.get(lang) ?: emptyMap()) }
            return UiStrings(merged, b.allergens)
        }

        fun load(context: Context): UiStrings {
            fun read(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
            return parse(read("ui-strings.json"), runCatching { read("ui-strings-android.json") }.getOrNull())
        }

        /** Replace {placeholders} — e.g. stepOf "Step {n} of {t}". */
        fun fill(s: String, vararg vars: Pair<String, Any>): String =
            vars.fold(s) { acc, (k, v) -> acc.replace("{$k}", v.toString()) }
    }
}

/** Language-bound view used by composables: `s[S.home]`. */
class Strings(private val table: UiStrings, val lang: String) {
    operator fun get(key: S): String = table.get(lang, key)
    fun allergen(code: String) = table.allergen(lang, code)
}
