package com.example.loom.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class CodeSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val code = text.text

        // Dictionaries for Java, C, Python, and C# (.NET)
        val keywords = listOf(
            "public", "private", "protected", "class", "interface", "fun", "def", "var", "val",
            "int", "float", "double", "string", "void", "static", "final", "const",
            "if", "else", "for", "while", "return", "import", "using", "namespace", "struct"
        )

        val keywordColor = Color(0xFFC678DD) // Purple
        val stringColor = Color(0xFF98C379)  // Green
        val numberColor = Color(0xFFD19A66)  // Orange
        val commentColor = Color(0xFF5C6370) // Grey

        val annotatedString = buildAnnotatedString {
            append(code)

            // 1. Highlight Keywords
            val keywordPattern = "\\b(${keywords.joinToString("|")})\\b".toRegex()
            keywordPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = keywordColor), match.range.first, match.range.last + 1)
            }

            // 2. Highlight Numbers
            val numberPattern = "\\b\\d+\\b".toRegex()
            numberPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }

            // 3. Highlight Strings (Single and Double quotes)
            val stringPattern = "(\"[^\"]*\"|'[^']*')".toRegex()
            stringPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }

            // 4. Highlight Comments (// and #)
            val commentPattern = "(//.*|#.*)".toRegex()
            commentPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            }
        }

        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}