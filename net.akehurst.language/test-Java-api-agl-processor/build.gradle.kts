plugins {

}

dependencies {
    "jvm8TestImplementation"(project(":agl-processor"))
    "jvm8TestImplementation"("junit:junit:4.13.2")
}

kotlin {
    jvm("jvm8") {
        withJava()
    }
}