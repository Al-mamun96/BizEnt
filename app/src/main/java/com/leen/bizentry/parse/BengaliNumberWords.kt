package com.leen.bizentry.parse

/**
 * Bengali and common Banglish number words, rewritten as digits before the amount scan runs, so
 * everything downstream sees `5 হাজার` instead of `পাঁচ হাজার` and needs no special case for words.
 *
 * Two details make this correct rather than merely plausible:
 *
 * - words are tried longest first, so `পাঁচশ` (500) is never read as `পাঁচ` (5) followed by `শ`;
 * - each word must stand alone in its own script, so `এক` does not match inside `একা`.
 *
 * Eleven to ninety-nine in words are deliberately not covered.
 *
 * The input is canonicalised here as well, so a caller may pass text straight from the keyboard
 * rather than having to normalise it first.
 */
object BengaliNumberWords {

    private val WORDS: Map<String, String> = mapOf(
        "দেড়" to "1.5",
        "আড়াই" to "2.5",
        "একশ" to "100",
        "দুইশ" to "200",
        "তিনশ" to "300",
        "চারশ" to "400",
        "পাঁচশ" to "500",
        "ছয়শ" to "600",
        "সাতশ" to "700",
        "আটশ" to "800",
        "নয়শ" to "900",
        "এক" to "1",
        "দুই" to "2",
        "তিন" to "3",
        "চার" to "4",
        "পাঁচ" to "5",
        "ছয়" to "6",
        "সাত" to "7",
        "আট" to "8",
        "নয়" to "9",
        "দশ" to "10",
        "char" to "4",
        "panch" to "5",
        "chhoy" to "6",
        "saat" to "7",
        "dosh" to "10",
        "dash" to "10",
        "pach" to "5",
        "choy" to "6",
        "dui" to "2",
        "tin" to "3",
        "aat" to "8",
        "noy" to "9",
        "ek" to "1"
    )

    private val DIGITS_BY_WORD: Map<String, String> = WORDS.mapKeys { (word, _) ->
        BengaliText.canonicalize(word)
    }

    private val BENGALI_PATTERN: Regex
    private val ASCII_PATTERN: Regex

    init {
        val bengali = WORDS.keys
            .filter { word -> word.any { it.code in 0x0980..0x09FF } }
            .sortedByDescending { it.length }
            .map { BengaliText.canonicalize(it) }

        val ascii = WORDS.keys
            .filterNot { word -> word.any { it.code in 0x0980..0x09FF } }
            .sortedByDescending { it.length }

        BENGALI_PATTERN = Regex(
            "(?<![\\u0980-\\u09FF])(?:" + bengali.joinToString("|") { Regex.escape(it) } +
                ")(?![\\u0980-\\u09FF])"
        )

        ASCII_PATTERN = Regex(
            "(?<![a-zA-Z])(?:" + ascii.joinToString("|") { Regex.escape(it) } + ")(?![a-zA-Z])",
            RegexOption.IGNORE_CASE
        )
    }

    fun replaceWithDigits(text: String): String {
        val canonical = BengaliText.canonicalize(text)

        val withBengali = BENGALI_PATTERN.replace(canonical) { DIGITS_BY_WORD.getValue(it.value) }

        return ASCII_PATTERN.replace(withBengali) {
            DIGITS_BY_WORD.getValue(it.value.lowercase())
        }
    }
}
