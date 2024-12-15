package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.BotConfiguration
import java.time.LocalDateTime
import java.time.ZoneOffset

class BotDetectionService(private val botConfiguration: BotConfiguration) : Logging {
    companion object {
        private var cache: List<String> = emptyList()
        private var lastCacheUpdate: LocalDateTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
    }

    fun isBot(userId: String) = getCache().contains(userId)

    private fun getCache(): List<String> {
        checkCache()
        return cache.toList()
    }

    private fun checkCache() {
        val oneDayAgo = LocalDateTime.now().minusDays(1)
        if (lastCacheUpdate.isBefore(oneDayAgo)) {
            synchronized(this) {
                if (lastCacheUpdate.isBefore(oneDayAgo)) {
                    logger().debug("Last update of cache ({}) is too long ago, updating cache now", lastCacheUpdate)
                    updateCache()
                }
            }
        }
    }

    private fun updateCache() {
        cache = RestApiClient(botConfiguration.host).getUsersByRole("bot").map { it.id }.toList()
        lastCacheUpdate = LocalDateTime.now()
    }
}
