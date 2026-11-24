package com.uade.tmt.data.mapper

import java.text.Normalizer

private val TILDES = Regex("\\p{Mn}+")

/** Texto comparable: sin tildes, en minúsculas y sin espacios de más ("  Peña  Ángel" → "pena angel"). */
fun String.normalizado(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(TILDES, "")
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")
