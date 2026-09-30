package cooking.fifi.android.data

/**
 * View-ready projection of a RecipeFile — a direct port of `localize()` in the
 * TV app's RecipeScreen (and the iOS RecipeLocalization), with its rules:
 *
 *  * `ar`: master recipe.* fields are Arabic; translations.ar is empty by
 *    design, so Arabic falls back to the master fields, never English.
 *  * steps: prefer the language's instruction text; else `ui.textEn` for
 *    non-Arabic, `ui.text` for Arabic.
 *  * culturalNotes: ONLY the requested language's note — never English.
 */
object RecipeLocalization {

    data class Ingredient(val name: String, val amount: String?)

    data class Step(
        val n: Int,
        val text: String,
        val phase: String?,
        val alternative: String?,
        val isTip: Boolean,
    )

    data class Localized(
        val title: String,
        val subtitle: String?,
        val chapter: String?,
        val category: String?,
        val cookingMethod: String?,
        val prepTime: String?,
        val cookTime: String?,
        val servings: String?,
        val culturalNotes: String?,
        val ingredients: List<Ingredient>,
        val steps: List<Step>,
    ) {
        val coreSteps get() = steps.filter { it.alternative == null }
        val tips get() = steps.filter { it.isTip }

        /** Alternative-method steps grouped by label, in first-seen order. */
        val alternativeGroups: List<Pair<String, List<Step>>>
            get() = steps.filter { it.alternative != null }
                .groupBy { it.alternative!! }
                .toList()
    }

    fun localize(file: RecipeFile, lang: String): Localized {
        val r = file.recipe
        val t = file.translations[lang] ?: RecipeTranslation()
        val en = file.translations["en"]
        val isAr = lang == "ar"

        fun pick(tr: String?, enV: String?, master: String?): String? =
            tr ?: if (isAr) master else (enV ?: master)

        val trIng = t.ingredients ?: if (isAr) emptyMap() else en?.ingredients.orEmpty()
        val trIns = t.instructions ?: if (isAr) emptyMap() else en?.instructions.orEmpty()
        val title = pick(t.title, en?.title, r.title) ?: r.title

        return Localized(
            title = title,
            subtitle = r.titleEn?.takeIf { it != title },
            chapter = pick(t.chapter, en?.chapter, r.chapter),
            category = pick(t.category, en?.category, r.category),
            cookingMethod = pick(t.cookingMethod, en?.cookingMethod, r.cookingMethod),
            prepTime = pick(t.prepTime, en?.prepTime, r.prepTime),
            cookTime = pick(t.cookTime, en?.cookTime, r.cookTime),
            servings = pick(t.servings, en?.servings, r.servings),
            culturalNotes = t.culturalNotes?.takeIf { it.isNotBlank() },
            ingredients = r.masterIngredients.map { mi ->
                Ingredient(
                    name = trIng[mi.id]?.name ?: mi.name,
                    amount = trIng[mi.id]?.standardAmount ?: mi.standardAmount,
                )
            },
            steps = r.uniqueInstructions.map { ui ->
                Step(
                    n = ui.stepNumber,
                    text = trIns[ui.stepNumber.toString()] ?: if (isAr) ui.text else (ui.textEn ?: ui.text),
                    phase = ui.phase,
                    alternative = if (ui.isAlternative == true) (ui.alternativeLabel ?: "alternative") else null,
                    isTip = ui.importance == "tip",
                )
            },
        )
    }
}
