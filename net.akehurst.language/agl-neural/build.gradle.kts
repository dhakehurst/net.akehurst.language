kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.korlibs.korio)
            }
        }
    }
}


tasks.withType<AbstractPublishToMaven> {
    onlyIf { false }
}