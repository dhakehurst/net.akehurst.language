package net.akehurst.language.editor.gui.tornadofx

import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.map.ServiceConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import org.junit.Test


class test_GUI {


    val app = object : Application {

        val sut = GUI()

        override val af = afApplication(this, "testApp") {
            defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
            defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }
            defineService(ConfigurationService::class) { ServiceConfigurationMap(
                    mapOf()
            ) }

            execute = {

            }
        }
    }

    @Test
    fun run() {

        app.af.startBlocking(emptyList())

    }

}