package at.rueckgr.kotlin.rocketbot

interface HealthChecker {
    fun performHealthCheck(): List<HealthProblem>
}

data class HealthProblem(val category: String, val subcategory: String, val description: String)
