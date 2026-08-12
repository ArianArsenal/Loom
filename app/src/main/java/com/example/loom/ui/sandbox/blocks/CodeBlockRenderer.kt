package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.loom.util.CodeSyntaxTransformation

@Composable
fun CodeBlockRenderer(textState: TextFieldValue, handleTextChange: (TextFieldValue) -> Unit, commonModifier: Modifier, isReadOnly: Boolean, onDeleteBlock: (String, Boolean) -> Unit) {
    Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(value = textState, onValueChange = handleTextChange, visualTransformation = CodeSyntaxTransformation(), textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace), modifier = commonModifier.padding(16.dp).padding(end = 24.dp), cursorBrush = SolidColor(Color.White), readOnly = isReadOnly, decorationBox = { inner -> Box{ if (textState.text.isEmpty()) Text("// write code here", color = Color.Gray, fontFamily = FontFamily.Monospace); inner() } })
            if (!isReadOnly) { IconButton(onClick = { onDeleteBlock("", true) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)) { Icon(Icons.Default.Close, "Delete Code", tint = Color.Gray, modifier = Modifier.size(16.dp)) } }
        }
    }
}