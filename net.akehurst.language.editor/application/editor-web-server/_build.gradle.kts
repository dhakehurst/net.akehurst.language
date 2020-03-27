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
val version_ktor:String by project
dependencies {
    // need this so that the gradle application-plugin can find the module built by the kotlin-plugin
    runtime( project(path=":application-editor-web-server", configuration="jvm8RuntimeElements") )

    jvm8MainImplementation(project(":application-client-angular"))

    // ktor server modules
    jvm8MainImplementation("io.ktor:ktor-websockets:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-core:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-jetty:$version_ktor")

    // for logging
    jvm8MainImplementation("org.slf4j:slf4j-simple:1.7.29")
}
