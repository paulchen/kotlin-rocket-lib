package at.rueckgr.kotlin.rocketbot

interface WebserviceUserValidator {
    fun validate(username: String, password: String): Boolean
}
