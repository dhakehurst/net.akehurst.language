package net.akehurst.language.editor.web.server


import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.generateNonce
import java.io.File

fun main(args: Array<String>) {
    println("PWD: " + File(".").absolutePath)
    val application = EditorApplication
    application.start()
}

object EditorApplication {

    val server = Server("0.0.0.0", 9999)

    fun start() {
        server.start()
    }

}

class Server(
        val host:String,
        val port:Int
) {

    fun start() {
        val server = embeddedServer(Jetty, port = port, host = host) {
            install(DefaultHeaders)
            install(CallLogging)
            install(Routing)
            install(Sessions) {
                cookie<String>("SESSION_ID")
            }
            intercept(ApplicationCallPipeline.Features) {
                call.sessions.set<String>(generateNonce())
            }
            install(SinglePageApplication) {
                defaultPage = "index.html"
                folderPath = "/dist"
                spaRoute = ""
                useFiles = false
            }
        }

        server.start(true)
    }


}