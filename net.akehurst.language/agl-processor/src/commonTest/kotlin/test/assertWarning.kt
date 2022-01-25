package test

import kotlin.test.fail

internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

fun <T> assertEqualsWarning(expected: T, actual: T, message: String? = null) {
    assertTrueWarning({ messagePrefix(message) + "Expected <$expected>, actual <$actual>." }, actual == expected)
}

fun assertTrueWarning(lazyMessage: () -> String?, actual: Boolean): Unit {
    if (!actual) {
        failWarn(lazyMessage())
    }
}

fun failWarn(message: String?) {
    println("WARN: $message")
//    fail(message)
}