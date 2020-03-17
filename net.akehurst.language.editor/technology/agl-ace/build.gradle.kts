
val version_agl:String by project
val version_ace:String = "1.4.8"

dependencies {

    jsMainApi(project(":technology-agl-editor-common"))

    jsMainImplementation(npm("ace-builds", version_ace))

    //jsMainImplementation(npm("kotlin-ace-loader", "1.0.3"))
    //jsMainImplementation(project(":technology-kotlin-ace-loader"))
    jsMainImplementation(npm("net.akehurst.language.editor-kotlin-ace-loader","https://nexus-intern.itemis.de/nexus/repository/akehurst-npm/net.akehurst.language.editor-kotlin-ace-loader/-/net.akehurst.language.editor-kotlin-ace-loader-1.0.4.tgz"))
}

tasks.withType<ProcessResources>  {
    filesMatching("**/package.json") {
        expand(project.properties)
    }
}