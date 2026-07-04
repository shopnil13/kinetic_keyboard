# Bangla / English / Banglish Keyboard — Technical Specification

**Project name:** Kinetic Keyboard · **Package / applicationId:** `com.kinetic.keyboard`
**Target:** Android IME (system keyboard) replicating the Avro "ইউনিজয়" (UniJoy) fixed layout from the reference photos, plus phonetic Banglish and English, aiming to match or beat Gboard for Bengali users.
**Stack decision (locked):** Kotlin + Android native `InputMethodService`, **Jetpack Compose** for the keyboard UI. **minSdk 28 (Android 9)**, targetSdk/compileSdk 35, JDK 17.
**Doc status:** v2 — spec + trackable implementation blueprint. Exact layouts recovered from the original APK (see `reference/`). §10 is the live build tracker — update checkboxes as work lands.

---

## 1. Goals & scope

### 1.1 Must-have (defines "done")
1. Reproduce the **exact 4-layer layout** from the reference photos:
   - Bangla normal, Bangla shift, Symbols (`?123`), English QWERTY.
2. Three input modes:
   - **Bangla fixed** (UniJoy): key → specific Bengali Unicode char.
   - **Banglish phonetic**: Roman input → Bengali (Avro-Phonetic style) with candidate list.
   - **English**: QWERTY with prediction/autocorrect.
3. Correct Bengali Unicode output (conjuncts, vowel-sign ordering, grapheme-aware backspace).
4. Suggestion bar with next-word prediction + autocorrect.
5. Language-switch key cycling the active mode (matches "ইউনিজয় ◄ ►" / "English" spacebar label in photos).

### 1.2 Should-have (to reach Gboard parity)
Emoji panel, GIF/sticker, clipboard manager, themes (light/dark/custom), gesture/glide typing, haptic + sound feedback, voice input, one-handed mode, number-row toggle, settings screen, per-app language memory.

### 1.3 Out of scope (v1)
Cloud sync of user dictionary, federated learning, handwriting input, multi-device clipboard.

---

## 2. Reference layout

> **✅ RESOLVED — ground truth extracted.** The original Ridmik APK was decompiled and its compiled `res/xml/kbd_unijoy*.xml`, `kbd_symbols.xml`, and `kbd_qwerty.xml` decoded (androguard). The authoritative, character-exact layouts — including every long-press popup and the special conjunct-key codes — now live in **[reference/EXTRACTED_LAYOUTS.md](reference/EXTRACTED_LAYOUTS.md)** with raw decoded XML in `reference/decoded_xml/`. **Encode Phase 1 from that file, not from the photo tables below.** All `⚠VERIFY` cells are resolved there (notable fixes: u-kar not ri-kar on row 2; dedicated ্র / র্ / ্য conjunct keys where the photos were ambiguous).

The tables below are the original photo transcription, kept for reference only.

Notation: each key has a **primary** glyph (produced on tap) and often a **long-press** glyph shown as a small superscript (produced by press-and-hold, surfaced in a popup). Grid is a standard 4-row phone keyboard. `⚠VERIFY` = superseded by the extracted layouts above.

A persistent **blue punctuation strip** sits above all letter layers in the photos:
`!  ?  ,  "  '  |  :  ;  (` — implement as a quick-punctuation row (candidate: make it the suggestion strip's alternate/long-press content, or a fixed top row — see §5.4).

### 2.1 Layer A — Bangla, normal (image 1)

Row 1 — primary (long-press = Bengali digit):

| # | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|----|
| primary | ঙ | য | ড | প | ট | চ | জ | হ | গ | ড় |
| long-press | ১ | ২ | ৩ | ৪ | ৫ | ৬ | ৭ | ৮ | ৯ | ০ |

Row 2 — 9 keys (primary / long-press):

| # | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| primary | ৃ | ৃ⚠VERIFY | ি | া | ্ (hasanta) | ব | ক | ত | দ |
| long-press | ঋ | ৄ⚠VERIFY | ⚠VERIFY | আ | ⚠VERIFY | ভ | খ | ৎ | ধ |

Row 3 — shift + 7 keys + backspace (primary / long-press):

| slot | shift | k1 | k2 | k3 | k4 | k5 | k6 | k7 | ⌫ |
|------|-------|----|----|----|----|----|----|----|----|
| primary | ⇧ | ঁ⚠VERIFY | ো | ে | র | ন | স | ম | ⌫ |
| long-press | — | ⚠VERIFY | ও | এ | ল | ণ | শ | ঃ | — |

Bottom row (all letter layers): `?123` · `,` · **spacebar (label "◄ ইউনিজয় ►")** · `।`(danda)⚠VERIFY · emoji/enter.

### 2.2 Layer B — Bangla, shift (image 2)

Row 1 (primary / long-press = Bengali digit):

| # | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|----|
| primary | ং | য় | ঢ | ফ | ঠ | ছ | ঝ | ঞ | ঘ | ঢ় |
| long-press | ১ | ২ | ৩ | ৪ | ৫ | ৬ | ৭ | ৮ | ৯ | ০ |

Row 2 — 9 keys:

| # | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| primary | ⚠VERIFY | ী | অ | ⚠VERIFY | ঃ⚠VERIFY | ভ | খ | থ | ধ |
| long-press | ⚠VERIFY | ঊ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | ⚠VERIFY |

Row 3 — shift + 7 keys + backspace:

| slot | shift | k1 | k2 | k3 | k4 | k5 | k6 | k7 | ⌫ |
|------|-------|----|----|----|----|----|----|----|----|
| primary | ⇧(active) | য | ৌ | ৈ | ল | ণ | ষ | শ | ⌫ |
| long-press | — | ⚠VERIFY | ঔ⚠VERIFY | ঐ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | ⚠VERIFY | — |

> Confirmed shift-pairs (high confidence): ব→ভ, ক→খ, ত→থ, দ→ধ, ি→ী, ো→ৌ, ে→ৈ, ন→ণ, স→ষ/শ, ঙ→ং, ড→ঢ, প→ফ, ট→ঠ, চ→ছ, জ→ঝ, গ→ঘ, ড়→ঢ়, য→য়. These pairings drive the shift map and are reliable even where a single superscript is fuzzy.

### 2.3 Layer C — Symbols `?123` (image 3)

Row 1 (primary / long-press = Latin digit):

| primary | ১ | ২ | ৩ | ৪ | ৫ | ৬ | ৭ | ৮ | ৯ | ০ |
|---------|---|---|---|---|---|---|---|---|---|---|
| long-press | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 0 |

Row 2 (primary / long-press):

| primary | @ | # | $ | % | & | * | - | + | ( | ) |
|---------|---|---|---|---|---|---|---|---|---|---|
| long-press | — | — | ৳ | — | — | ★ | _ | — | < | > |

Row 3: `ALT` · `!` · `"` · `'` · `:` · `;`(long-press ঃ⚠) · `/` · `?` · `⌫`
Bottom: `ABC` · mic · **spacebar "◄ ইউনিজয় ►"** · `.` · enter
(`ALT` opens a secondary symbols page — plan a Layer C2 for `~ = { } [ ] \ | € £ ¥ ° •` etc., Gboard-style.)

### 2.4 Layer D — English QWERTY (image 4)

Row 1 (primary / long-press digit):
`q(1) w(2) e(3) r(4) t(5) y(6) u(7) i(8) o(9) p(0)`

Row 2 (primary / long-press symbol):
`a(@) s(#) d(&) f(*) g(-) h(+) j(=) k(( ) l())`

Row 3: `⇧` · `z(_) x(") c(') v(:) b(;) n(/) m(?)`⚠VERIFY superscripts · `⌫`
Bottom: `123`+mic · `,` · **spacebar "◄ English ►"** · `.` · enter

---

## 3. Bengali text-handling rules (the correctness core)

These are why a Bengali keyboard is hard; the UI is the easy part.

1. **Storage vs. display order.** The pre-base vowel sign ই-kar `ি` (U+09BF) is *typed and stored after* its consonant but *rendered before* it. We store logical order and let Android's HarfBuzz shaper handle display. Never reorder in storage.
2. **Two-part vowel signs.** `ো` = ে(U+09C7)+া(U+09BE) conceptually but has its own single code point U+09CB; `ৌ` = U+09CC; `ৈ` = U+09C8. Emit the single canonical code point per key (the photos map one key → one sign), not decomposed sequences.
3. **Conjuncts (যুক্তাক্ষর).** Formed by `consonant + হসন্ত/virama ্ (U+09CD) + consonant`. The `্` key in Layer A row 2 must insert U+09CD; the shaper forms the ligature. Provide a **ZWJ/ZWNJ** affordance (long-press on `্`) for cases where users want to force/break a ligature (U+200D / U+200C).
4. **Grapheme-aware backspace.** Backspace deletes a full **grapheme cluster** (e.g. ক্ত or কি), not one code point, unless the user long-presses for fine delete. Use `BreakIterator.getCharacterInstance(BENGALI_LOCALE)` or ICU grapheme segmentation.
5. **Normalization.** Normalize output to **NFC**. Guard against illegal sequences (e.g. two vowel signs in a row, hasanta at word start) — either block or auto-correct per a validation table.
6. **Reph & ya-phala / ra-phala.** র্ (ref) and ্য/্র (phala) arise naturally from hasanta sequences; no special key needed, but the phonetic engine (§5.2) must produce them correctly.
7. **Bengali digits vs Latin digits.** Layer C primary = Bengali digits; long-press = Latin. Respect a user setting for default digit script.

A dedicated `BanglaTextValidator` + unit-test corpus enforces all of the above.

---

## 4. Architecture (Compose)

```
com.yourco.keyboard
├── service/
│   ├── KeyboardImeService : InputMethodService   // OS entry point, owns InputConnection
│   └── ImeLifecycleOwner                          // makes Compose work inside an IME window
├── engine/
│   ├── LayoutRepository        // loads JSON layouts (§6) into models
│   ├── KeyboardStateMachine    // active layer, shift state, mode, caps-lock
│   ├── TouchController         // tap / long-press / swipe / repeat-on-hold
│   └── PopupController         // long-press popups & key preview
├── input/
│   ├── InputProcessor (interface)     // onKey(event) -> InputConnection ops
│   ├── BanglaFixedProcessor           // UniJoy key->Unicode + §3 rules
│   ├── PhoneticProcessor              // Banglish transliteration (§5.2)
│   └── LatinProcessor                 // English
├── suggest/
│   ├── Dictionary (Trie + freq)       // Bangla + English, user dict
│   ├── NgramModel                     // next-word prediction
│   ├── AutoCorrector                  // edit-distance + keyboard-adjacency
│   └── SuggestionManager              // merges candidates for the strip
├── text/
│   ├── BanglaTextValidator            // §3 rules
│   └── GraphemeUtils                  // cluster-aware cursor/backspace
├── ui/  (Jetpack Compose)
│   ├── KeyboardScreen                 // top-level composable per layer
│   ├── KeyView / KeyRow / PopupView
│   ├── SuggestionStrip
│   ├── panels/ EmojiPanel, GifPanel, ClipboardPanel
│   └── theme/ KeyboardTheme, ThemeRepository
├── settings/  (Compose Activity)      // preferences UI
└── data/  DataStore prefs, Room (user dict, clipboard history)
```

### 4.1 Compose-inside-IME notes (known pitfalls)
- An `InputMethodService` is **not** a `ComponentActivity`; Compose needs `ViewTreeLifecycleOwner`, `ViewTreeViewModelStoreOwner`, and `ViewTreeSavedStateRegistryOwner` set on the `ComposeView` you return from `onCreateInputView()`. Provide a small `ImeLifecycleOwner` (FlorisBoard and the AndroidX `androidx.savedstate` samples do exactly this).
- Keep the composable tree **shallow and stateless per key**; hoist all state into `KeyboardStateMachine` (a `StateFlow`), because IME views are recreated on config/theme change.
- Touch latency matters: use `pointerInput`/`detectTapGestures` carefully, or drop to a custom `Modifier.pointerInteropFilter` for the key grid if Compose gesture latency is measurable vs. a custom View. Benchmark in Phase 1; keep an escape hatch to render the key grid as an `AndroidView` if needed.

---

## 5. Input modes

### 5.1 Bangla fixed (UniJoy) — `BanglaFixedProcessor`
- Pure table lookup: `(layer, shiftState, keyId) -> outputString`.
- Applies §3 rules on commit; uses composing region only for the current cluster so backspace/reorder behaves.
- Shift is one-shot (auto-releases after one key) unless caps-locked (double-tap shift).

### 5.2 Banglish phonetic — `PhoneticProcessor`
The differentiator. Roman → Bengali as-you-type, with a candidate list.

> **✅ REFERENCE ENGINE RECOVERED.** The old app's complete phonetic engine was extracted: **[reference/RidmikParser.java](reference/RidmikParser.java)** (`toBangla(String)` — a sliding-window parser tracking the previous 3 chars via `v1`/`v9`/`v12`) and **[reference/BanglaUnicode.java](reference/BanglaUnicode.java)** (the full rule tables). We **port these to Kotlin** rather than rebuild from scratch. Key tables in `BanglaUnicode`:
> - `map` — Roman→Bengali base letters (`kh→খ`, `Ng→ঙ`, `x→ক্ষ`, `nj→ঞ্জ`, `gg→জ্ঞ`, `OU→ঔ`…)
> - `kars` — vowel signs (`i→ি`, `U→ূ`, `OI→ৈ`…)
> - `jkt` / `djkt` / `djktt` — juktakkhor (conjunct) join rules driving auto-hasanta
> - Hardcoded special cases in `toBangla`: ঋ/ৃ (ri), reph, ya-phala, ৎ, ZWJ `‍`, `x→ক্ষ` at word start, etc.
>
> **Port plan:** translate literally to Kotlin first (keep behavior identical, add a golden-file test comparing Kotlin output to the Java reference over a word list), then refactor. Externalize `map`/`kars`/`jkt` tables to JSON so they're tunable without recompiling. Note the legacy code is dense/obfuscated (`v1`,`v9` = previous chars) — preserve the logic, improve the names.

- **Context sensitivity** is already handled by the sliding window (vowel full-form vs. sign depends on whether the previous char is a consonant/vowel/zero).
- **Candidates:** the parser output is candidate #1; the **dictionary** supplies real-word candidates ranked by frequency (typing "amar" → আমার, আমরা, আমান …). Selecting a candidate commits + trains the user model.

### 5.3 English — `LatinProcessor`
- Standard commit + composing region, autocorrect + prediction from the English dictionary/n-gram.
- Long-press = accent popups; double-space = `.`; auto-capitalize sentence start.

### 5.4 The blue punctuation strip
Photos show a fixed punctuation strip above letters. Options: (a) render it as a permanent thin row above the top letter row on Bangla/English layers, or (b) fold it into the suggestion strip and reveal on long-press. **Recommendation:** permanent thin row to match the photos exactly; make its visibility a setting.

---

## 6. Layout file schema (data-driven)

Layouts are **data, not code**, so the exact photos are encoded once and tweaked freely. One JSON per layer, referenced by a keyboard-set manifest.

```jsonc
// layouts/bn_unijoy_normal.json
{
  "id": "bn_unijoy_normal",
  "mode": "bangla_fixed",
  "shiftTo": "bn_unijoy_shift",
  "rows": [
    { "keys": [
      { "id": "k_uu", "label": "ঙ", "output": "ঙ", "longPress": { "label": "১", "output": "১" } },
      { "id": "k_ja", "label": "য", "output": "য", "longPress": { "label": "২", "output": "২" } }
      // … 10 keys
    ]},
    // row 2 (9 keys), row 3 (shift + 7 + backspace)
    { "keys": [
      { "type": "shift" },
      { "id": "k_o_kar", "label": "ো", "output": "ো", "longPress": {"label":"ও","output":"ও"} },
      // …
      { "type": "backspace" }
    ]},
    { "keys": [
      { "type": "layerSwitch", "target": "symbols", "label": "?১২৩" },
      { "id": "k_comma", "label": ",", "output": "," },
      { "type": "space", "label": "◄ ইউনিজয় ►" },
      { "id": "k_danda", "label": "।", "output": "।" },
      { "type": "enter" }
    ]}
  ]
}
```

Key `type`s: `char` (default), `shift`, `backspace`, `space`, `enter`, `layerSwitch`, `modeSwitch`, `emoji`. Special values (`output`) use `\uXXXX` for clarity. A JSON-schema file validates every layout at build time.

---

## 7. Suggestion & prediction engine

- **Dictionaries:** ship precompiled Bangla + English word lists with frequencies as a compact Trie (or FST) in assets; merge a **user dictionary** (Room) that learns from accepted words.
- **Next-word:** on-device n-gram (bigram/trigram) with Kneser-Ney smoothing; small, fast, offline. Optional upgrade path: a tiny quantized neural LM later.
- **Autocorrect:** edit distance weighted by keyboard adjacency + phonetic similarity; conservative by default (Bengali users hate aggressive autocorrect).
- **Sources of candidates merged by `SuggestionManager`:** exact dict match, autocorrect, phonetic engine output, user-dict, emoji suggestions, clipboard.
- **Data:** where to source Bengali frequency lists — Wikipedia dumps, OSCAR/CC-100 Bengali, Prothom Alo corpus (respect licensing). Document provenance.

---

## 8. Feature roadmap to match/beat Gboard

| Feature | Priority | Notes |
|---|---|---|
| 4-layer UniJoy layout (photos) | P0 | The core ask |
| Bangla correctness (§3) | P0 | Conjuncts, ordering, backspace |
| Banglish phonetic + candidates | P0 | Key differentiator vs Gboard |
| Suggestion strip + next-word | P0 | |
| Themes (light/dark/custom) | P1 | Compose theming, wallpaper-adaptive |
| Emoji panel + search | P1 | Recent, categories, skin tones |
| Gesture/glide typing | P1 | Hard; do after tap is solid |
| Haptics + key sound + preview popup | P1 | Feel parity |
| Clipboard manager | P2 | Room-backed, auto-expire |
| GIF/sticker | P2 | Needs a provider (Tenor) + API key |
| Voice input | P2 | Delegate to system speech recognizer |
| One-handed / floating / split | P2 | |
| Number-row toggle, height/size settings | P2 | |
| Per-app language memory | P2 | |

**Honest positioning:** matching Gboard's English/gesture engine is a multi-year, data-heavy effort. Gboard's *Bengali* and *Banglish* support is weak — that's the winnable ground. Strategy: be unbeatable at Bengali/Banglish with the familiar UniJoy layout, hit table-stakes parity elsewhere.

---

## 9. Project setup & dependencies

- **Language/build:** Kotlin, Gradle KTS, `minSdk 24` (Android 7), `targetSdk` latest.
- **UI:** Jetpack Compose (BOM), Material 3.
- **Persistence:** DataStore (prefs), Room (user dict, clipboard).
- **Async:** Coroutines + Flow.
- **DI:** Hilt (or manual — keep light for an IME).
- **Text:** ICU4J / `android.icu` for grapheme + normalization.
- **Testing:** JUnit + a Bengali golden-file corpus; Compose UI tests; a macrobenchmark for key latency.
- **Manifest:** `<service>` with `BIND_INPUT_METHOD` permission + `android.view.im` metadata (`method.xml` declaring subtypes: `bn` fixed, `bn` phonetic, `en`).
- **Reference implementations to study (Apache/GPL — check license before borrowing code):** FlorisBoard (Compose IME architecture, layout-as-data), HeliBoard/OpenBoard (AOSP LatinIME engine, dictionaries, gesture typing), Avro Phonetic (rule grammar).

---

## 10. Implementation blueprint (live tracker)

This is the single source of truth for build progress. Every unit of work is a task with an ID, a checkbox, and a **DoD** (definition of done). Work top-to-bottom; respect `deps`. Update this file as the first step of finishing any task.

### 10.0 How to use this tracker
- **Checkbox** = state: `- [ ]` todo · `- [~]` in progress · `- [x]` done · `- [!]` blocked (add `— blocked: reason`).
- **Task ID** (e.g. `P1.3`) is stable — reference it in branch names (`feat/P1.3-encode-unijoy`), commits (`P1.3: encode unijoy normal layout`), and PRs so history maps back here.
- **DoD** is the acceptance criteria; a task isn't `[x]` until its DoD holds and its tests pass.
- **Gate** at each phase end is a hard checkpoint — do not start the next phase until the gate passes.
- Keep the **dashboard (§10.1)** status in sync with the phase task lists.

**Global DoD (applies to every code task):** compiles; unit tests for the unit pass; no new lint errors; public APIs documented; if it touches typing behavior, a golden/UI test covers it; tracker updated.

### 10.1 Milestone dashboard
| Phase | Milestone | Est. | Status | Gate |
|-------|-----------|------|--------|------|
| P0 | Scaffold & IME plumbing | 1–2 d | 🟢 done (verified in Android Studio) | Keyboard appears in picker, types 1 char on device |
| P1 | Exact 4-layer layout rendering | 1–1.5 wk | 🟡 code written; on-device QA pending | All layers match photos, type correct Unicode |
| P2 | Bengali correctness core | 1 wk | 🟡 code + tests green; on-device QA pending | Golden corpus passes; cluster backspace works |
| P3 | Banglish phonetic (port) | 1–2 wk | 🔴 | Kotlin output == Java reference on word list |
| P4 | Suggestions / prediction / autocorrect | 2 wk | 🔴 | Relevant suggestions in all 3 modes |
| P5 | Parity polish (themes, emoji, settings, …) | ongoing | 🔴 | P1.2 feature set complete |
| P6 | Hardening & release | 1–2 wk | 🔴 | Passes device matrix; shippable build |

Status legend: 🔴 not started · 🟡 in progress · 🟢 done · ⛔ blocked.

---

### P0 — Project scaffold & IME plumbing
*Goal: prove the riskiest infra (Compose inside an IME) end-to-end before building features.*

- [x] **P0.1** Create Android Studio project — Kotlin, Compose, Gradle KTS, minSdk 28, `com.kinetic.keyboard`. *Done: builds & runs in Android Studio.*
- [x] **P0.2** Add dependencies — version catalog `gradle/libs.versions.toml` (Compose BOM, Material 3, DataStore, lifecycle, savedstate, kotlinx-serialization, test libs). *Done: sync green.* `deps: P0.1`
- [x] **P0.3** Declare the IME — `<service>` with `BIND_INPUT_METHOD` + `res/xml/method.xml` (subtypes bn-unijoy / bn-phonetic / en). *Done: manifest + method.xml written.* `deps: P0.1`
- [x] **P0.4** `KeyboardImeService : InputMethodService` — `onCreateInputView()` returns a `ComposeView`. *Done.* `deps: P0.3`
- [x] **P0.5** `ImeLifecycleOwner` — sets `ViewTreeLifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner` on the ComposeView. *Done.* `deps: P0.4`
- [x] **P0.6** Minimal keyboard composable — placeholder keys commit via `InputConnection`. *Done (`MinimalKeyboard.kt`).* `deps: P0.5`
- [x] **P0.7** Manual device/emulator test — enable in picker, switch, type. *Done: verified by user in Android Studio.* `deps: P0.1, P0.6`
- [ ] **P0.8** CI pipeline — GitHub Actions: build + lint + unit tests on push. *DoD: green run on main.* `deps: P0.2`
- [~] **P0.9** Repo hygiene — README ✓, `.gitignore` ✓, module layout ✓; LICENSE decision pending (§12 Q4). *DoD: docs present + license chosen.*

**🚦 Gate P0: ✅ PASSED** — keyboard installs, appears in the system picker, types on device. Compose-in-IME confirmed working.

---

### P1 — Exact 4-layer layout rendering
*Goal: the real photographed keyboard — all four layers — typing correct Unicode. Encode from [reference/EXTRACTED_LAYOUTS.md](reference/EXTRACTED_LAYOUTS.md), the authoritative source.*

- [x] **P1.1** Data model — `LayoutDef`/`RowDef`/`KeyDef` (`engine/model/LayoutModels.kt`): type, label, output, popup[], width%, gap, target. *Done.*
- [x] **P1.2** JSON schema + validator — kotlinx.serialization strict parse + `LayoutParser.validate()` (structure, widths ≤100%, known types, targets). *Done; enforced by unit tests.* `deps: P1.1`
- [x] **P1.3** Layer A `assets/layouts/bn_unijoy.json` — all popups (`ড→ঢ,৩`…), ্র conjunct key, danda. *Done.* `deps: P1.2`
- [x] **P1.4** Layer B `bn_unijoy_shift.json` — র্ reph, ্য ya-phola, ঁ, ঈ/ঊ popups. *Done.* `deps: P1.2`
- [x] **P1.5** Layers C/C2 `symbols.json` + `symbols_shift.json` — fractions, currency (`$→¢£৳€¥₣₤₱`), ALT page. *Done.* `deps: P1.2`
- [x] **P1.6** Layer D `en_qwerty.json` — long-press digits/symbols per `alternates_for_*`. *Done.* `deps: P1.2`
- [x] **P1.7** `LayoutRepository` — asset load + cache; parser is pure-JVM for testability. *Done.* `deps: P1.3–P1.6`
- [x] **P1.8** `KeyboardStateMachine` — layer/shift/language via `StateFlow`; one-shot shift, double-tap caps-lock. *Done (dedicated unit tests still worth adding).* `deps: P1.7`
- [x] **P1.9** Compose render — `KeyboardScreen` with weight-based widths, row gaps (5% offsets), hint superscripts. *Done.* `deps: P1.8`
- [x] **P1.10** Touch — tap dispatch, pressed highlight. *Done (in `KeyView` gestures).* `deps: P1.9`
- [~] **P1.11** Popups — long-press opens multi-char popup, tap-to-commit. *Done; slide-to-select refinement pending.* `deps: P1.10`
- [x] **P1.12** Special keys — shift (one-shot/lock), backspace repeat-on-hold, space (label + swipe = language cycle), action-aware enter, `?123`/`ABC`/`ALT`, danda, tab. *Done.* `deps: P1.10`
- [x] **P1.13** Wired to `InputConnection` in `KeyboardImeService.handleAction`. *Done.* `deps: P1.11, P1.12`
- [ ] **P1.14** Latency benchmark + UI-stack decision — measure; fall back to custom View if needed. *DoD: recorded numbers + decision.* `deps: P1.13`
- [ ] **P1.15** Visual QA vs. original photos — side-by-side all layers on device. *DoD: sign-off; fixes filed.* `deps: P1.13`

**🚦 Gate P1:** all four layers render faithfully to the photos, every key/popup types its correct Unicode, and shift/layer switching works on device.

---

### P2 — Bengali correctness core
*Goal: output is always valid, normalized Bengali; editing respects grapheme clusters. See §3.*

- [x] **P2.1** `BengaliText` cluster segmentation — purpose-built backward scanner (virama chains, nukta, joiners) instead of ICU: JVM-testable, deterministic, typing-tuned. *Done + golden tests.*
- [x] **P2.2** Grapheme-aware backspace — one press deletes a full cluster (শক্তি → শ in two presses); selection-aware. *Done (`handleDelete`). Deviation: repeat-on-hold also deletes clusters (predictable), not code points.* `deps: P2.1`
- [x] **P2.3** NFC normalization on commit (in `BanglaTextValidator.process`). *Done + test.*
- [x] **P2.4** `BanglaTextValidator` — permissive policy with repairs: double vowel-sign → replace, sign-after-hasanta → replace, no double hasanta. *Done + tests.*
- [x] **P2.5** Hasanta long-press → explicit ্+ZWNJ (break ligature) and ্+ZWJ (force) variants. *Done (popup on ্).* `deps: P2.1`
- [x] **P2.6** Conjunct keys ্র / র্ / ্য verified end-to-end (ক+্র→ক্র, র্+ক→র্ক). *Done: golden tests.* `deps: P2.5`
- [ ] **P2.7** Cursor movement respects clusters. *Partially N/A — layout has no arrow keys; app-side taps control the caret. Revisit if cursor keys are added (P5).* `deps: P2.1`
- [~] **P2.8** Golden corpus — ~40 cases across segmentation + validator, running in unit tests. *Grow toward 200 as bugs surface.* `deps: P2.2–P2.6`

**🚦 Gate P2:** the golden corpus passes end-to-end; backspace/cursor are cluster-correct; all output is valid NFC. *Status: 21/21 unit tests green (build 2026-07-04); needs on-device confirmation alongside P1.15.*

---

### P3 — Banglish phonetic engine (port from reference)
*Goal: as-you-type Roman→Bengali matching the old app. Port [reference/RidmikParser.java](reference/RidmikParser.java) + [reference/BanglaUnicode.java](reference/BanglaUnicode.java); do not rebuild from scratch.*

- [ ] **P3.1** Port `BanglaUnicode` tables to Kotlin (`map`, `kars`, `jkt`, `djkt`, `djktt`). *DoD: table-equality test vs. the Java maps.*
- [ ] **P3.2** Port `RidmikParser.toBangla()` literally to Kotlin (preserve logic; keep `v1/v9/v12` semantics via a comment). *DoD: compiles, runs on a string.* `deps: P3.1`
- [ ] **P3.3** Golden equivalence test — run a large Banglish word list through Kotlin vs. the Java reference; outputs must be identical. *DoD: 100% match (or documented, justified diffs).* `deps: P3.2`
- [ ] **P3.4** Streaming/incremental parse — re-run over the current word as user types, backed by the composing region (not whole-buffer). *DoD: typing "amar" updates composing text live.* `deps: P3.3`
- [ ] **P3.5** `PhoneticProcessor` — integrate with `InputConnection` composing region; commit on space/punct/candidate select. *DoD: full words type correctly in a field.* `deps: P3.4`
- [ ] **P3.6** Refactor — rename cryptic vars, externalize `map`/`kars`/`jkt` to JSON assets, keep the golden test green throughout. *DoD: readable code, tests still pass.* `deps: P3.3`
- [ ] **P3.7** Candidate generation — parser output as candidate #1 (dictionary ranking added in P4). *DoD: at least the deterministic transliteration surfaces.* `deps: P3.5`

**🚦 Gate P3:** Banglish typing on device produces the same output as the reference app for the test word list.

---

### P4 — Suggestions, prediction & autocorrect
*Goal: a useful suggestion strip and next-word prediction across all three modes. See §7.*

- [ ] **P4.1** Dictionary format + asset build pipeline — compact Trie/FST with frequencies. *DoD: builder script + loader round-trip test.*
- [ ] **P4.2** Source & preprocess corpora — Bangla + English word-frequency lists; record provenance & license. *DoD: cleaned lists checked in / scripted.* `deps: P4.1`
- [ ] **P4.3** `Dictionary` loader + prefix lookup. *DoD: lookup latency < a few ms; unit tests.* `deps: P4.2`
- [ ] **P4.4** `SuggestionStrip` Compose UI — 3-slot strip, scroll, tap-to-commit, blue punctuation strip (§5.4). *DoD: renders & commits.* `deps: P1.9`
- [ ] **P4.5** Prefix suggestions wired strip↔engine for fixed + English modes. *DoD: typing shows ranked candidates.* `deps: P4.3, P4.4`
- [ ] **P4.6** N-gram next-word model (bigram/trigram, Kneser-Ney). *DoD: predicts next word after commit; tests.* `deps: P4.3`
- [ ] **P4.7** `AutoCorrector` — edit-distance + keyboard-adjacency, conservative defaults, easy undo. *DoD: fixes typos without over-correcting (test set).* `deps: P4.3`
- [ ] **P4.8** User dictionary (Room) — learn accepted/typed words, boost their frequency. *DoD: accepted word ranks higher next time.* `deps: P4.3`
- [ ] **P4.9** `SuggestionManager` — merge & rank exact/autocorrect/phonetic/user/emoji/clipboard sources. *DoD: single ranked list; unit tests for ranking.* `deps: P4.5–P4.8`
- [ ] **P4.10** Phonetic candidates in strip (dictionary-ranked Banglish). *DoD: "amar" → আমার, আমরা… ranked.* `deps: P3.7, P4.9`

**🚦 Gate P4:** relevant word suggestions and next-word prediction work in Bangla-fixed, Banglish, and English.

---

### P5 — Parity polish (match/beat Gboard)
*Goal: the §1.2 feature set. Each item is independently shippable; prioritize P1-tier first.*

- [ ] **P5.1** Settings app — Compose Activity + DataStore (layouts, theme, feedback, digit script §12 Q2). *DoD: settings persist & take effect live.*
- [ ] **P5.2** Theme system — light/dark + custom, `ThemeRepository`, wallpaper-adaptive option. *DoD: theme switch with no restart.* `deps: P1.9`
- [ ] **P5.3** Sizing prefs — key height, number-row toggle, punctuation-strip toggle, one-handed offset. *DoD: layout responds live.* `deps: P5.1`
- [ ] **P5.4** Feedback — haptics, key sound, key-preview popup (all toggleable). *DoD: honors system + app settings.*
- [ ] **P5.5** Emoji panel — categories, recents, skin tones, search, emoji suggestions. *DoD: inserts emoji; recents persist.* `deps: P1.9`
- [ ] **P5.6** Clipboard manager — Room-backed history, pin, auto-expire. *DoD: copy→appears→paste; expiry works.*
- [ ] **P5.7** Voice input — delegate to system speech recognizer via the mic key. *DoD: dictation inserts text.*
- [ ] **P5.8** Per-app language/mode memory. *DoD: reopening an app restores its last mode.* `deps: P5.1`
- [ ] **P5.9** Gesture / glide typing (own sub-milestone — hard). *DoD: swiping a word commits it; accuracy baseline met.* `deps: P4.3`
- [ ] **P5.10** GIF / sticker panel (Tenor provider + API key). *DoD: search & insert.* `deps: P5.5`
- [ ] **P5.11** One-handed / floating / split modes. *DoD: each mode usable & persisted.* `deps: P5.3`

**🚦 Gate P5:** all §1.2 P1-priority features complete and stable.

---

### P6 — Hardening & release
- [ ] **P6.1** Device/OEM matrix — Bengali shaping across fonts/Android versions; bundle fallback font. *DoD: no rendering regressions on the matrix.*
- [ ] **P6.2** Accessibility — TalkBack, content descriptions, min touch targets. *DoD: navigable via screen reader.*
- [ ] **P6.3** Performance pass — cold start, memory, sustained key latency. *DoD: meets budget from P1.14.*
- [ ] **P6.4** Privacy review — guarantee no network on the typing path; document data handling. *DoD: written statement + a test asserting no egress while typing.*
- [ ] **P6.5** Store assets — listing, screenshots, privacy policy, content rating. *DoD: Play Console draft complete.*
- [ ] **P6.6** Release engineering — signing, versioning, R8/proguard rules for the IME, release build. *DoD: signed AAB builds & runs.*
- [ ] **P6.7** Closed beta — internal/closed track, collect feedback, triage. *DoD: beta live, feedback loop running.* `deps: P6.6`

**🚦 Gate P6:** passes the device matrix, privacy guarantee holds, signed build shippable to Play.

---

### 10.2 Immediate next actions (start here)
1. ✅ Gate P0 passed. ✅ P1 core built: 5 layouts encoded, state machine, Compose renderer, popups, special keys, InputConnection wiring, layout unit tests.
2. **Re-run from Android Studio** and QA on device: all 4 layers vs. the photos (**P1.15**), popup feel, shift behavior, space-swipe language switch.
3. **P1.14** latency check (subjective is fine at first: no visible lag while typing fast).
4. Then **P2**: grapheme-aware backspace + Bengali correctness rules (§3).

---

## 11. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Compose gesture latency in IME | Benchmark Phase 1; fall back to custom-View key grid if needed (architecture keeps UI swappable) |
| Bengali shaping/rendering bugs across OEM fonts | Test on multiple devices; ship a bundled Bengali font as fallback |
| Ambiguous glyphs in reference photos | `⚠VERIFY` cells; confirm against originals before coding |
| Gesture typing complexity | Defer to P1/P2; ship excellent tap-typing first |
| Dictionary/corpus licensing | Track provenance; prefer permissively-licensed corpora |
| Aggressive autocorrect frustrating users | Conservative defaults, easy undo, per-mode toggle |

---

## 12. Open questions for you
1. **Branding/package name** for the app and IME id? (Old one was `net.hasnath.android.keyboard` — we need a new, distinct id.)
2. **Digit default** — Bengali (১২৩) or Latin (123) as the primary on the number row? (Symbols layer shows Latin primary with Bengali on long-press.)
3. **Monetization/distribution** — Play Store, free/paid, any privacy stance to advertise (fully offline is a strong selling point vs Gboard)?
4. **Licensing** — the old app is an AOSP LatinIME derivative (Apache-2.0) with custom Ridmik code. We're re-implementing clean in Kotlin/Compose and *porting* the phonetic logic. Confirm you have the right to reuse this code/these layouts (your own app? permission?) before we ship.
5. **Min Android version** you care about — affects Compose/API choices.

> ~~Higher-res images~~ — no longer needed; exact layouts recovered from the APK (§2, `reference/`).
```

