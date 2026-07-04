package com.kinetic.keyboard.input

/**
 * Roman→Bengali rule tables, ported 1:1 from the recovered reference
 * (reference/BanglaUnicode.java). Do NOT "fix" apparent oddities here — e.g. later `put`s
 * overwriting earlier ones (dh→"" wins over dh→"wn") are the shipped behavior the golden corpus
 * locks in. Tunable-as-data extraction happens in P3.6.
 */
class BanglaUnicodeTable {

    /** Roman token → base letter/cluster (map). */
    private val map = hashMapOf(
        "o" to "অ", "O" to "ও", "a" to "আ", "A" to "আ",
        "S" to "শ", "sh" to "শ", "s" to "স", "Sh" to "ষ",
        "h" to "হ", "H" to "হ",
        "r" to "র", "R" to "ড়", "Rh" to "ঢ়",
        "k" to "ক", "K" to "ক", "q" to "ক", "qq" to "ঁ", "kh" to "খ",
        "g" to "গ", "G" to "গ", "gh" to "ঘ", "Ng" to "ঙ",
        "c" to "চ", "C" to "চ", "ch" to "ছ",
        "j" to "জ", "jh" to "ঝ", "J" to "জ", "NG" to "ঞ",
        "T" to "ট", "Th" to "ঠ", "TH" to "ৎ",
        "f" to "ফ", "F" to "ফ", "ph" to "ফ",
        "i" to "ই", "I" to "ঈ", "e" to "এ", "E" to "এ",
        "u" to "উ", "U" to "ঊ",
        "b" to "ব", "B" to "ব", "w" to "ব", "bh" to "ভ", "V" to "ভ", "v" to "ভ",
        "t" to "ত", "th" to "থ", "d" to "দ", "dh" to "ধ",
        "D" to "ড", "Dh" to "ঢ",
        "n" to "ন", "N" to "ণ",
        "z" to "য", "Z" to "য", "y" to "য়",
        "l" to "ল", "L" to "ল", "m" to "ম", "M" to "ম",
        "P" to "প", "p" to "প",
        "ng" to "ং", "cb" to "ঁ",
        "x" to "ক্স",
        "OU" to "ঔ", "OI" to "ঐ",
        "hs" to "্",
        "nj" to "ঞ্জ", "nc" to "ঞ্চ", "gg" to "জ্ঞ",
    )

    /** Roman vowel → dependent sign (kars). */
    private val kars = hashMapOf(
        "o" to "", "a" to "া", "A" to "া",
        "e" to "ে", "E" to "ে",
        "O" to "ো", "OI" to "ৈ", "OU" to "ৌ",
        "i" to "ি", "I" to "ী",
        "u" to "ু", "U" to "ূ",
        "oo" to "ু",
    )

    /** consonant → the roman consonants that may join under it (jkt). Later puts win, as in the original. */
    private val jkt = HashMap<String, String>().apply {
        put("k", "kTtnNslw"); put("g", "gnNmlw"); put("ch", "w"); put("Ng", "gkm"); put("NG", "cj")
        put("g", "gnNmlw"); put("G", "gnNmlw"); put("th", "w"); put("gh", "Nn"); put("c", "c")
        put("j", "jw"); put("T", "T"); put("D", "D"); put("R", "g"); put("N", "DNmwT")
        put("t", "tnmwN"); put("d", "wdmv"); put("dh", "wn"); put("n", "ndwmtsDT"); put("p", "plTtns")
        put("f", "l"); put("ph", "l"); put("b", "jdbwl"); put("v", "l"); put("bh", "l")
        put("m", "npfwvmlb"); put("l", "lwmpkgTDf"); put("Sh", "kTNpmf"); put("S", "clwnm")
        put("sh", "clwnm"); put("s", "kTtnpfmlw"); put("h", "Nnmlw")
        put("cb", ""); put("jh", ""); put("TH", ""); put("qq", ""); put("ng", ""); put("kh", "")
        put("gg", ""); put("dh", ""); put("Th", "")
    }

    /** two-char consonant → single consonants it sits under (djkt). */
    private val djkt = hashMapOf(
        "kh" to "Ngs", "ch" to "c", "Dh" to "N", "ph" to "mls", "dh" to "gdnbl",
        "bh" to "dm", "Sh" to "k", "th" to "tns", "Th" to "Nn", "jh" to "j", "NG" to "cj",
    )

    /** two-char consonant → two-char consonants it sits under (djktt). */
    private val djktt = hashMapOf(
        "ch" to "NG", "gh" to "Ng", "Th" to "Sh", "jh" to "NG", "sh" to "ch",
    )

    fun get(cp: Int): String? = map[cp.toChar().toString()]
    fun getKar(cp: Int): String? = kars[cp.toChar().toString()]
    fun getJkt(cp: Int): String? = jkt[cp.toChar().toString()]
    /** map lookup for the two-char token prev+current (note reversed args, as in the original). */
    fun getDual(cur: Int, prev: Int): String? = map["${prev.toChar()}${cur.toChar()}"]
    fun getDualKar(cur: Int, prev: Int): String? = kars["${prev.toChar()}${cur.toChar()}"]
    fun getDualJkt(a: Int, b: Int): String? = jkt["${a.toChar()}${b.toChar()}"]
    fun getDjkt(a: Int, b: Int): String? = djkt["${a.toChar()}${b.toChar()}"]
    fun getDjktt(a: Int, b: Int): String? = djktt["${a.toChar()}${b.toChar()}"]
}
