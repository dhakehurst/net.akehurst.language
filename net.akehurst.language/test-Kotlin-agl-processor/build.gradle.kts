plugins {
    id("project-conventions")
}

dependencies {

}

kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":agl-processor"))
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

tasks.withType<AbstractPublishToMaven> {
    onlyIf { false }
}