plugins {

}

dependencies {
    commonTestImplementation(project(":agl-processor"))
//    commonTestImplementation("net.akehurst.language:agl-processor:4.0.1")

    commonTestImplementation("junit:junit:4.13.2")
}

kotlin {
    jvm("jvm8") {
    }
}

tasks.withType<PublishToMavenLocal> {
    onlyIf { false }
}