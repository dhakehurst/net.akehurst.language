dependencies {
    "jsTestImplementation"(project(":agl-processor"))
}

configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    //sourceSets{
    //    val jsTest by getting {
    //        resources.srcDir("src/test/javascript")
    //    }
    //}
    js("js", IR) {

        nodejs {

            testTask {
                //this.testFrameworkSettings
                //inputFileProperty.set(file("src/test/javascript/test.js"))
            }
        }
        browser {
            testTask {
                //inputFileProperty.set(file("src/test/javascript/test.js"))
            }
        }
    }
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