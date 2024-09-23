plugins {
    alias(libs.plugins.jsIntegration)
}

dependencies {
    jsTestImplementation(project(":agl-processor"))
}

configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    js("js", IR) {
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target.set("es2015")
        }
        nodejs()
        browser()
    }
}

val jsSrcDir = project.layout.buildDirectory.dir("dist/js/developmentLibrary")
val jsOutDir = project.layout.buildDirectory.dir("dist/rollup")

jsIntegration {
    nodeSrcDirectory.set(jsSrcDir)
    nodeOutDirectory.set(jsOutDir)

    productionCommand.set(listOf("run build"))
    developmentCommand.set(listOf("run build"))
}




//tasks.named<Copy>("jsTestTestDevelopmentExecutableCompileSync") {
//    duplicatesStrategy = DuplicatesStrategy.WARN
//}
tasks.create<Copy>("overwriteTestEntryFile") {
    mustRunAfter("compileTestDevelopmentExecutableKotlinJs")
    from("src/test/javascript")
    into(File(rootProject.projectDir, ".gradle-build/net.akehurst.language/js/packages/net.akehurst.language-test-JS-api-agl-processor-test/kotlin"))
}
tasks["jsNodeTest"].dependsOn("overwriteTestEntryFile")
tasks["jsBrowserTest"].dependsOn("overwriteTestEntryFile")

tasks.withType<PublishToMavenLocal> {
    onlyIf { false }
}