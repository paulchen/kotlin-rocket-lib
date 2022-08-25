package at.rueckgr.kotlin.rocketbot.util

import kotlinx.coroutines.delay

class ReconnectWaitService : Logging {
    private var waitingTime = -1

    companion object {
        val instance = ReconnectWaitService()
    }

    fun resetWaitingTime() {
        this.waitingTime = -1
    }

    suspend fun wait() {
        val waitingTime = this.getWaitingTime()
        logger().info("Waiting $waitingTime seconds")

        delay(waitingTime * 1000L)
    }

    private fun getWaitingTime(): Int {
        val waitingTimes = listOf(5, 10, 30)

        val newWaitingTime = if (waitingTime == waitingTimes.last()) {
            waitingTimes.last()
        }
        else {
            waitingTimes[waitingTimes.indexOf(waitingTime) + 1]
        }

        this.waitingTime = newWaitingTime
        return newWaitingTime
    }
}
