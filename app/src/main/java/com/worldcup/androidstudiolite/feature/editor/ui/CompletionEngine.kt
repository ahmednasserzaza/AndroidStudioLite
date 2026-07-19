package com.worldcup.androidstudiolite.feature.editor.ui

object CompletionEngine {

    private val WORD = Regex("[A-Za-z_][A-Za-z0-9_]{2,}")

    private val KOTLIN_KEYWORDS = listOf(
        "abstract", "class", "companion", "const", "constructor", "continue", "data",
        "else", "enum", "false", "final", "finally", "fun", "import", "interface",
        "internal", "lateinit", "object", "override", "package", "private", "protected",
        "public", "return", "sealed", "suspend", "true", "typealias", "val", "var",
        "when", "while",
    )

    private val ANDROID_SYMBOLS = listOf(
        "Modifier", "Composable", "remember", "mutableStateOf", "LaunchedEffect",
        "rememberCoroutineScope", "collectAsState", "ViewModel", "viewModelScope",
        "MutableStateFlow", "StateFlow", "Column", "Row", "Box", "Text", "Button",
        "Spacer", "LazyColumn", "Alignment", "Arrangement", "padding", "fillMaxSize",
        "fillMaxWidth", "background", "clickable", "getValue", "setValue", "launch",
        "override", "Intent", "Context", "Bundle", "savedInstanceState",
    )

    fun suggest(
        prefix: String,
        activeContent: String,
        otherContents: List<String> = emptyList(),
        limit: Int = 6,
    ): List<String> {
        if (prefix.length < 2) return emptyList()

        val fromActive = wordsIn(activeContent)
        val fromOthers = otherContents.asSequence().flatMap { wordsIn(it) }.toSet()

        fun rank(candidates: Collection<String>, tier: Int): List<Pair<String, Int>> =
            candidates.mapNotNull { word ->
                when {
                    word == prefix -> null
                    word.startsWith(prefix) -> word to tier
                    word.startsWith(prefix, ignoreCase = true) -> word to tier + 1
                    else -> null
                }
            }

        return (
            rank(fromActive, tier = 0) +
                rank(KOTLIN_KEYWORDS, tier = 2) +
                rank(ANDROID_SYMBOLS, tier = 2) +
                rank(fromOthers, tier = 4)
            )
            .sortedWith(compareBy({ it.second }, { it.first.length }, { it.first }))
            .map { it.first }
            .distinct()
            .take(limit)
    }

    private fun wordsIn(content: String): Set<String> {
        val capped = if (content.length > MAX_SCAN_CHARS) content.take(MAX_SCAN_CHARS) else content
        return WORD.findAll(capped).mapTo(mutableSetOf()) { it.value }
    }

    private const val MAX_SCAN_CHARS = 100_000
}
