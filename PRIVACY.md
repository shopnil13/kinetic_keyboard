# Privacy statement — Kinetic Keyboard

_Last updated: 2026-07-07 (P6.4, revised for the P5.10 GIF/sticker panel)_

**Nothing you type ever leaves your device.** The typing path — keys, learning,
suggestions, autocorrect — is fully offline. The one network feature is the optional
GIF/sticker panel, which talks to GIPHY only when you open it.

## The guarantee

- The app holds exactly **one** permission: `android.permission.INTERNET`, used solely by
  the GIF/sticker panel (GIPHY search/trending and media downloads). No other permission
  is requested — no contacts, microphone, location, or network-state access.
- Network code is physically confined to the `giphy` package; typing, dictionary,
  and suggestion code cannot reach the network at all.
- Both facts are locked by `PrivacyGuaranteeTest` (runs in CI on every push): the manifest
  may hold only INTERNET, dependencies may not add permissions, and any network API used
  outside the `giphy` package fails the build.
- What GIPHY sees when you use the panel: your search query and standard connection
  metadata (IP address) — governed by [GIPHY's privacy policy](https://support.giphy.com/hc/en-us/articles/360032872931).
  Nothing from your typing, dictionary, or clipboard is ever included.
- Release checklist: `aapt2 dump permissions app-release.apk` must list exactly
  `android.permission.INTERNET` and nothing else.

## What the keyboard stores (on your device only)

| Data | Where | Why | Lifetime |
|------|-------|-----|----------|
| Words you type often (user dictionary) | App-private storage (`user_dict.tsv`) | Better suggestions & autocorrect | Until you clear app data |
| Word pairs (bigrams) | App-private storage (`user_bigrams.tsv`) | Next-word prediction | Until you clear app data |
| Settings (theme, key height, feedback toggles) | App-private DataStore | Your preferences | Until you clear app data |

App-private storage is sandboxed by Android: no other app can read these files, and they
are removed when the app is uninstalled.

| Recently used emoji | App-private storage (`emoji_recents.txt`) | Recents tab in the emoji panel | Until you clear app data |
| Downloaded GIFs | App cache (`cache/giphy/`) | Inserting the GIF you picked | Cleared with the app cache |

## What the keyboard does NOT do

- No network access from the typing path (test-enforced, see above)
- No analytics, telemetry, crash reporting, or advertising SDKs
- No reading of the clipboard in the background
- No account, sign-up, or cloud sync
- Password and `textNoSuggestions` fields are never fed into learning, never autocorrected,
  and never show suggestions (enforced in `KeyboardImeService.isPrivateField`)

## Contact

Questions: open an issue at https://github.com/shopnil13/kinetic_keyboard
