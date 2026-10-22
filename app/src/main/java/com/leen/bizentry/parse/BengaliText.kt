package com.leen.bizentry.parse

import java.text.Normalizer

/**
 * Bengali text helpers for the parser.
 *
 * [canonicalize] exists because Bengali composes two ways: `য়` is either the single code point
 * U+09DF or `য` U+09AF followed by the nukta U+09BC, and keyboards disagree about which they emit.
 * NFC does *not* unify them, because those letters are on Unicode's composition-exclusion list.
 * Decomposing to NFD does, so both the input and the keyword tables are canonicalised before they
 * are compared. Without this, a keyword would silently fail to match depending on the phone.
 *
 * The digit table uses escapes rather than literals so the mapping cannot be altered by anything
 * that rewrites this source file.
 */
object BengaliText {

    private val DIGITS = mapOf(
        '\u09E6' to '0',
        '\u09E7' to '1',
        '\u09E8' to '2',
        '\u09E9' to '3',
        '\u09EA' to '4',
        '\u09EB' to '5',
        '\u09EC' to '6',
        '\u09ED' to '7',
        '\u09EE' to '8',
        '\u09EF' to '9'
    )

    fun canonicalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)

    fun toAsciiDigits(text: String): String = buildString(text.length) {
        text.forEach { character -> append(DIGITS[character] ?: character) }
    }
}
