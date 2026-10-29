package io.github.hatake716.rocketboy

enum class QuoteCategory(val key: String, val title: String, val description: String) {
    HISTORY("history", "歴史上の人物", "思想家や歴史に名を残した人々の言葉"),
    BUSINESS("business", "企業経営者", "事業、仕事、人との関わりについての言葉"),
    FICTION("fiction", "アニメ・漫画作品", "物語の登場人物たちの言葉"),
    GAME("game", "ゲームの名言", "ゲームの台詞・メッセージ・キャッチコピー"),
    PROVERB("proverb", "ことわざ", "暮らしの中で受け継がれてきた知恵"),
    HYAKUNIN("hyakunin", "百人一首", "小倉百人一首の百首と、その情景・心情");

    val showsWorkTitle: Boolean get() = this == FICTION || this == GAME
}

data class QuoteAuthor(val id: String, val name: String, val period: String, val biography: String,
    val isWork: Boolean = false)
data class HistoricalQuote(
    val id: String, val text: String, val reading: String, val authorId: String,
    val explanation: String, val source: String, val sourceUrl: String,
    val category: QuoteCategory = QuoteCategory.HISTORY,
    val wording: String = "原文・慣用表記・本アプリ訳",
    val license: String = "",
) {
    val points get() = Scoring.quote(reading)
    val author get() = Quotes.authorById.getValue(authorId)
}

/** Stable IDs survive updates; quote text is Wikipedia-sourced and explanations are authored for this app. */
object Quotes {
    const val FIRST_APPEARANCE = 25.0
    const val INTERVAL = 35.0
    const val MAX_READING_LENGTH = 64
    private fun table(name: String, columns: Int): List<List<String>> =
        requireNotNull(javaClass.getResourceAsStream("/quotes/$name.tsv")) { "Missing quote data: $name" }
            .bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
                    line.split('\t').also { require(it.size == columns) { "Malformed $name row" } }
                        .map { it.replace("\\n", "\n") }
                }.toList()
            }
    val authors: List<QuoteAuthor> by lazy {
        table("authors", 5).map { QuoteAuthor(it[0], it[1], it[2], it[3], it[4] == "work") }
    }
    val authorById: Map<String, QuoteAuthor> by lazy { authors.associateBy { it.id } }
    val all: List<HistoricalQuote> by lazy {
        table("catalog", 10).map { row ->
            HistoricalQuote(row[0], row[1], row[2], row[3], row[4], row[5], row[6],
                QuoteCategory.entries.single { it.key == row[7] }, row[8], row[9])
        }
    }
    private val byId by lazy { all.associateBy { it.id } }
    fun find(id: String?) = byId[id]

    // Keep only retired IDs/readings for save migration; they never enter the playable catalog.
    private val retiredReadings by lazy { table("retired", 2).associate { it[0] to it[1] } }
    fun isRetired(id: String?, reading: String) = id != null && retiredReadings[id] == reading

    fun inCategory(category: QuoteCategory) = all.filter { it.category == category }

    fun search(category: QuoteCategory?, collected: Set<String>, query: String = ""): List<HistoricalQuote> {
        val needle = GameEngine.normalizeKana(query).lowercase(java.util.Locale.ROOT)
        return all.filter { quote ->
            quote.id in collected && (category == null || quote.category == category) &&
                (needle.isBlank() || listOf(quote.text, quote.reading, quote.author.name, quote.author.period, quote.explanation)
                    .any { GameEngine.normalizeKana(it).lowercase(java.util.Locale.ROOT).contains(needle) })
        }
    }
}
