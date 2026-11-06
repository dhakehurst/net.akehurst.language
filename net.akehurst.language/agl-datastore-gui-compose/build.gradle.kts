plugins {
    id("project-conventions")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

// A reusable Compose UI for editing datastore compositions. UI only: everything in commonMain,
// the host application supplies the behaviour (tests, types, saving and loading) through callbacks.
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":agl-datastore-common"))
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.ui)
                api(libs.compose.material3)
            }
        }
    }
}
