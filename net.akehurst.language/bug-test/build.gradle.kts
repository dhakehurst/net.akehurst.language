plugins {
    id("project-conventions")
}

// do not publish
tasks.withType<AbstractPublishToMaven> { onlyIf { false } }

dependencies {

}

kotlin {

    //@OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    // wasmJs {
    //    binaries.library()
    //     browser()
    // }

    sourceSets {
        commonTest.configure {
            //      resources.srcDir(projectDir.resolve("../language-repository/languages"))
        }
    }
}

