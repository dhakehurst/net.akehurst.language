plugins {
    id("project-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.nak.kotlinx.collections) // for MutableStack
            }
        }
    }
}
