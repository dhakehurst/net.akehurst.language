dependencies {

    //jsMainImplementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.1")
    jsMainImplementation(project(":information-editor"))
    jsMainImplementation(project(":technology-gui-widgets"))
    jsMainImplementation(project(":technology-agl-editor-ace"))
    jsMainImplementation(project(":technology-agl-editor-monaco"))

    //result of this must be copied into resources
    jsMainImplementation(project(":technology-agl-editor-worker"))

}

tasks.register<Copy>("copyAglEditorWorkerJs") {
    dependsOn("jsProcessResources")
    from(file("$buildDir/../technology-agl-editor-worker/distributions/technology-agl-editor-worker.js"))
    into(file("$buildDir/processedResources/js/main/"))
}
