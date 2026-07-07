package com.kinetic.keyboard

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P6.4 (revised for P5.10): the typing path never touches the network. Since the GIF/sticker
 * panel (GIPHY) needs INTERNET, the guarantee is now enforced three ways:
 *  1. the manifest may hold ONLY the INTERNET permission — nothing else, ever;
 *  2. dependencies may not smuggle in additional network/state permissions;
 *  3. network code (java.net / http clients) may exist ONLY in the giphy package, so no
 *     typing, learning, or suggestion code can physically reach the network.
 * PRIVACY.md documents the full data-handling statement; this test keeps it honest.
 */
class PrivacyGuaranteeTest {

    private val allowedPermissions = setOf("android.permission.INTERNET")

    private val forbiddenExtras = listOf(
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_NETWORK_STATE",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.BLUETOOTH",
        "android.permission.READ_CONTACTS",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
    )

    /** Unit-test working dir is the module dir; walk up in case a runner differs. */
    private fun moduleDir(): File {
        var dir: File? = File(System.getProperty("user.dir")!!)
        while (dir != null) {
            if (File(dir, "src/main/AndroidManifest.xml").isFile) return dir
            if (File(dir, "app/src/main/AndroidManifest.xml").isFile) return File(dir, "app")
            dir = dir.parentFile
        }
        error("module dir not found from ${System.getProperty("user.dir")}")
    }

    @Test
    fun `manifest holds only the INTERNET permission`() {
        val manifest = File(moduleDir(), "src/main/AndroidManifest.xml").readText()
        val declared = Regex("<uses-permission[^>]*android:name=\"([^\"]+)\"")
            .findAll(manifest).map { it.groupValues[1] }.toSet()
        assertEquals(
            "Only INTERNET is permitted (GIF/sticker panel) — see PRIVACY.md before adding any.",
            allowedPermissions, declared,
        )
    }

    @Test
    fun `merged manifest (when built) gains no extra permissions from dependencies`() {
        // Opportunistic: only present after an assemble; catches permissions smuggled in by
        // dependencies, which the source-manifest check above cannot see.
        File(moduleDir(), "build").walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && it.path.contains("merged_manifest") }
            .forEach { file ->
                val text = file.readText()
                forbiddenExtras.forEach { perm ->
                    assertTrue(
                        "Merged manifest ${file.path} contains $perm — a dependency added it!",
                        !text.contains(perm),
                    )
                }
            }
    }

    @Test
    fun `network code lives only in the giphy package`() {
        val srcRoot = File(moduleDir(), "src/main/java/com/kinetic/keyboard")
        val offenders = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.replace('\\', '/').contains("/giphy/") }
            .filter { file ->
                val text = file.readText()
                text.contains("java.net.") || text.contains("okhttp") ||
                    text.contains("HttpURLConnection") || text.contains("Socket(")
            }
            .map { it.name }
            .toList()
        assertTrue(
            "Network code outside the giphy package: $offenders — the typing path must stay offline.",
            offenders.isEmpty(),
        )
    }
}
