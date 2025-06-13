plugins {

}

dependencies {
    jvm8TestImplementation(project(":agl-processor"))
    jvm8TestImplementation("junit:junit:4.13.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.withType<AbstractPublishToMaven> {
    onlyIf { false }
}