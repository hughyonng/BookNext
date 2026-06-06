package com.booknext.app.util

import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object EncodingDetector {

    fun readLines(file: File): List<String> {
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            file.inputStream().use { InputStreamReader(it, decoder).readLines() }
        } catch (_: Exception) {
            file.bufferedReader(Charset.forName("GBK")).use { it.readLines() }
        }
    }

    fun readText(file: File): String {
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            file.inputStream().use { InputStreamReader(it, decoder).readText() }
        } catch (_: Exception) {
            file.readText(Charset.forName("GBK"))
        }
    }
}
