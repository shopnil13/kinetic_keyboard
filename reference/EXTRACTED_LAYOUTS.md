# Extracted Ridmik Layouts — Ground Truth

Source: `D:/PROJECTS/RIDMIK/Ridmik_Keyboard.apk` (`net.hasnath.android.keyboard`), decoded with androguard from the compiled `res/xml/*.xml`. These **supersede the photo transcription** in `../SPEC.md` §2 — every character below is authoritative. Raw decoded XML is in `decoded_xml/`.

Notation: `label` = glyph on tap. `popup` = long-press popup characters (order preserved). The original packs the shift-consonant **and** the Bengali digit into one popup string (e.g. `ড` → popup `ঢ৩`).

## Special key codes (custom, negative = conjunct formers)
| code | key | inserts | notes |
|------|-----|---------|-------|
| −565 | `্র` r-phola | ZWJ+্+র | unijoy normal, row3 slot1 |
| −588 | `র্` reph | র+্+ZWJ | unijoy shift, row2 slot1 |
| −564 | `্য` ya-phola | ZWJ+্+য | unijoy shift, row3 slot2 |
| `label_symbol_key` | mode key | → symbols | label text = `?123` |
| `label_alpha_key` | mode key | → letters | label text = `ABC` |
| `label_alt_key` | alt | → symbols page 2 | label text = `ALT` |

The many repeated bottom rows in the raw XML are the same space/enter/`।`/comma row re-declared per `keyboardMode` (normal, email, URL, IM, etc.) — collapse to one logical bottom row + action-key variants when re-encoding.

---

## Layer A — `kbd_unijoy` (Bangla normal)

**Row 1** (label / popup)
| ঙ `১` | য `২` | ড `ঢ৩` | প `ফ৪` | ট `ঠ৫` | চ `ছ৬` | জ `ঝ৭` | হ `ঞ৮` | গ `ঘ৯` | ড় `ঢ়০` |

**Row 2** (offset 5%)
| ৃ `ঋ` | ু `উ` | ি `ই` | া `আ` | ্ (no popup) | ব `ভ` | ক `খ` | ত `ৎ` | দ `ধ` |

**Row 3**
| ⇧ | `্র`(−565) | ো `ও` | ে `এ` | র `ঢ়ল` | ন `ণ` | স `ষশ` | ম `শঃ` | ⌫ |

**Bottom** `?123` · `,` · space · `।`(popup) · enter

---

## Layer B — `kbd_unijoy_shift` (Bangla shift)

**Row 1**
| ং `১` | য় `২` | ঢ `৩` | ফ `৪` | ঠ `৫` | ছ `৬` | ঝ `৭` | ঞ `৮` | ঘ `৯` | ঢ় `০` |

**Row 2** (offset 5%)
| `র্`(−588) | ূ `ঊ` | ী `ঈ` | অ | ঁ | ভ | খ | থ | ধ |

**Row 3**
| ⇧(sticky) | `্য`(−564) | ৌ `ঔ` | ৈ `ঐ` | ল | ণ | ষ | শ | ⌫ |

**Bottom** same as Layer A.

> Confirmed corrections vs. photo guesses: row2-slot2 = **ু/ূ** (u-kar) not ৃ; normal row3-slot1 = **্র r-phola** not ঁ; **ঁ** chandrabindu lives on shift row2-slot4; reph/ya-phola are dedicated keys.

---

## Layer C — `kbd_symbols` (page 1)

**Row 1** (digit / popup)
| 1 `¹½⅓¼⅛১` | 2 `²⅔২` | 3 `³¾⅜৩` | 4 `⁴৪` | 5 `⅝৫` | 6 `৬` | 7 `⅞৭` | 8 `৮` | 9 `৯` | 0 `ⁿ∅০` |

**Row 2**
| @ | # | $ `¢£৳€¥₣₤₱` | % `‰` | & | * `†‡★` | - `_–—` | + `±` | ( `[{<` | ) `]}>` |

**Row 3**
| ALT(sticky) | ! `¡` | " `“”«»˝` | ' `‘’` | : `ঃ` | ; | / | ? `¿` | ⌫ |

**Bottom** `ABC` · `,` · space · `.`(popup) · enter. `ALT` → `kbd_symbols_shift` (page 2).

---

## Layer D — `kbd_qwerty` (English)

**Row 1** `q w e r t y u i o p` — long-press digits `1 2 3 4 5 6 7 8 9 0`
**Row 2** (offset 5%) `a s d f g h j k l` — long-press `@ # & * - + = ( )`
**Row 3** `⇧ z x c v b n m ⌫` — long-press `_ " (c:none) ' : ; /`
**Bottom** `?123` · `,` · space · `.`(popup) · enter

(Long-press alternates come from `alternates_for_*` string resources — standard AOSP LatinIME set.)
