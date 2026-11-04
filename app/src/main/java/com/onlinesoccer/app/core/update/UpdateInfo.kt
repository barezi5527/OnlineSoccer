package com.onlinesoccer.app.core.update

/** Eine stabile, auf GitHub veröffentlichte App-Version. */
data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
)
