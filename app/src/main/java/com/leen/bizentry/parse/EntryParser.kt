package com.leen.bizentry.parse

import com.leen.bizentry.data.Money
import com.leen.bizentry.data.TransactionType
import java.time.LocalDate

data class ParsedEntry(
    val rawText: String,
    val amountMinor: Long?,
    val type: TransactionType,
    val transactionDate: LocalDate?,
    val category: String?
)

private data class AmountCandidate(val minorUnits: Long, val score: Int)

private const val CURRENCY_SCORE = 2
private const val SCALE_SCORE = 1
private const val TAIL_LENGTH = 24

/**
 * Deterministic, offline reading of a Bengali or Banglish ledger sentence.
 *
 * It extracts rather than invents: when it cannot decide it returns null, and the caller is
 * expected to ask. That is why a sentence holding two unmarked numbers yields no amount at all
 * instead of a guess.
 *
 * Only the word immediately following a number is examined for a scale or a currency word, so the
 * reading does not depend on how far ahead the scan happens to look.
 */
object EntryParser {

    val knownCategories: List<String> = EntryKeywords.categories.map { (category, _) -> category }

    private val NUMBER = Regex("""\d[\d,]*(?:\.\d+)?""")
    private val DATE_SHAPED = Regex("""\d{1,4}[./-]\d{1,2}[./-]\d{2,4}""")

    private val CURRENCY_MARKERS = listOf(
        "\u09F3", "টাকা", "টাকার", "taka", "takar", "tk", "poisha"
    )

    private val SCALES: List<Pair<List<String>, Long>> = listOf(
        listOf("কোটি", "koti", "crore", "cr") to 10_000_000L,
        listOf("লক্ষ", "লাখ", "lakh", "lac") to 100_000L,
        listOf("হাজার", "হাযার", "হাজর", "hazar", "hajar", "thousand") to 1_000L,
        listOf("শত", "শো", "hundred", "sho") to 100L
    ).map { (words, factor) ->
        words.map { BengaliText.canonicalize(it) } to factor
    }

    private val typeGroups = EntryKeywords.typeGroups.map { (type, words) ->
        type to words.map(::matcher)
    }

    private val categoryGroups = EntryKeywords.categories.map { (category, words) ->
        category to words.map(::matcher)
    }

    private val todayMatchers = EntryKeywords.today.map(::matcher)
    private val yesterdayMatchers = EntryKeywords.yesterday.map(::matcher)
    private val tomorrowMatchers = EntryKeywords.tomorrow.map(::matcher)

    fun parse(text: String, today: LocalDate): ParsedEntry {
        val canonical = BengaliText.canonicalize(text)

        return ParsedEntry(
            rawText = text,
            amountMinor = extractAmountMinor(
                BengaliText.toAsciiDigits(BengaliNumberWords.replaceWithDigits(canonical))
            ),
            type = extractType(canonical),
            transactionDate = extractDate(canonical, today),
            category = extractCategory(canonical)
        )
    }

    private fun matcher(keyword: String): Regex {
        val escaped = Regex.escape(BengaliText.canonicalize(keyword))
        return if (keyword.all { it.code < 128 }) {
            Regex("(?<![a-zA-Z])$escaped(?![a-zA-Z])", RegexOption.IGNORE_CASE)
        } else {
            Regex(escaped)
        }
    }

    private fun extractAmountMinor(text: String): Long? {
        val dateSpans = DATE_SHAPED.findAll(text).map { it.range }.toList()

        val candidates = NUMBER.findAll(text)
            .filterNot { number -> dateSpans.any { number.range.first in it } }
            .mapNotNull { number ->
                val base = Money.parseMinorUnits(number.value.replace(",", ""))
                    ?: return@mapNotNull null

                val followingWord = followingWord(text, number.range.last + 1)
                val factor = scaleFactor(followingWord)

                val minorUnits = try {
                    Math.multiplyExact(base, factor)
                } catch (e: ArithmeticException) {
                    return@mapNotNull null
                }

                var score = 0
                if (factor > 1L) score += SCALE_SCORE
                if (CURRENCY_MARKERS.any { followingWord.contains(it) }) score += CURRENCY_SCORE

                AmountCandidate(minorUnits, score)
            }
            .toList()

        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.single().minorUnits

        val best = candidates.maxOf { it.score }
        val winners = candidates.filter { it.score == best }
        return if (winners.size == 1) winners.single().minorUnits else null
    }

    private fun followingWord(text: String, fromIndex: Int): String {
        val tail = text.substring(fromIndex, minOf(text.length, fromIndex + TAIL_LENGTH))
        return tail.trimStart().takeWhile { !it.isWhitespace() }
    }

    private fun scaleFactor(word: String): Long {
        if (word.startsWith("k") && (word.length == 1 || !word[1].isLetter())) {
            return 1_000L
        }

        SCALES.forEach { (words, factor) ->
            if (words.any { word.startsWith(it, ignoreCase = true) }) {
                return factor
            }
        }

        return 1L
    }

    private fun extractType(text: String): TransactionType =
        typeGroups.firstOrNull { (_, matchers) -> matchers.any { it.containsMatchIn(text) } }
            ?.first
            ?: TransactionType.UNKNOWN

    private fun extractCategory(text: String): String? =
        categoryGroups.firstOrNull { (_, matchers) -> matchers.any { it.containsMatchIn(text) } }
            ?.first

    private fun extractDate(text: String, today: LocalDate): LocalDate? = when {
        yesterdayMatchers.any { it.containsMatchIn(text) } -> today.minusDays(1)
        tomorrowMatchers.any { it.containsMatchIn(text) } -> today.plusDays(1)
        todayMatchers.any { it.containsMatchIn(text) } -> today
        else -> null
    }
}
