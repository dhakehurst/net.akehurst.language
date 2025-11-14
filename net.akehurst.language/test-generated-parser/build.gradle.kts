plugins {
    id("project-conventions")
}

dependencies {
    "commonMainImplementation"(project(":agl-processor"))
    "commonTestImplementation"("junit:junit:4.13.2")
}


tasks.withType<AbstractPublishToMaven> {
    onlyIf { false }
}