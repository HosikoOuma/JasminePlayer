package com.example.jasmineplayer

import android.net.Uri

data class Folder(
    val name: String,
    val path: String,
    val songCount: Int,
    val albumArtUri: Uri? = null
)
