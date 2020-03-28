import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput


val version_agl_editor:String by project

dependencies {
    jsMainImplementation("net.akehurst.language.editor:technology-agl-editor-worker:$version_agl_editor")
}

kotlin {
    js("js") {
        nodejs()
        browser {
            webpackTask {
                output.libraryTarget = KotlinWebpackOutput.Target.SELF
            }
        }
    }
}
