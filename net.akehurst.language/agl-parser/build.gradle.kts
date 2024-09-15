
dependencies {
    commonMainImplementation(project(":agl-regex"))
    commonMainImplementation(project(":collections"))
}

exportPublic {
    exportPatterns.set(
        listOf(
            "net.akehurst.language.parser.api",
            "net.akehurst.language.parser.runtime"
        )
    )
}