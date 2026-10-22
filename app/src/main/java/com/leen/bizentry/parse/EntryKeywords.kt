package com.leen.bizentry.parse

import com.leen.bizentry.data.TransactionType

/**
 * The keyword tables behind the offline parser. These are plain data and are the part most likely
 * to need tuning, so they are kept together and out of the matching logic.
 *
 * Ordering matters for the type groups: the first group that matches wins, and `বিক্রয়` (sale)
 * contains `ক্রয়` (purchase) as a substring, so sale has to be tested first or every sale would be
 * read as a purchase.
 *
 * Category names are stored on the entry, so this list is worth settling before real data builds
 * up. Renaming a category later means updating rows that already carry the old name.
 */
internal object EntryKeywords {

    val sale = listOf(
        "বেচেছি", "বেচলাম", "বেচা", "বিক্রি", "বিক্রয়",
        "bechechi", "bechlam", "bikri", "sold", "sale"
    )

    val purchase = listOf(
        "কিনেছি", "কিনলাম", "কেনা", "ক্রয়",
        "kinechi", "kinlam", "kine", "bought", "purchase"
    )

    val expense = listOf(
        "খরচ", "খরচা", "ব্যয়", "বিল",
        "khoroch", "kharch", "khorooch", "expense", "bill"
    )

    val income = listOf(
        "আয়", "মুনাফা", "লাভ", "aay", "profit", "income"
    )

    val paymentIn = listOf(
        "পেয়েছি", "পেলাম", "জমা", "আদায়",
        "peyechi", "pelam", "joma", "adai", "received"
    )

    val paymentOut = listOf(
        "দিয়েছি", "দিলাম", "পরিশোধ", "শোধ",
        "diyechi", "dilam", "porishodh", "shodh", "paid"
    )

    val cash = listOf("নগদ", "নগদে", "নগদি", "ক্যাশ", "nagad", "nakad", "cash")

    val credit = listOf(
        "ধার", "ধারে", "বাকি", "বাকিতে", "বাকী", "বাকীতে",
        "dhar", "dhare", "baki", "bakite", "udhar", "credit", "due"
    )

    val today = listOf("আজ", "আজকে", "aj", "aaj", "ajke")

    val yesterday = listOf("গতকাল", "গতকালকে", "gotokal", "gotokalke", "gato kal")

    val tomorrow = listOf("আগামীকাল", "আগামী কাল", "agamikal")

    val categories: List<Pair<String, List<String>>> = listOf(
        "কাপড়" to listOf("কাপড়", "কাপড়চোপড়", "cloth", "kapod", "kapodchoporo"),
        "খাবার" to listOf(
            "খাবার", "চাল", "ডাল", "তেল", "মাছ", "মাংস", "সবজি", "মুদি",
            "food", "chal", "dal", "tel", "mach", "mangsho", "sobji"
        ),
        "যাতায়াত" to listOf(
            "যাতায়াত", "রিকশা", "সিএনজি", "বাস", "ট্রেন", "গাড়ি ভাড়া",
            "transport", "riksha", "cng", "bus", "train"
        ),
        "বেতন" to listOf("বেতন", "salary", "beton", "মজুরি", "majuri"),
        "বিদ্যুৎ" to listOf("বিদ্যুৎ", "কারেন্ট", "electricity", "bidyut", "current"),
        "ওষুধ" to listOf("ওষুধ", "ঔষধ", "medicine", "osudh"),
        "কাঁচামাল" to listOf("কাঁচামাল", "কাচামাল", "raw material", "kachamal"),
        "দোকান ভাড়া" to listOf("দোকান ভাড়া", "shop rent", "dokan bhara")
    )

    val typeGroups: List<Pair<TransactionType, List<String>>> = listOf(
        TransactionType.SALE to sale,
        TransactionType.PURCHASE to purchase,
        TransactionType.EXPENSE to expense,
        TransactionType.INCOME to income,
        TransactionType.PAYMENT_IN to paymentIn,
        TransactionType.PAYMENT_OUT to paymentOut
    )
}
