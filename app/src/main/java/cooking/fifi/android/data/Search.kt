package cooking.fifi.android.data

/**
 * On-device search over /data/search/{lang}.json — the TV app's rule: every
 * whitespace-separated term must appear in the recipe's lowercase haystack.
 */
object RecipeSearch {
    fun match(
        haystack: Map<String, String>,
        query: String,
        known: Set<String>,
        limit: Int,
    ): List<String> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        for ((id, text) in haystack) {
            if (id in known && terms.all { text.contains(it) }) {
                out += id
                if (out.size >= limit) break
            }
        }
        return out
    }
}
