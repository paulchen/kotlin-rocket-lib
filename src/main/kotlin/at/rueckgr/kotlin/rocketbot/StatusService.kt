package at.rueckgr.kotlin.rocketbot

import at.rueckgr.kotlin.rocketbot.handler.message.PingMessageHandler
import java.time.LocalDateTime

class StatusService {
    private val warningSeconds = 60L
    private val criticalSeconds = 120L

    var healthChecker: HealthChecker? = null

    fun getStatus(): Status {
        val problems = healthChecker!!.performHealthCheck()
        val status = if (problems.isNotEmpty() || LocalDateTime.now().minusSeconds(criticalSeconds).isAfter(
                PingMessageHandler.lastPing
            )) {
            BotStatus.CRITICAL
        } else if (LocalDateTime.now().minusSeconds(warningSeconds).isAfter(PingMessageHandler.lastPing)) {
            BotStatus.WARNING
        } else {
            BotStatus.OK
        }
        val additionalStatusInformation = healthChecker!!.getAdditionalStatusInformation()

        return Status(status, PingMessageHandler.lastPing, problems, additionalStatusInformation)
    }

    data class Status(
        val status: BotStatus,
        val lastPing: LocalDateTime,
        val problems: List<HealthProblem>,
        val additionalStatusInformation: Map<String, Map<String, String>>
    )

    enum class BotStatus {
        CRITICAL,
        WARNING,
        OK
    }
}
