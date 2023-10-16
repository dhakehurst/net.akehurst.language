plugins {

}

dependencies {
    "jvm8TestImplementation"(project(":agl-processor"))
    "jvm8TestImplementation"("junit:junit:4.13.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}


kotlin {
    jvm("jvm8") {
        withJava()
    }
}

tasks.withType<PublishToMavenLocal> {
    onlyIf { false }
}