package cooking.fifi.android.data

/** Top-level destinations: bottom bar on phones, rail on foldables, drawer on tablets. */
enum class Section { Home, Chapters, Search, Kids, Settings }

/** Screens pushed onto a section's back stack. */
sealed interface Route {
    data class Recipe(val id: String) : Route
    data class ChapterDetail(val id: Int) : Route
    data class KidsReady(val id: String) : Route
    data class KidsSteps(val id: String) : Route
    data class KidsDone(val id: String, val title: String) : Route

    /** Compact string form for SavedStateHandle (process-death restore). */
    fun encode(): String = when (this) {
        is Recipe -> "recipe\u0000$id"
        is ChapterDetail -> "chapter\u0000$id"
        is KidsReady -> "kidsReady\u0000$id"
        is KidsSteps -> "kidsSteps\u0000$id"
        is KidsDone -> "kidsDone\u0000$id\u0000$title"
    }

    companion object {
        fun decode(s: String): Route? {
            val p = s.split('\u0000')
            return when (p.firstOrNull()) {
                "recipe" -> p.getOrNull(1)?.let(::Recipe)
                "chapter" -> p.getOrNull(1)?.toIntOrNull()?.let(::ChapterDetail)
                "kidsReady" -> p.getOrNull(1)?.let(::KidsReady)
                "kidsSteps" -> p.getOrNull(1)?.let(::KidsSteps)
                "kidsDone" -> if (p.size >= 3) KidsDone(p[1], p[2]) else null
                else -> null
            }
        }
    }
}

/**
 * App Links: https://fifi.cooking/recipe/{id}/, /chapter/{n}, /kids/{id} —
 * the same paths the iOS AASA file claims.
 */
object DeepLinks {
    fun parse(path: String?): Pair<Section, Route>? {
        val parts = path.orEmpty().split('/').filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val id = java.net.URLDecoder.decode(parts[1], "UTF-8")
        return when (parts[0]) {
            "recipe" -> Section.Home to Route.Recipe(id)
            "chapter" -> id.toIntOrNull()?.let { Section.Chapters to Route.ChapterDetail(it) }
            "kids" -> Section.Kids to Route.KidsReady(id)
            else -> null
        }
    }
}
