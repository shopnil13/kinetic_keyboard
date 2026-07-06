# Privacy statement — Kinetic Keyboard

_Last updated: 2026-07-06 (P6.4)_

Kinetic Keyboard is a fully offline input method. **Nothing you type ever leaves your
device.** This is not a policy promise — it is enforced by the operating system:

## The guarantee

- The app requests **no permissions at all**. In particular it does not hold
  `android.permission.INTERNET`, so Android forbids it from opening any network
  connection. There is no code path — first-party or dependency — that can transmit data.
- This is locked by `PrivacyGuaranteeTest` (runs in CI on every push): the manifest may not
  contain a single `<uses-permission>` entry, and the merged manifest may not gain a
  network permission from a dependency.
- Release checklist: `aapt2 dump permissions app-release.apk` must list no
  `android.permission.*` entries.

## What the keyboard stores (on your device only)

| Data | Where | Why | Lifetime |
|------|-------|-----|----------|
| Words you type often (user dictionary) | App-private storage (`user_dict.tsv`) | Better suggestions & autocorrect | Until you clear app data |
| Word pairs (bigrams) | App-private storage (`user_bigrams.tsv`) | Next-word prediction | Until you clear app data |
| Settings (theme, key height, feedback toggles) | App-private DataStore | Your preferences | Until you clear app data |

App-private storage is sandboxed by Android: no other app can read these files, and they
are removed when the app is uninstalled.

## What the keyboard does NOT do

- No network access (OS-enforced, see above)
- No analytics, telemetry, crash reporting, or advertising SDKs
- No reading of the clipboard in the background
- No account, sign-up, or cloud sync
- Password and `textNoSuggestions` fields are never fed into learning, never autocorrected,
  and never show suggestions (enforced in `KeyboardImeService.isPrivateField`)

## Contact

Questions: open an issue at https://github.com/shopnil13/kinetic_keyboard
