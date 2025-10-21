plugins {
    id("project-conventions")
}

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":agl-processor"))
                implementation("junit:junit:4.13.2")
            }
        }
    }
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.withType<AbstractPublishToMaven> {
    onlyIf { false }
}