plugins {

}

dependencies {
    "commonMainImplementation"(project(":agl-processor"))
    "commonTestImplementation"("junit:junit:4.13.2")
}


tasks.withType<PublishToMavenLocal> {
    onlyIf { false }
}