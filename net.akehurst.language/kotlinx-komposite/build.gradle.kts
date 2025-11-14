plugins {
    id("project-conventions")
}

dependencies {
    commonMainImplementation(project(":agl-processor"))
    commonMainImplementation(libs.nak.kotlinx.reflect)
}