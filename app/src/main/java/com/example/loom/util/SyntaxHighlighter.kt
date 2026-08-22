package com.example.loom.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// --- OPTIMIZATION: Moved heavy Regex compilation OUT of the render loop! ---
private val keywords = listOf(
    "public", "private", "protected", "class", "interface", "fun", "def", "var", "val",
    "int", "float", "double", "string", "void", "static", "final", "const",
    "if", "else", "for", "while", "return", "import", "using", "namespace", "struct"
)
private val keywordPattern = "\\b(${keywords.joinToString("|")})\\b".toRegex()
private val numberPattern = "\\b\\d+\\b".toRegex()
private val stringPattern = "(\"[^\"]*\"|'[^']*')".toRegex()
private val commentPattern = "(//.*|#.*)".toRegex()

private val keywordColor = Color(0xFFC678DD) // Purple
private val stringColor = Color(0xFF98C379)  // Green
private val numberColor = Color(0xFFD19A66)  // Orange
private val commentColor = Color(0xFF5C6370) // Grey

class CodeSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val code = text.text

        val annotatedString = buildAnnotatedString {
            append(code)

            // 1. Highlight Keywords
            keywordPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = keywordColor), match.range.first, match.range.last + 1)
            }

            // 2. Highlight Numbers
            numberPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }

            // 3. Highlight Strings
            stringPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }

            // 4. Highlight Comments
            commentPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            }
        }

        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}