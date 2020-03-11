val version_agl:String by project
dependencies {

    jsMainImplementation(npm("monaco-editor", "0.19.3"))
    jsMainImplementation("net.akehurst.language:agl-processor:$version_agl")

    // for webpack
    jsMainImplementation(npm("monaco-editor-webpack-plugin", "1.8.2"))
    jsMainImplementation(npm("css-loader", "3.4.2"))
    jsMainImplementation(npm("style-loader", "1.1.3"))
    jsMainImplementation(npm("ts-loader", "6.2.1"))
    jsMainImplementation(npm("file-loader", "^5.0.2"))
}
