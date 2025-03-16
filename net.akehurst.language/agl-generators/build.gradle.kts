dependencies {
    commonMainApi(project(":agl-processor"))
    commonMainImplementation(project(":collections"))
    commonMainImplementation(project(":kotlinx-komposite"))

    jvm8MainImplementation(kotlin("reflect"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    }
}