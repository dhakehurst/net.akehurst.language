plugins {
    id("project-conventions")
}


// do not publish, yet
tasks.withType<AbstractPublishToMaven> { onlyIf { false } }

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.korlibs.korio)
            }
        }
    }
}
