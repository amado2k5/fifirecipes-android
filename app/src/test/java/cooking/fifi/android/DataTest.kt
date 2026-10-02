package cooking.fifi.android

import cooking.fifi.android.data.ApiClient
import cooking.fifi.android.data.DeepLinks
import cooking.fifi.android.data.RecipeFile
import cooking.fifi.android.data.RecipeLocalization
import cooking.fifi.android.data.RecipeSearch
import cooking.fifi.android.data.Route
import cooking.fifi.android.data.S
import cooking.fifi.android.data.Section
import cooking.fifi.android.data.TvManifest
import cooking.fifi.android.data.UiStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DataTest {
    private val recipeJson = """
    {
      "recipe": {
        "id": "meat-01", "title": "كفتة", "titleEn": "Kofta", "chapter": "اللحوم", "prepTime": "١٠ دقائق",
        "masterIngredients": [{"id": "i1", "name": "لحمة", "standardAmount": "٥٠٠ جم"}, {"id": "i2", "name": "بصل"}],
        "uniqueInstructions": [
          {"stepNumber": 1, "text": "اخلط", "textEn": "Mix"},
          {"stepNumber": 2, "text": "اشوي", "textEn": "Grill", "isAlternative": true, "alternativeLabel": "Oven"},
          {"stepNumber": 3, "text": "نصيحة", "textEn": "Tip text", "importance": "tip"}
        ],
        "futureField": 42
      },
      "translations": {
        "en": {"title": "Kofta", "prepTime": "10 min", "culturalNotes": "English note",
               "ingredients": {"i1": {"name": "Beef", "standardAmount": "500 g"}}, "instructions": {"1": "Mix well"}},
        "fr": {"title": "Kefta", "culturalNotes": "Note FR"},
        "ar": {}
      }
    }""".trimIndent()

    private val file = ApiClient.json.decodeFromString(RecipeFile.serializer(), recipeJson)

    @Test fun arabicUsesMasterFieldsNeverEnglish() {
        val l = RecipeLocalization.localize(file, "ar")
        assertEquals("كفتة", l.title)
        assertEquals("١٠ دقائق", l.prepTime)
        assertEquals("لحمة", l.ingredients[0].name)
        assertEquals("اخلط", l.steps[0].text)
        assertNull("cultural notes never fall back to English", l.culturalNotes)
    }

    @Test fun otherLanguagesFallBackToEnglishThenMaster() {
        val l = RecipeLocalization.localize(file, "fr")
        assertEquals("Kefta", l.title)
        assertEquals("10 min", l.prepTime) // en fallback
        assertEquals("Beef", l.ingredients[0].name) // en ingredient map
        assertEquals("Mix well", l.steps[0].text) // en instruction map
        assertEquals("Grill", l.steps[1].text) // textEn
        assertEquals("Note FR", l.culturalNotes)
        assertEquals("Kofta", l.subtitle)
    }

    @Test fun partialTranslationsFallBackToEnglishNotArabic() {
        // de has an ingredient map, but without i1's amount and without i2 at all.
        val json = recipeJson.replace(
            "\"fr\": {",
            "\"de\": {\"ingredients\": {\"i1\": {\"name\": \"Rind\"}}},\n        \"fr\": {",
        ).replace("\"i1\": {\"name\": \"Beef\", \"standardAmount\": \"500 g\"}}", "\"i1\": {\"name\": \"Beef\", \"standardAmount\": \"500 g\"}, \"i2\": {\"name\": \"Onion\", \"standardAmount\": \"1\"}}")
        val l = RecipeLocalization.localize(ApiClient.json.decodeFromString(RecipeFile.serializer(), json), "de")
        assertEquals("Rind", l.ingredients[0].name)
        assertEquals("500 g", l.ingredients[0].amount) // English, not "٥٠٠ جم"
        assertEquals("Onion", l.ingredients[1].name) // English, not "بصل"
        assertEquals("1", l.ingredients[1].amount)
        // Arabic still shows the Arabic master text.
        val ar = RecipeLocalization.localize(ApiClient.json.decodeFromString(RecipeFile.serializer(), json), "ar")
        assertEquals("٥٠٠ جم", ar.ingredients[0].amount)
        assertEquals("بصل", ar.ingredients[1].name)
    }

    @Test fun stepsSplitIntoCoreAlternativesAndTips() {
        val l = RecipeLocalization.localize(file, "en")
        assertEquals(listOf(1, 3), l.coreSteps.map { it.n })
        assertEquals(listOf("Oven"), l.alternativeGroups.map { it.first })
        assertEquals(listOf(3), l.tips.map { it.n })
        assertNull("subtitle hidden when equal to title", l.subtitle)
    }

    @Test fun searchRequiresEveryTerm() {
        val hay = mapOf("a" to "chicken rice oven", "b" to "chicken soup", "c" to "rice pudding", "gone" to "chicken rice")
        val known = setOf("a", "b", "c")
        assertEquals(listOf("a"), RecipeSearch.match(hay, "  Chicken   RICE ", known, 10))
        assertEquals(listOf("a", "b"), RecipeSearch.match(hay, "chicken", known, 10))
        assertEquals(1, RecipeSearch.match(hay, "chicken", known, 1).size)
        assertTrue(RecipeSearch.match(hay, "   ", known, 10).isEmpty())
    }

    @Test fun templatesAndVersioning() {
        assertEquals("/data/kids/en/a%20b.json", ApiClient.fillTemplate("/data/kids/{lang}/{id}.json", mapOf("lang" to "en", "id" to "a b")))
        assertEquals("/x.json?v=abc", ApiClient.versionedPath("/x.json", "abc"))
        assertEquals("/x.json", ApiClient.versionedPath("/x.json", null))
        assertEquals("https://fifi.cooking/recipe-images/a.jpg?v=9", ApiClient.resolveAsset("/recipe-images/a.jpg", "9"))
        assertEquals("https://i.ytimg.com/x.jpg", ApiClient.resolveAsset("https://i.ytimg.com/x.jpg", "9"))
        assertNull(ApiClient.resolveAsset("", "9"))
        assertEquals("https://fifi.cooking/recipe/meat-01/", ApiClient.shareUrl("meat-01"))
    }

    @Test fun videoBucketPreference() {
        val v = cooking.fifi.android.data.VideoItem("x", "t")
        assertEquals(1, ApiClient.pickVideos(mapOf("ar" to listOf(v)), "fr").size)
        assertTrue(ApiClient.pickVideos(emptyMap(), "fr").isEmpty())
    }

    @Test fun deepLinks() {
        assertEquals(Section.Home to Route.Recipe("meat-01"), DeepLinks.parse("/recipe/meat-01/"))
        assertEquals(Section.Chapters to Route.ChapterDetail(3), DeepLinks.parse("/chapter/3"))
        assertEquals(Section.Kids to Route.KidsReady("banana-pops"), DeepLinks.parse("/kids/banana-pops"))
        assertNull(DeepLinks.parse("/chapter/abc"))
        assertNull(DeepLinks.parse("/"))
        assertNull(DeepLinks.parse("/unknown/x"))
    }

    @Test fun routesRoundTrip() {
        listOf(Route.Recipe("a"), Route.ChapterDetail(4), Route.KidsReady("k"), Route.KidsSteps("k"), Route.KidsDone("k", "Title / with: slashes"))
            .forEach { assertEquals(it, Route.decode(it.encode())) }
    }

    @Test fun manifestToleratesUnknownFields() {
        val m = ApiClient.json.decodeFromString(TvManifest.serializer(), """{"version":"v1","languages":[{"code":"ar","nativeName":"العربية","englishName":"Arabic","dir":"rtl","complete":true}],"newThing":1}""")
        assertTrue(m.languages.single().isRtl)
        assertEquals("/data/tv/index/{lang}.json", m.endpoints.index)
    }

    @Test fun legacyLanguageCodesNormalize() {
        assertEquals("he", AppViewModel.normalizeLanguage("iw"))
        assertEquals("id", AppViewModel.normalizeLanguage("in"))
        assertEquals("ar", AppViewModel.normalizeLanguage("ar"))
    }

    /** The bundled string tables: every key in every language, Android overrides never name Apple devices. */
    @Test fun bundledStringsCoverEveryKey() {
        val assets = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }
        val strings = UiStrings.parse(File(assets, "ui-strings.json").readText(), File(assets, "ui-strings-android.json").readText())
        val langs = listOf("ar", "de", "el", "en", "es", "fa", "fr", "he", "hi", "id", "it", "ja", "ko", "ku", "nl", "pl", "ps", "pt", "ru", "sv", "sw", "te", "tr", "ur", "zh")
        for (lang in langs) for (key in S.entries) {
            val v = strings.get(lang, key)
            assertTrue("$lang.$key missing", v != key.name || strings.get("en", key) == key.name)
            assertTrue("$lang.$key mentions Apple hardware: $v", !v.contains("iPad") && !v.contains("iPhone") && !v.contains("آیپد") && !v.contains("الآيباد"))
        }
        assertEquals("Step 2 of 5", UiStrings.fill(strings.get("en", S.stepOf), "n" to 2, "t" to 5))
        assertEquals("tree nuts", strings.allergen("en", "nuts"))
    }
}
