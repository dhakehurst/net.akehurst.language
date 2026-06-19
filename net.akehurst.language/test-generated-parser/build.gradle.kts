plugins {
    id("project-conventions")
}

// do not publish
tasks.withType<AbstractPublishToMaven> { onlyIf { false } }

dependencies {
    "commonMainImplementation"(project(":agl-processor"))
    "commonTestImplementation"("junit:junit:4.13.2")
}
