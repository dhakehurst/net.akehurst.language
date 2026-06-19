plugins {
    id("project-conventions")
}

// do not publish
tasks.withType<AbstractPublishToMaven> { onlyIf { false } }

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
