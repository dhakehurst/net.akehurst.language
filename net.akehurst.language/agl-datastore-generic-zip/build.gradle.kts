plugins {
    id("project-conventions")
    alias(libs.plugins.exportPublic)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":agl-datastore-common"))
                implementation(libs.nak.kotlinx.utils)
                // zip archives are read by kotlinx-filesystem's ZipFileSystem
                implementation(libs.nak.kotlinx.filesystem)
            }
        }

        commonTest {
            dependencies {
                // only to write deflated test archives
                implementation(libs.korlibs.compression)
            }
        }

        jvmTest {
            dependencies {
                implementation(project(":agl-datastore-filesystem"))
                implementation(project(":agl-datastore-text"))
            }
        }
    }
}
