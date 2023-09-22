plugins {

}

// do not publish
tasks.withType<PublishToMavenLocal> { onlyIf { false } }

dependencies {

}

kotlin {

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasm("wasm") {
        binaries.library()
        browser()
    }

    sourceSets {
        commonTest.configure {
            // add language repository so we can test the grammars with specific sentences here
            resources.srcDir(projectDir.resolve("../language-repository/languages"))
        }
    }
}

