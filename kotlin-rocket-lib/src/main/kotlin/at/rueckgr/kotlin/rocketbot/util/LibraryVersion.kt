package at.rueckgr.kotlin.rocketbot.util

import java.io.InputStream
import java.util.*

class LibraryVersion {
    companion object {
        val instance = LibraryVersion()
    }

    fun getVersion(): VersionInfo {
        return when (val resource = LibraryVersion::class.java.getResourceAsStream("/library-git-revision")) {
            null -> VersionInfo("unknown", "unknown")
            else -> readVersionInfo(resource)
        }
    }

    private fun readVersionInfo(stream: InputStream): VersionInfo {
        val properties = Properties()
        properties.load(stream)

        return VersionInfo(properties.getProperty("revision"), properties.getProperty("commitMessage"))
    }
}
