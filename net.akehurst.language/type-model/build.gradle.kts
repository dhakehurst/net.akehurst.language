plugins {

}

kotlin {
    js("js") {
        binaries.library()
        compilations["main"].packageJson {
            customField(
                "author", mapOf(
                    "name" to "Dr. David H. Akehurst",
                    "email" to "dr.david.h@akehurst.net",
                    "url" to "https://medium.com/@dr.david.h.akehurst"
                )
            )
            customField("license", "Apache-2.0")
            customField("keywords", listOf("types", "type model", "type definition", "OO"))
            customField("homepage", "https://github.com/dhakehurst/net.akehurst.language")
            customField("description:", "For defining types")
        }
    }

//    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
//    wasm("wasm") {
//        binaries.library()
//        browser()
//    }
}

exportPublic {
    exportPatterns.set(
        listOf(
            "net.akehurst.language.typemodel.api.**",
            "net.akehurst.language.typemodel.simple.**",
        )
    )
}

