package com.worldcup.androidstudiolite.entities

data class SearchMatch(
    val path: String,
    val relativePath: String,
    val fileName: String,
    val lineNumber: Int,
    val lineText: String,
    val columnStart: Int,
)
