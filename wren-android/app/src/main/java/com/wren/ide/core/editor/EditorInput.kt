package com.wren.ide.core.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Editor input engine: auto-closing pairs and auto-indentation.
 *
 * Compose only gives us before/after [TextFieldValue] snapshots, so we diff them
 * to detect single-character insertions and augment the edit (e.g. inserting a
 * closing bracket, or copying indentation onto a new line).
 */
object EditorInput {

    private val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'', '`' to '`')
    private val CLOSERS = PAIRS.values.toSet()

    /**
     * Given the previous value and the new value produced by the text field,
     * returns the value that should actually be applied. Adds closing characters
     * and smart indentation. Falls back to [new] for anything non-trivial.
     */
    fun process(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val oldText = old.text
        val newText = new.text

        // Only augment simple single-character insertions at the caret.
        if (newText.length != oldText.length + 1) return new
        val caret = new.selection.end
        if (caret <= 0 || caret > newText.length) return new

        val inserted = newText[caret - 1]

        // 1. Newline -> replicate leading whitespace and expand block braces.
        if (inserted == '\n') {
            return handleNewline(newText, caret)
        }

        // 2. Typing a closer right before the same closer -> just step over it.
        if (inserted in CLOSERS && caret < newText.length && newText[caret] == inserted) {
            val without = newText.removeRange(caret - 1, caret)
            return TextFieldValue(without, TextRange(caret))
        }

        // 3. Opening char -> insert the matching closer after the caret.
        val closer = PAIRS[inserted]
        if (closer != null) {
            // Avoid doubling quotes when the next char is alphanumeric (likely mid-word).
            val nextChar = newText.getOrNull(caret)
            if ((inserted == '"' || inserted == '\'' || inserted == '`') && nextChar?.isLetterOrDigit() == true) {
                return new
            }
            val augmented = newText.substring(0, caret) + closer + newText.substring(caret)
            return TextFieldValue(augmented, TextRange(caret))
        }

        return new
    }

    private fun handleNewline(text: String, caret: Int): TextFieldValue {
        val lineStart = text.lastIndexOf('\n', caret - 2).let { if (it == -1) 0 else it + 1 }
        val currentLine = text.substring(lineStart, (caret - 1).coerceAtLeast(lineStart))
        val indent = currentLine.takeWhile { it == ' ' || it == '\t' }

        val prevNonSpace = currentLine.trimEnd().lastOrNull()
        val nextChar = text.getOrNull(caret)

        // Expand `{|}` into a fully indented block.
        if (prevNonSpace == '{' && nextChar == '}') {
            val innerIndent = "$indent    "
            val insertion = innerIndent + "\n" + indent
            val newText = text.substring(0, caret) + insertion + text.substring(caret)
            return TextFieldValue(newText, TextRange(caret + innerIndent.length))
        }

        // Extra indent step after an opening brace/paren/bracket.
        val extra = if (prevNonSpace == '{' || prevNonSpace == '(' || prevNonSpace == '[') "    " else ""
        val insertion = indent + extra
        if (insertion.isEmpty()) return TextFieldValue(text, TextRange(caret))
        val newText = text.substring(0, caret) + insertion + text.substring(caret)
        return TextFieldValue(newText, TextRange(caret + insertion.length))
    }
}
