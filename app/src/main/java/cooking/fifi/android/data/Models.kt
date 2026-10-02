package cooking.fifi.android.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.roundToInt

/*
 * Models matching docs/tv-api.md in the fifirecipes repo — the same contract
 * the Fire TV (src/api/types.ts), iPhone and iPad clients decode.
 */

@Serializable
data class LanguageInfo(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val dir: String = "ltr",
    val complete: Boolean = true,
) {
    val isRtl: Boolean get() = dir == "rtl"
}

@Serializable
data class EndpointTemplates(
    val index: String = "/data/tv/index/{lang}.json",
    val feed: String = "/data/tv/feed/{lang}.json",
    val chapters: String = "/data/tv/chapters/{lang}.json",
    val kids: String = "/data/tv/kids/{lang}.json",
    val recipe: String = "/data/recipes/{id}.json",
    val i18n: String = "/data/i18n/{lang}.json",
    val search: String = "/data/search/{lang}.json",
    val videos: String = "/data/videos/{id}.json",
    val kidsRecipe: String = "/data/kids/{lang}/{id}.json",
    val images: String = "/data/tv/images.json",
)

@Serializable
data class TvManifest(
    val version: String,
    val generatedAt: String = "",
    val recipeCount: Int = 0,
    val pageSize: Int = 100,
    val languages: List<LanguageInfo>,
    val endpoints: EndpointTemplates = EndpointTemplates(),
)

/** One entry in tv/index/{lang}.json */
@Serializable
data class RecipeCard(
    val id: String,
    val title: String,
    val titleEn: String? = null,
    val category: String? = null,
    val cookingMethod: String? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val difficulty: String? = null,
    val image: String? = null,
    val hasVideo: Boolean? = null,
    val chapter: Int? = null,
    val chapterName: String? = null,
)

@Serializable
data class FeedRow(val key: String, val title: String, val items: List<String>)

@Serializable
data class Feed(val rows: List<FeedRow> = emptyList())

@Serializable
data class Chapter(val id: Int, val name: String, val recipeCount: Int, val coverImage: String? = null)

@Serializable
data class KidsCard(
    val id: String,
    val title: String,
    val group: String,
    val ages: String,
    val minutes: Int,
    val noCook: Boolean = false,
    val allergens: List<String>? = null,
    val cover: String,
)

@Serializable
data class ImageInfo(
    val card: String,
    val card2x: String? = null,
    val full: String,
    val full2x: String? = null,
    val w: Int = 0,
    val h: Int = 0,
)

/* /data/recipes/{id}.json */

@Serializable
data class RecipeIngredient(
    val id: String,
    val name: String,
    val standardAmount: String? = null,
    val notes: String? = null,
)

@Serializable
data class RecipeInstruction(
    val stepNumber: Int,
    val text: String,
    val textEn: String? = null,
    val phase: String? = null,
    val isAlternative: Boolean? = null,
    val alternativeLabel: String? = null,
    val importance: String? = null,
)

@Serializable
data class RecipeCore(
    val id: String,
    val title: String,
    val titleEn: String? = null,
    val chapter: String? = null,
    val chapterNumber: Int? = null,
    val category: String? = null,
    val cookingMethod: String? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val masterIngredients: List<RecipeIngredient> = emptyList(),
    val uniqueInstructions: List<RecipeInstruction> = emptyList(),
)

@Serializable
data class IngredientTranslation(val name: String? = null, val standardAmount: String? = null)

@Serializable
data class RecipeTranslation(
    val title: String? = null,
    val chapter: String? = null,
    val category: String? = null,
    val cookingMethod: String? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val culturalNotes: String? = null,
    val ingredients: Map<String, IngredientTranslation>? = null,
    val instructions: Map<String, String>? = null,
)

// Estimates are model-generated and sometimes fractional (servings 3.5 in 54
// recipes). Strict Int decoding rejected the whole recipe file, so accept any
// number and round it.
@Serializable
data class RecipeEstimate(
    @Serializable(with = RoundedIntSerializer::class) val servings: Int? = null,
    @Serializable(with = RoundedIntSerializer::class) val kcal: Int? = null,
    @Serializable(with = RoundedIntSerializer::class) val protein: Int? = null,
    @Serializable(with = RoundedIntSerializer::class) val fat: Int? = null,
    @Serializable(with = RoundedIntSerializer::class) val carbs: Int? = null,
    @Serializable(with = RoundedIntSerializer::class) val fiber: Int? = null,
    @Serializable(with = RoundedIntSerializer::class) val sugar: Int? = null,
)

/** Any JSON number, rounded to the nearest Int; anything else decodes as null. */
object RoundedIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RoundedInt", PrimitiveKind.INT).nullable

    override fun deserialize(decoder: Decoder): Int? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return (element as? JsonPrimitive)?.doubleOrNull?.roundToInt()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

/** The estimate is optional garnish: a malformed one must never stop the recipe opening. */
object LenientEstimateSerializer : KSerializer<RecipeEstimate?> {
    override val descriptor: SerialDescriptor = RecipeEstimate.serializer().descriptor.nullable

    override fun deserialize(decoder: Decoder): RecipeEstimate? {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement()
        return runCatching { json.json.decodeFromJsonElement(RecipeEstimate.serializer(), element) }.getOrNull()
    }

    override fun serialize(encoder: Encoder, value: RecipeEstimate?) {
        if (value == null) encoder.encodeNull() else encoder.encodeSerializableValue(RecipeEstimate.serializer(), value)
    }
}

@Serializable
data class RecipeFile(
    val recipe: RecipeCore,
    @Serializable(with = LenientEstimateSerializer::class) val estimate: RecipeEstimate? = null,
    val translations: Map<String, RecipeTranslation> = emptyMap(),
)

/** /data/videos/{id}.json — YouTube hits per language. */
@Serializable
data class VideoItem(
    val id: String,
    val title: String,
    val channel: String? = null,
    val duration: String? = null,
    val views: String? = null,
    val short: Boolean? = null,
)

/* /data/kids/{lang}/{id}.json */

@Serializable
data class KidsIngredient(val art: String, val text: String)

@Serializable
data class KidsStep(
    val act: String? = null,
    val items: List<String>? = null,
    val tool: String? = null,
    val adult: String? = null,
    val timer: Int? = null,
    val text: String,
)

@Serializable
data class KidsRecipeDetail(
    val id: String,
    val group: String,
    val ages: String,
    val minutes: Int,
    val servings: Int? = null,
    val noCook: Boolean = false,
    val allergens: List<String>? = null,
    val cover: String,
    val title: String,
    val intro: String? = null,
    val ingredients: List<KidsIngredient> = emptyList(),
    val tools: List<String>? = null,
    val steps: List<KidsStep> = emptyList(),
    val tip: String? = null,
)
