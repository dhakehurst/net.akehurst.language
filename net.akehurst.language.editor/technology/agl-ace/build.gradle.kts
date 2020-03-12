val version_agl:String by project
val version_ace:String = "1.4.8"
dependencies {

    jsMainImplementation("net.akehurst.language:agl-processor:$version_agl")
    jsMainImplementation(npm("ace-builds", version_ace))
}
