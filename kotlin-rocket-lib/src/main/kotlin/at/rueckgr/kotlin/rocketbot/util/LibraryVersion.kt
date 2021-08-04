package at.rueckgr.kotlin.rocketbot.util

class LibraryVersion {
    companion object {
        val revision: String = when (val resource = LibraryVersion::class.java.getResource("/library-git-revision")) {
            null -> "unknown"
            else -> resource.readText().trim()
        }
    }
}
