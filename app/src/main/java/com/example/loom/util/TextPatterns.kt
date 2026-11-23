package com.example.loom.util

/** Precompiled patterns: these used to be recompiled inside composables and per-record loops. */
object TextPatterns {
    val NUMBER = Regex("\\d+(\\.\\d+)?")
    val WHITESPACE = Regex("\\s")
    val PERSIAN = Regex("[\\u0600-\\u06FF]")
}
