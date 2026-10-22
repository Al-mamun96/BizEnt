package com.leen.bizentry.data

/**
 * Whether the money for an entry moved at the time it was recorded.
 *
 * Kept separate from [TransactionType] on purpose: a `SALE` can be settled either way, and without
 * this distinction a report cannot tell "sold but not yet paid" from "cash has arrived", so it can
 * answer neither "how much am I owed" nor "how much cash came in".
 */
enum class Settlement {
    CASH,
    CREDIT
}
