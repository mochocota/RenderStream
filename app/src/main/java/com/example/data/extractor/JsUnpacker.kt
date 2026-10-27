package com.example.data.extractor

import java.math.BigInteger

/**
 * Desempaquetador de scripts Javascript ofuscados con el patrón Dean Edwards:
 * eval(function(p,a,c,k,e,d)...)
 *
 * Utilizado por servidores como Filemoon, StreamWish, Doodstream, etc.
 */
object JsUnpacker {
    private val PACKED_REGEX = Regex(
        """\}\s*\(\s*'(.*?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'(.*?)'\.split\('\|'\)""",
        RegexOption.DOT_MATCHES_ALL
    )

    fun isPacked(script: String): Boolean {
        return script.contains("eval(function(p,a,c,k,e,d)") || script.contains("eval(function(p,a,c,k,e,r)")
    }

    fun unpack(packedJs: String): String {
        val match = PACKED_REGEX.find(packedJs) ?: return packedJs
        val payload = match.groupValues[1]
        val radixStr = match.groupValues[2]
        val countStr = match.groupValues[3]
        val symtab = match.groupValues[4].split("|")

        val radix = radixStr.toIntOrNull() ?: 36
        val count = countStr.toIntOrNull() ?: symtab.size

        val tokenRegex = Regex("""\b\w+\b""")
        return tokenRegex.replace(payload) { tokenMatch ->
            val word = tokenMatch.value
            val index = try {
                BigInteger(word, radix).toInt()
            } catch (e: Exception) {
                -1
            }

            if (index in 0 until count && index < symtab.size) {
                val replacement = symtab[index]
                if (replacement.isNotEmpty()) replacement else word
            } else {
                word
            }
        }
    }
}
