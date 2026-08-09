package com.example.loom.ui.notes

import kotlinx.serialization.Serializable

const val ZWSP = "\u200B"

@Serializable
data class NoteBlock(
    val id: String,
    var type: String,
    var content: String = "",
    var isChecked: Boolean = false,
    var width: Float = 250f,
    var height: Float = 250f,
    var isCropped: Boolean = true,
    var alignment: String = "Center" // NEW: Remembers horizontal placement
)