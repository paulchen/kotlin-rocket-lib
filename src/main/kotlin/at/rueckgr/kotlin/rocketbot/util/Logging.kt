package at.rueckgr.kotlin.rocketbot.util

import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger
import kotlin.reflect.full.companionObject

// https://www.baeldung.com/kotlin-logging
interface Logging

fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
    return javaClass.enclosingClass?.takeIf {
        it.kotlin.companionObject?.java == javaClass
    } ?: javaClass
}

inline fun <reified T : Logging> T.logger(): Logger
        = getLogger(getClassForLogging(T::class.java))

fun Logging.logExceptions(lambda: Logging.() -> Any) {
    try {
        lambda.invoke(this)
    }
    catch (e: Throwable) {
        logger().error("Exception occurred", e)
    }
}

fun Logging.handleExceptions(function: () -> Unit) {
    try {
        function.invoke()
    }
    catch (e: Throwable) {
        logger().error("Exception occurred: ", e)
    }
}

