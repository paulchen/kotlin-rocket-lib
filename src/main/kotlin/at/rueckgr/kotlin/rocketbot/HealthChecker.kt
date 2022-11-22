package at.rueckgr.kotlin.rocketbot

interface HealthChecker {
    fun performHealthCheck(): List<HealthProblem>

    fun getAdditionalStatusInformation(): Map<String, Map<String, String>>
}

data class HealthProblem(val category: String, val subcategory: String, val description: String)
