
val version_agl:String by project
val version_ace:String = "1.4.8"

dependencies {

    jsMainApi(project(":technology-agl-editor-common"))

    jsMainImplementation(npm("ace-builds", version_ace))

    jsMainImplementation(npm("kotlin-ace-loader", "1.0.3"))
}
