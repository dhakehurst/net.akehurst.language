plugins {
    id("project-conventions")
}


// do not publish, yet
tasks.withType<AbstractPublishToMaven> { onlyIf { false } }

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":agl-processor"))
                implementation(libs.nak.kotlinx.collections)
                implementation(project(":kotlinx-komposite"))
            }
        }
        jvmMain {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
    }
}
