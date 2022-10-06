package at.rueckgr.kotlin.rocketbot.util

import kotlinx.serialization.Serializable

@Serializable
class VersionInfo(val revision: String, val commitMessage: String)
