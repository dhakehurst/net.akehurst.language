plugins {
    id("project-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.korlibs.korio)
            }
        }
    }
}

// do not publish, yet
tasks.withType<AbstractPublishToMaven> { onlyIf { false } }