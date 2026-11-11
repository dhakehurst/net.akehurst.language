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
                // inflate (raw deflate) on every platform; java.util.zip.Inflater underneath on the JVM
                implementation(libs.korlibs.compression)
                implementation(libs.korlibs.io.stream)
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
