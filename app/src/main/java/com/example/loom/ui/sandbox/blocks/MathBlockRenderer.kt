package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.SandboxBlock
import com.example.loom.ui.sandbox.MathVisualTransformation


@Composable
fun MathBlockRenderer(
    currentBlock: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier, // --- FIX: Renamed modifier ---
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    isReadOnly: Boolean,
    onUpdate: (SandboxBlock) -> Unit
) {
    var showMathSymbols by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Text("∑", fontSize = 24.sp, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { if (!isReadOnly) showMathSymbols = true })
                DropdownMenu(expanded = showMathSymbols, onDismissRequest = { showMathSymbols = false }, modifier = Modifier.width(280.dp).background(MaterialTheme.colorScheme.surface)) {
                    val symbols = listOf("∫", "∬", "√", "∞", "≈", "≠", "≤", "≥", "±", "×", "÷", "π", "θ", "α", "β", "Δ", "λ", "μ", "σ", "°")
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Math Symbols", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                        symbols.chunked(5).forEach { rowSymbols ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                rowSymbols.forEach { sym -> Text(sym, fontSize = 20.sp, modifier = Modifier.padding(8.dp).clickable { val start = textState.selection.start.coerceIn(0, textState.text.length); val newContent = textState.text.substring(0, start) + sym + textState.text.substring(start); onUpdate(currentBlock.copy(content = newContent)); showMathSymbols = false }) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            BasicTextField(
                value = textState,
                onValueChange = handleTextChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 20.sp),
                modifier = modifier.weight(1f), // --- FIX: Applied renamed modifier ---
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = MathVisualTransformation(),
                decorationBox = placeholderUI,
                readOnly = isReadOnly
            )
        }
    }
}