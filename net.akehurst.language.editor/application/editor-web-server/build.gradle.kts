import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    application
}

// so that the application plugin can find the jars from the kotlin-plugin jvm configuration
val runtimeClasspath by configurations.getting {
    attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
}
application {
    mainClassName = "net.akehurst.language.editor.web.server.MainKt"
}

val version_kaf:String by project
dependencies {
    // need this so that the gradle application-plugin can find the module built by the kotlin-plugin
    runtime( project(path=":application-editor-web-server", configuration="jvm8RuntimeElements") )

    jvm8MainImplementation(project(":application-client-angular"))

    // --- framework ---
    jvm8MainImplementation("net.akehurst.kaf:kaf-common-realisation:$version_kaf")

    // --- services
    commonMainImplementation("net.akehurst.kaf:kaf-service-logging-api:$version_kaf")
    jvm8MainImplementation("net.akehurst.kaf:kaf-service-logging-log4j2:$version_kaf")
    commonMainImplementation("net.akehurst.kaf:kaf-service-configuration-persistence-fs-korio-hjson:$version_kaf")
    jvm8MainImplementation("net.akehurst.kaf:kaf-service-commandLineHandler-kaf:$version_kaf")

    // -- information ---
    commonMainImplementation(project(":information-editor"))

    // --- technology
    jvm8MainImplementation("net.akehurst.kaf:kaf-technology-webserver-ktor:$version_kaf")
}
