package com.wren.ide.core.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.wren.ide.core.theme.EditorYellow
import com.wren.ide.core.theme.ElectricCyan
import com.wren.ide.core.theme.TerminalGreen
import com.wren.ide.core.theme.TextLight
import com.wren.ide.core.theme.TextMuted

/**
 * Language-aware syntax highlighter. Detects the language from a file extension
 * and applies keyword / string / number / comment styling. Pure and stateless so
 * it can be used inside a Compose [androidx.compose.ui.text.input.VisualTransformation].
 */
object SyntaxHighlighter {

    private val KEYWORD_COLOR = EditorYellow
    private val STRING_COLOR = TerminalGreen
    private val NUMBER_COLOR = ElectricCyan
    private val COMMENT_COLOR = TextMuted
    private val TYPE_COLOR = Color(0xFF6EC1FF)
    private val ANNOTATION_COLOR = Color(0xFFC792EA)

    enum class Language { KOTLIN, JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT, RUST, GO, C_CPP, JSON, MARKUP, PLAIN }

    fun detectLanguage(fileName: String): Language = when (fileName.substringAfterLast('.', "").lowercase()) {
        "kt", "kts" -> Language.KOTLIN
        "java" -> Language.JAVA
        "py" -> Language.PYTHON
        "js", "jsx", "mjs", "cjs" -> Language.JAVASCRIPT
        "ts", "tsx" -> Language.TYPESCRIPT
        "rs" -> Language.RUST
        "go" -> Language.GO
        "c", "h", "cpp", "cc", "hpp", "cxx" -> Language.C_CPP
        "json" -> Language.JSON
        "html", "htm", "xml", "css", "yml", "yaml", "md" -> Language.MARKUP
        else -> Language.PLAIN
    }

    private val COMMON = setOf(
        "if", "else", "for", "while", "return", "true", "false", "null", "break",
        "continue", "class", "import", "new", "try", "catch", "throw", "switch", "case"
    )

    private fun keywordsFor(language: Language): Set<String> = when (language) {
        Language.KOTLIN -> COMMON + setOf(
            "package", "fun", "val", "var", "object", "interface", "when", "is", "in",
            "private", "public", "protected", "internal", "override", "suspend", "data",
            "sealed", "companion", "init", "constructor", "lateinit", "by", "as", "this",
            "super", "enum", "abstract", "open", "const", "operator", "vararg", "typealias"
        )
        Language.JAVA -> COMMON + setOf(
            "package", "public", "private", "protected", "static", "final", "void", "int",
            "long", "double", "float", "boolean", "char", "byte", "short", "abstract",
            "extends", "implements", "interface", "enum", "this", "super", "synchronized",
            "volatile", "transient", "instanceof", "throws", "final"
        )
        Language.PYTHON -> setOf(
            "def", "class", "import", "from", "as", "if", "elif", "else", "for", "while",
            "return", "yield", "try", "except", "finally", "with", "lambda", "pass", "None",
            "True", "False", "and", "or", "not", "in", "is", "global", "nonlocal", "async", "await"
        )
        Language.JAVASCRIPT, Language.TYPESCRIPT -> COMMON + setOf(
            "function", "const", "let", "var", "async", "await", "export", "default",
            "extends", "implements", "interface", "type", "enum", "of", "this", "typeof",
            "instanceof", "void", "yield", "static", "get", "set", "public", "private", "readonly"
        )
        Language.RUST -> COMMON + setOf(
            "fn", "let", "mut", "pub", "struct", "enum", "impl", "trait", "use", "mod",
            "match", "loop", "move", "ref", "self", "Self", "where", "async", "await", "dyn", "unsafe", "crate"
        )
        Language.GO -> COMMON + setOf(
            "func", "var", "const", "package", "type", "struct", "interface", "map",
            "chan", "go", "defer", "select", "range", "fallthrough", "goto"
        )
        Language.C_CPP -> COMMON + setOf(
            "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed",
            "struct", "union", "enum", "typedef", "const", "static", "extern", "sizeof",
            "include", "define", "namespace", "template", "using", "public", "private", "virtual"
        )
        Language.JSON -> setOf("true", "false", "null")
        else -> emptySet()
    }

    fun highlight(text: String, language: Language): AnnotatedString = buildAnnotatedString {
        if (language == Language.PLAIN || language == Language.MARKUP) {
            append(text)
            return@buildAnnotatedString
        }

        val keywords = keywordsFor(language)
        val lineComment = when (language) {
            Language.PYTHON -> "#"
            else -> "//"
        }

        var index = 0
        val tokenRegex = Regex(
            """(#.*|//.*)|("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|`(?:\\.|[^`\\])*`)|(@[A-Za-z_][A-Za-z0-9_]*)|(\b\d[\d._]*[fFlLdD]?\b)|([A-Za-z_][A-Za-z0-9_]*)|([^\s])"""
        )

        tokenRegex.findAll(text).forEach { match ->
            val value = match.value
            val start = match.range.first
            if (start > index) append(text.substring(index, start))

            when {
                (value.startsWith("//") || value.startsWith("#")) && lineComment.isNotEmpty() ->
                    withStyle(SpanStyle(color = COMMENT_COLOR, fontStyle = FontStyle.Italic)) { append(value) }

                value.startsWith("\"") || value.startsWith("'") || value.startsWith("`") ->
                    withStyle(SpanStyle(color = STRING_COLOR)) { append(value) }

                value.startsWith("@") ->
                    withStyle(SpanStyle(color = ANNOTATION_COLOR)) { append(value) }

                value.firstOrNull()?.isDigit() == true ->
                    withStyle(SpanStyle(color = NUMBER_COLOR)) { append(value) }

                keywords.contains(value) ->
                    withStyle(SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold)) { append(value) }

                value.first().isUpperCase() && value.length > 1 ->
                    withStyle(SpanStyle(color = TYPE_COLOR)) { append(value) }

                else -> withStyle(SpanStyle(color = TextLight)) { append(value) }
            }
            index = match.range.last + 1
        }

        if (index < text.length) append(text.substring(index))
    }
}
