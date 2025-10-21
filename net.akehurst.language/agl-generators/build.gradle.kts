plugins {
    id("project-conventions")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":agl-processor"))
                implementation(project(":collections"))
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