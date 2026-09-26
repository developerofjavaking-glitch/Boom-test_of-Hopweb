package com.example.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

object CodeHighlighter {

    val ColorTag = Color(0xFF38BDF8)       // Light blue
    val ColorAttr = Color(0xFFA78BFA)      // Lavender violet
    val ColorString = Color(0xFF34D399)    // Mint emerald
    val ColorKeyword = Color(0xFFF43F5E)   // Rose red
    val ColorFunction = Color(0xFF60A5FA)  // Electric blue
    val ColorNumber = Color(0xFFFBBF24)    // Warm amber
    val ColorComment = Color(0xFF64748B)   // Slate gray
    val ColorProperty = Color(0xFF22D3EE)  // Cyan
    val ColorSelector = Color(0xFFF472B6)  // Pink

    private val HTML_TAG_PATTERN = Pattern.compile("</?[a-zA-Z0-9_-]+|/?>")
    private val HTML_ATTR_PATTERN = Pattern.compile("\\s+([a-zA-Z0-9_-]+)(?==)")
    private val STRING_DOUBLE_PATTERN = Pattern.compile("\"[^\"]*\"")
    private val STRING_SINGLE_PATTERN = Pattern.compile("'[^']*'")
    private val STRING_BACKTICK_PATTERN = Pattern.compile("`[^`]*`")
    private val HTML_COMMENT_PATTERN = Pattern.compile("<!--[\\s\\S]*?-->")
    private val LINE_COMMENT_PATTERN = Pattern.compile("//.*")
    private val BLOCK_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/")

    private val JS_KEYWORD_PATTERN = Pattern.compile(
        "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|default|try|catch|finally|throw|new|typeof|instanceof|class|extends|super|this|import|export|from|as|async|await|yield|null|undefined|true|false)\\b"
    )
    private val JS_BUILTIN_PATTERN = Pattern.compile(
        "\\b(console|window|document|Math|JSON|Array|Object|String|Number|Boolean|Date|Promise|setTimeout|setInterval|clearTimeout|clearInterval|localStorage|sessionStorage|fetch|alert)\\b"
    )
    private val NUMBER_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?(px|vh|vw|em|rem|%|s|ms|deg|Hz|kHz)?\\b")

    private val CSS_PROPERTY_PATTERN = Pattern.compile("\\b([a-zA-Z-]+)(?=\\s*:)")
    private val CSS_SELECTOR_PATTERN = Pattern.compile("(^|\\n)\\s*([.#]?[a-zA-Z0-9_:-]+)(\\s*[,{])")

    fun highlight(text: String, extension: String): AnnotatedString {
        if (text.length > 50000) {
            // Guard for extremely large files to prevent UI lag
            return AnnotatedString(text)
        }

        return buildAnnotatedString {
            append(text)

            when (extension.lowercase()) {
                "html", "htm" -> highlightHtml(text)
                "css" -> highlightCss(text)
                "js", "javascript" -> highlightJs(text)
                "json" -> highlightJson(text)
                else -> {
                    // Try generic highlight
                    highlightGeneric(text)
                }
            }
        }
    }

    private fun AnnotatedString.Builder.highlightHtml(text: String) {
        // Tag names
        applyRegex(text, HTML_TAG_PATTERN, ColorTag, FontWeight.SemiBold)
        // Attributes
        applyRegex(text, HTML_ATTR_PATTERN, ColorAttr)
        // Strings
        applyRegex(text, STRING_DOUBLE_PATTERN, ColorString)
        applyRegex(text, STRING_SINGLE_PATTERN, ColorString)
        // Comments
        applyRegex(text, HTML_COMMENT_PATTERN, ColorComment)
    }

    private fun AnnotatedString.Builder.highlightCss(text: String) {
        // Selectors
        applyRegex(text, CSS_SELECTOR_PATTERN, ColorSelector, FontWeight.Medium)
        // Properties
        applyRegex(text, CSS_PROPERTY_PATTERN, ColorProperty)
        // Numbers & units
        applyRegex(text, NUMBER_PATTERN, ColorNumber)
        // Strings
        applyRegex(text, STRING_DOUBLE_PATTERN, ColorString)
        applyRegex(text, STRING_SINGLE_PATTERN, ColorString)
        // Comments
        applyRegex(text, BLOCK_COMMENT_PATTERN, ColorComment)
    }

    private fun AnnotatedString.Builder.highlightJs(text: String) {
        // Keywords
        applyRegex(text, JS_KEYWORD_PATTERN, ColorKeyword, FontWeight.Bold)
        // Builtins
        applyRegex(text, JS_BUILTIN_PATTERN, ColorFunction, FontWeight.Medium)
        // Numbers
        applyRegex(text, NUMBER_PATTERN, ColorNumber)
        // Strings
        applyRegex(text, STRING_DOUBLE_PATTERN, ColorString)
        applyRegex(text, STRING_SINGLE_PATTERN, ColorString)
        applyRegex(text, STRING_BACKTICK_PATTERN, ColorString)
        // Comments
        applyRegex(text, LINE_COMMENT_PATTERN, ColorComment)
        applyRegex(text, BLOCK_COMMENT_PATTERN, ColorComment)
    }

    private fun AnnotatedString.Builder.highlightJson(text: String) {
        applyRegex(text, STRING_DOUBLE_PATTERN, ColorAttr)
        applyRegex(text, NUMBER_PATTERN, ColorNumber)
        applyRegex(text, Pattern.compile("\\b(true|false|null)\\b"), ColorKeyword)
    }

    private fun AnnotatedString.Builder.highlightGeneric(text: String) {
        applyRegex(text, STRING_DOUBLE_PATTERN, ColorString)
        applyRegex(text, NUMBER_PATTERN, ColorNumber)
        applyRegex(text, LINE_COMMENT_PATTERN, ColorComment)
    }

    private fun AnnotatedString.Builder.applyRegex(
        text: String,
        pattern: Pattern,
        color: Color,
        fontWeight: FontWeight = FontWeight.Normal
    ) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            if (start in 0..text.length && end in 0..text.length && start < end) {
                addStyle(
                    style = SpanStyle(color = color, fontWeight = fontWeight),
                    start = start,
                    end = end
                )
            }
        }
    }

    fun formatCode(code: String, extension: String): String {
        return try {
            when (extension.lowercase()) {
                "html", "htm" -> formatHtml(code)
                "css", "js", "json" -> formatBraces(code)
                else -> code
            }
        } catch (e: Exception) {
            code
        }
    }

    private fun formatHtml(html: String): String {
        val lines = html.lines()
        val result = StringBuilder()
        var indent = 0
        val voidTags = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                result.append("\n")
                continue
            }

            if (line.startsWith("</")) {
                indent = maxOf(0, indent - 1)
            }

            result.append("  ".repeat(indent)).append(line).append("\n")

            // Check if line opens a non-void tag without closing it on the same line
            val isClosing = line.startsWith("</")
            val isComment = line.startsWith("<!--") || line.startsWith("<!DOCTYPE")
            val isSelfClosing = line.endsWith("/>") || line.contains("</")

            if (!isClosing && !isComment && !isSelfClosing && line.startsWith("<")) {
                val tagMatch = Regex("""^<([a-zA-Z0-9]+)""").find(line)
                val tagName = tagMatch?.groupValues?.get(1)?.lowercase()
                if (tagName != null && tagName !in voidTags) {
                    indent++
                }
            }
        }
        return result.toString().trimEnd()
    }

    private fun formatBraces(code: String): String {
        val lines = code.lines()
        val result = StringBuilder()
        var indent = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                result.append("\n")
                continue
            }

            if (line.startsWith("}") || line.startsWith("]")) {
                indent = maxOf(0, indent - 1)
            }

            result.append("  ".repeat(indent)).append(line).append("\n")

            val openCount = line.count { it == '{' || it == '[' }
            val closeCount = line.count { it == '}' || it == ']' }
            val netOpen = openCount - closeCount
            if (netOpen > 0) {
                indent += netOpen
            }
        }
        return result.toString().trimEnd()
    }
}
