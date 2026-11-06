plugins {
    id("project-conventions")
    alias(libs.plugins.exportPublic)
//    alias(libs.plugins.reflex)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":agl-datastore-common"))
            }
        }
    }
}