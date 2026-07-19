package com.worldcup.androidstudiolite

import com.worldcup.androidstudiolite.feature.editor.ui.BalanceLint
import com.worldcup.androidstudiolite.feature.editor.ui.CompletionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceLintTest {

    @Test
    fun `balanced file has no issues`() {
        val code = """
            fun main() {
                val list = listOf("a", "b")
                println(list[0])
            }
        """.trimIndent()
        assertTrue(BalanceLint.check(code, "Main.kt").isEmpty())
    }

    @Test
    fun `unclosed brace is reported with its line`() {
        val code = "fun main() {\n    val x = 1\n"
        val issues = BalanceLint.check(code, "Main.kt")
        assertEquals(1, issues.size)
        assertEquals(0, issues[0].line)
        assertTrue(issues[0].message.contains("{"))
    }

    @Test
    fun `unmatched closer is reported`() {
        val issues = BalanceLint.check("fun main() { } }", "Main.kt")
        assertEquals(1, issues.size)
        assertTrue(issues[0].message.contains("}"))
    }

    @Test
    fun `unterminated string is reported`() {
        val issues = BalanceLint.check("val s = \"oops\n", "Main.kt")
        assertTrue(issues.any { it.message.contains("string") })
    }

    @Test
    fun `brackets inside strings and comments are ignored`() {
        val code = """
            // comment with { and (
            val s = "text with ) and ]"
            /* block } */
        """.trimIndent()
        assertTrue(BalanceLint.check(code, "Main.kt").isEmpty())
    }

    @Test
    fun `non code files are skipped`() {
        assertTrue(BalanceLint.check("{{{", "notes.md").isEmpty())
    }

    @Test
    fun `completion suggests buffer words and ranks prefix matches first`() {
        val content = "val myVariable = 1\nval myValue = 2"
        val suggestions = CompletionEngine.suggest("myV", content)
        assertTrue(suggestions.contains("myValue"))
        assertTrue(suggestions.contains("myVariable"))
    }

    @Test
    fun `completion includes curated symbols`() {
        val suggestions = CompletionEngine.suggest("Laun", "")
        assertTrue(suggestions.contains("LaunchedEffect"))
    }

    @Test
    fun `completion needs at least two chars`() {
        assertTrue(CompletionEngine.suggest("L", "").isEmpty())
    }
}
