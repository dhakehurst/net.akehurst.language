dependencies {
    commonMainApi(project(":agl-processor"))
    commonMainImplementation(project(":collections"))

    jvm8MainImplementation(kotlin("reflect"))
}