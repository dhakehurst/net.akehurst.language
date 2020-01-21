plugins {
    id("org.openjfx.javafxplugin") version "0.0.8"
}

val version_coroutines:String by project
val version_kaf:String by project
val version_javafx: String = "11.0.2"
val version_tornado: String = "1.7.19"
val version_agl:String = "3.3.2"
dependencies {

    commonMainImplementation( "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$version_coroutines")

    // --- framework ---
    jvm8MainImplementation("net.akehurst.kaf:kaf-common-realisation:$version_kaf")


    // ---
    jvm8MainImplementation("no.tornado:tornadofx:$version_tornado")
    jvm8MainImplementation("org.fxmisc.richtext:richtextfx:0.10.3")
    jvm8MainImplementation("net.akehurst.language:agl-processor:$version_agl")

    // For Test

    // --- services
    commonTestImplementation("net.akehurst.kaf:kaf-service-logging-api:$version_kaf")
    jvm8TestImplementation("net.akehurst.kaf:kaf-service-logging-console:$version_kaf")
    commonTestImplementation("net.akehurst.kaf:kaf-service-configuration-map:$version_kaf")
    jvm8TestImplementation("net.akehurst.kaf:kaf-service-commandLineHandler-simple:$version_kaf")

}

javafx {
    configuration = "jvm8Implementation"
    //version = "8"
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}