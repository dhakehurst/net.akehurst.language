package net.akehurst.language.editor.desktop.jfx.tornado

import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.clikt.CommandLineHandlerClikt
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.hjson.ServiceConfigurationHJsonFile
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.log4j2.LoggingServiceLog4j2
import java.io.File

fun main(args: Array<String>) {
    println("PWD: " + File(".").absolutePath)
    val application = EditorApplication
    application.af.startBlocking(args.toList())
}

object EditorApplication  : Application {

    override val af = afApplication(this,"application") {
        defineService(LoggingService::class) { LoggingServiceLog4j2() }
        defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerClikt(commandLineArgs) }
        defineService(ConfigurationService::class) { ServiceConfigurationHJsonFile("configuration/application.configuration.hjson") }
        initialise = {
            // --- Computational <-> Engineering

            // --- Engineering <-> Technology

        }
        execute = {

        }
        finalise = {

        }
    }

}