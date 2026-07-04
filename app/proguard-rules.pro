# Kinetic Keyboard — ProGuard/R8 rules.
# The IME service is referenced from the framework by name; keep it.
-keep class com.kinetic.keyboard.service.KeyboardImeService { *; }

# Add keyboard-engine keep rules here as the app grows (e.g. JSON-mapped layout models).
