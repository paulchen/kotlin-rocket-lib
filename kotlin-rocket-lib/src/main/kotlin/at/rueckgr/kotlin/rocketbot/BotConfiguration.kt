package at.rueckgr.kotlin.rocketbot

data class BotConfiguration(
    val host: String,
    val username: String,
    val password: String,
    val ignoredChannels: List<String>,
    val botId: String
)
