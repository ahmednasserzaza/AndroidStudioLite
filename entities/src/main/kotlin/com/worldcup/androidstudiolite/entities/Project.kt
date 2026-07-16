package com.worldcup.androidstudiolite.entities

data class Project(
    val id: String,
    val name: String,
    val packageName: String,
    val repoName: String,
    val path: String,
    val lastModifiedEpochMs: Long,
)

data class FileNode(
    val path: String,
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val depth: Int,
)
