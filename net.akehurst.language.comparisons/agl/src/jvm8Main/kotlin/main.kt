package net.akehurst.language.comparisons.agl

actual suspend fun main() {
    val jh = System.getProperty("java.home")
    println("******************************")
    println("JVM: $jh")
    println("******************************")

    runTests()
}