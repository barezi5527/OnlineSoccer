package com.onlinesoccer.app.data.model

/** Eine private Nachricht aus `/osneu/pm`. */
data class PmNachricht(
    val id: Long,
    val sender: String?,
    val empfänger: String?,
    val betreff: String?,
    val datum: String?,
    val gelesen: Boolean = false,
)

data class PmDetail(
    val nachricht: PmNachricht?,
    val body: String?,
)

data class PmAntwortFormular(
    val empfaenger: String,
    val empfaengerId: String,
    val betreff: String,
    val text: String,
    val transferId: String = "0",
)
