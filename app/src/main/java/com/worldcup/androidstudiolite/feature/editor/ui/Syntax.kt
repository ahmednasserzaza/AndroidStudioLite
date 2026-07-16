package com.worldcup.androidstudiolite.feature.editor.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import com.worldcup.androidstudiolite.designsystem.theme.AslDarkColors

object Syntax {

    private val palette = AslDarkColors.syntax

    private val KEYWORD = SpanStyle(color = palette.keyword)
    private val STRING = SpanStyle(color = palette.string)
    private val COMMENT = SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
    private val NUMBER = SpanStyle(color = palette.number)
    private val ANNOTATION = SpanStyle(color = palette.annotation)
    private val FUNCTION = SpanStyle(color = palette.function)
    private val TAG = SpanStyle(color = palette.keyword)
    private val ATTR = SpanStyle(color = palette.number)

    private val KOTLIN_KEYWORDS = (
        "package|import|class|interface|object|fun|val|var|if|else|when|for|while|do|return|" +
            "break|continue|is|in|as|null|true|false|this|super|throw|try|catch|finally|" +
            "private|public|protected|internal|override|open|abstract|final|sealed|data|enum|" +
            "companion|lateinit|by|lazy|suspend|inline|reified|typealias|constructor|init|out|vararg"
        )

    private data class Rule(val regex: Regex, val style: SpanStyle, val group: Int = 0)

    private val kotlinRules = listOf(
        Rule(Regex("//.*"), COMMENT),
        Rule(Regex("/\\*[\\s\\S]*?\\*/"), COMMENT),
        Rule(Regex("\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\""), STRING),
        Rule(Regex("'(?:\\\\.|[^'\\\\\\n])'"), STRING),
        Rule(Regex("@\\w+"), ANNOTATION),
        Rule(Regex("\\b($KOTLIN_KEYWORDS)\\b"), KEYWORD),
        Rule(Regex("\\b(\\w+)\\s*\\("), FUNCTION, group = 1),
        Rule(Regex("\\b\\d+(\\.\\d+)?[fFLdu]?\\b"), NUMBER),
    )

    private val xmlRules = listOf(
        Rule(Regex("<!--[\\s\\S]*?-->"), COMMENT),
        Rule(Regex("\"[^\"\\n]*\""), STRING),
        Rule(Regex("</?[\\w.-]+|/?>"), TAG),
        Rule(Regex("\\b[\\w:.-]+(?==)"), ATTR),
    )

    private val yamlRules = listOf(
        Rule(Regex("#.*"), COMMENT),
        Rule(Regex("(\"[^\"\\n]*\"|'[^'\\n]*')"), STRING),
        Rule(Regex("^\\s*-?\\s*([\\w.-]+)\\s*:", RegexOption.MULTILINE), ATTR, group = 1),
        Rule(Regex("\\b\\d+(\\.\\d+)?\\b"), NUMBER),
    )

    private val jsonRules = listOf(
        Rule(Regex("\"(?:\\\\.|[^\"\\\\])*\"(?=\\s*:)"), ATTR),
        Rule(Regex("\"(?:\\\\.|[^\"\\\\])*\""), STRING),
        Rule(Regex("\\b(true|false|null)\\b"), KEYWORD),
        Rule(Regex("-?\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b"), NUMBER),
    )

    private val propRules = listOf(
        Rule(Regex("^\\s*[#!].*", RegexOption.MULTILINE), COMMENT),
        Rule(Regex("^\\s*([\\w.-]+)\\s*(?==)", RegexOption.MULTILINE), ATTR, group = 1),
    )

    private fun rulesFor(fileName: String): List<Rule> {
        val name = fileName.lowercase()
        return when {
            name.endsWith(".kt") || name.endsWith(".kts") || name.endsWith(".gradle") ||
                name.endsWith(".java") -> kotlinRules
            name.endsWith(".xml") -> xmlRules
            name.endsWith(".yml") || name.endsWith(".yaml") -> yamlRules
            name.endsWith(".json") -> jsonRules
            name.endsWith(".properties") || name == ".gitignore" -> propRules
            else -> emptyList()
        }
    }

    fun highlight(text: String, fileName: String): AnnotatedString {
        val rules = rulesFor(fileName)
        if (rules.isEmpty() || text.length > 200_000) return AnnotatedString(text)

        val styles = arrayOfNulls<SpanStyle>(text.length)
        for (rule in rules) {
            for (match in rule.regex.findAll(text)) {
                val range = if (rule.group == 0) match.range
                else (match.groups[rule.group]?.range ?: continue)
                var taken = false
                for (i in range) if (styles[i] != null) { taken = true; break }
                if (taken) continue
                for (i in range) styles[i] = rule.style
            }
        }
        return buildAnnotatedString {
            append(text)
            var i = 0
            while (i < text.length) {
                val style = styles[i]
                if (style == null) { i++; continue }
                var end = i
                while (end < text.length && styles[end] === style) end++
                addStyle(style, i, end)
                i = end
            }
        }
    }
}
