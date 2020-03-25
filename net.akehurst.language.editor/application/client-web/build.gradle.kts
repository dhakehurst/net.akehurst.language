import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

val worker by configurations.creating {
    extendsFrom(configurations.jsMainImplementation.get())
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_RUNTIME))
    }
}

dependencies {

    //jsMainImplementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.1")
    jsMainImplementation(project(":information-editor"))
    jsMainImplementation(project(":technology-gui-widgets"))
    jsMainImplementation(project(":technology-agl-editor-ace"))
    jsMainImplementation(project(":technology-agl-editor-monaco"))

    //result of this must be copied into resources
    jsMainImplementation(project(":technology-agl-editor-worker"))
    //worker(project(":technology-agl-editor-worker"))

}


val workerTask = tasks.register<Copy>("copyAglEditorWorkerJs") {
    dependsOn(":technology-agl-editor-worker:jsBrowserDevelopmentWebpack")
    dependsOn("jsProcessResources")
        from("$buildDir/../technology-agl-editor-worker/distributions") {
            include("technology-agl-editor-worker.js")
        }
        into(file("$buildDir/processedResources/js/main"))

}

tasks.getByName("jsBrowserDevelopmentWebpack").dependsOn(workerTask)
tasks.getByName("jsBrowserDevelopmentRun").dependsOn(workerTask)
