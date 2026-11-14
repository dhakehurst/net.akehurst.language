plugins {
    id("project-conventions")
    alias(libs.plugins.exportPublic)
    alias(libs.plugins.kotlinx.serialization)
//    alias(libs.plugins.reflex)
}


kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":agl-datastore-api"))
                api(project(":agl-processor"))
                api(libs.kotlinx.coroutines.core)
                // SecretSource and its registry, for secrets referenced by configurations
                api(libs.nak.kotlinx.secure.storage)
                // saving and loading configurations as JSON
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.nak.kotlinx.utils)
            }
        }
    }
}