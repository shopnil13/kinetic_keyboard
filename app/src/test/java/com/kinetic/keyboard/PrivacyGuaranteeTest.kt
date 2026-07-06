package com.kinetic.keyboard

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P6.4: the privacy guarantee is enforced at the OS level — the app must never hold a
 * network-capable permission, so nothing typed can leave the device. PRIVACY.md documents the
 * full data-handling statement; this test keeps the manifest honest.
 *
 * The release checklist additionally verifies the merged APK:
 * `aapt2 dump permissions app-release.apk` must list no android.permission.* entries.
 */
class PrivacyGuaranteeTest {

    private val networkPermissions = listOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_NETWORK_STATE",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.BLUETOOTH",
    )

    /** Unit-test working dir is the module dir; walk up in case a runner differs. */
    private fun sourceManifest(): File {
        var dir: File? = File(System.getProperty("user.dir")!!)
        while (dir != null) {
            val candidates = listOf(
                File(dir, "src/main/AndroidManifest.xml"),
                File(dir, "app/src/main/AndroidManifest.xml"),
            )
            candidates.firstOrNull(File::isFile)?.let { return it }
            dir = dir.parentFile
        }
        error("AndroidManifest.xml not found from ${System.getProperty("user.dir")}")
    }

    @Test
    fun `manifest declares no uses-permission at all`() {
        val manifest = sourceManifest().readText()
        assertTrue(
            "The keyboard must not request ANY permission — see PRIVACY.md before adding one.",
            !manifest.contains("<uses-permission"),
        )
    }

    @Test
    fun `merged manifest (when built) has no network permissions`() {
        // Opportunistic: only present after an assemble; catches permissions smuggled in by
        // dependencies, which the source-manifest check above cannot see.
        val merged = File(sourceManifest().parentFile.parentFile.parentFile, "build")
            .walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && it.path.contains("merged_manifest") }
            .toList()
        merged.forEach { file ->
            val text = file.readText()
            networkPermissions.forEach { perm ->
                assertTrue(
                    "Merged manifest ${file.path} contains $perm — a dependency added it!",
                    !text.contains(perm),
                )
            }
        }
    }
}
