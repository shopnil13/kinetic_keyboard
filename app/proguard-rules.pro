# Kinetic Keyboard — ProGuard/R8 rules.
# The IME service is referenced from the framework by name; keep it.
-keep class com.kinetic.keyboard.service.KeyboardImeService { *; }

# Layout JSON is deserialized via kotlinx.serialization. The runtime artifact ships its own
# consumer rules, but R8 full mode can still strip the generated $serializer objects when the
# only references are reflective — keep the model package's serializers explicitly (P6.6).
-keep,includedescriptorclasses class com.kinetic.keyboard.engine.model.**$$serializer { *; }
-keepclassmembers class com.kinetic.keyboard.engine.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.kinetic.keyboard.engine.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
