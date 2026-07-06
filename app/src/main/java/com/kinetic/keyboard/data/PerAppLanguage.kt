package com.kinetic.keyboard.data

import java.io.File

/**
 * P5.8: remembers the last language used in each app (package → language-set id) so reopening
 * an app restores its mode — chat in Bangla, terminal in English. File-backed in app-private
 * storage (see PRIVACY.md); load/save follow the UserDictionary pattern.
 */
class PerAppLanguage(private val file: File) {

    private val map = mutableMapOf<String, String>()
    @Volatile private var dirty = false

    @Synchronized
    fun load() {
        if (!file.isFile) return
        map.clear()
        file.forEachLine { line ->
            val (pkg, lang) = line.split('\t').takeIf { it.size == 2 } ?: return@forEachLine
            if (pkg.isNotEmpty() && lang.isNotEmpty()) map[pkg] = lang
        }
    }

    @Synchronized
    fun get(packageName: String): String? = map[packageName]

    @Synchronized
    fun set(packageName: String, languageId: String) {
        if (map.put(packageName, languageId) != languageId) dirty = true
    }

    @Synchronized
    fun saveIfDirty() {
        if (!dirty) return
        file.writeText(map.entries.joinToString("\n") { (pkg, lang) -> "$pkg\t$lang" })
        dirty = false
    }
}
