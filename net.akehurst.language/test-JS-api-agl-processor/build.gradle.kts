plugins {
    id("project-conventions")
    alias(libs.plugins.jsIntegration)
}

dependencies {
    jsTestImplementation(project(":agl-processor"))
}

kotlin {
    js {
        binaries.library()
        compilerOptions {
            target.set("es2015")
        }
        nodejs()
        browser {
            testTask {
                useMocha {
                    timeout = "5000"
                }
            }
        }
    }
}

val jsOutDir = project.layout.buildDirectory.dir("dist/rollup")

jsIntegration {
    nodeSrcDirectoryDev.set(project.layout.buildDirectory.dir("dist/js/developmentLibrary"))
    nodeOutDirectoryDev.set(jsOutDir)

    nodeSrcDirectoryProd.set(project.layout.buildDirectory.dir("dist/js/productionLibrary"))
    nodeOutDirectoryProd.set(jsOutDir)

    productionCommand.set(mapOf("prod" to "run build"))
    developmentCommand.set(mapOf("dev" to "run build"))
}

//tasks.named<Copy>("jsTestTestDevelopmentExecutableCompileSync") {
//    duplicatesStrategy = DuplicatesStrategy.WARN
//}
tasks.register<Copy>("overwriteTestEntryFile") {
    mustRunAfter("compileTestDevelopmentExecutableKotlinJs")
    from("src/test/javascript")
    into(File(rootProject.projectDir, ".gradle-build/net.akehurst.language/js/packages/net.akehurst.language-test-JS-api-agl-processor-test/kotlin"))
}
tasks["jsTestTestDevelopmentExecutableCompileSync"].mustRunAfter("overwriteTestEntryFile")
tasks["jsNodeTest"].dependsOn("overwriteTestEntryFile")
tasks["jsBrowserTest"].dependsOn("overwriteTestEntryFile")

tasks.withType<AbstractPublishToMaven> {
    onlyIf { false }
}