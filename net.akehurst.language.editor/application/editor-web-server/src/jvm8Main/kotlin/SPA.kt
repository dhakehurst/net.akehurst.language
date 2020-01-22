package net.akehurst.language.editor.web.server

import io.ktor.application.*
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.request.acceptItems
import io.ktor.request.uri
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The SPA configuration class.
 * @param configuration The object configured by the install lambda.
 */
class SinglePageApplication(private val configuration: Configuration) {

    companion object Feature :
            ApplicationFeature<Application, SinglePageApplication.Configuration, SinglePageApplication> {

        override val key = AttributeKey<SinglePageApplication>("SinglePageApplication")

        override fun install(
                pipeline: Application,
                configure: Configuration.() -> Unit
        ): SinglePageApplication {

            val feature = SinglePageApplication(Configuration().apply(configure))

            pipeline.routing {
                static(feature.configuration.spaRoute) {
                    default(feature.configuration.defaultPage)
                    if (feature.configuration.useFiles)
                        files(feature.configuration.folderPath)
                    else
                        resources(feature.configuration.folderPath)
                }
            }

            pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
                feature.intercept(this, message)
            }

            return feature
        }

    }

    private suspend fun intercept(
            pipelineContext: PipelineContext<Any, ApplicationCall>,
            message: Any
    ) = pipelineContext.apply {

        val requestUrl = call.request.uri
        val regex = configuration.ignoreIfContains
        val stop by lazy {
            !((regex == null || requestUrl.notContains(regex))
                    && (requestUrl.startsWith(configuration.spaRoute)
                    || requestUrl.startsWith("/${configuration.spaRoute}")))
        }
        val is404 by lazy {
            if (message is HttpStatusCodeContent)
                message.status == HttpStatusCode.NotFound
            else
                false
        }
        val acceptsHtml by lazy {
            call.request.acceptItems().any {
                ContentType.Text.Html.match(it.value)
            }
        }

        if (call.attributes.contains(StatusPages.key) || stop || !is404 || !acceptsHtml)
            return@apply

        call.attributes.put(key, this@SinglePageApplication)

        if (configuration.useFiles) {
            val file = configuration.fullPath().toFile()
            if (file.notExists()) throw FileNotFoundException("${configuration.fullPath()} not found")
            call.respondFile(File(configuration.folderPath), configuration.defaultPage)
        } else {
            val indexPageApplication = call.resolveResource(configuration.fullPath().toString())
                    ?: throw FileNotFoundException("${configuration.fullPath()} not found")
            call.respond(indexPageApplication)
        }
        finish()
    }

    data class Configuration(
            var spaRoute: String = "",
            var useFiles: Boolean = false,
            var folderPath: String = "",
            var defaultPage: String = "index.html",
            var ignoreIfContains: Regex? = null
    ) {
        fun fullPath() = Paths.get(folderPath, defaultPage)!!
    }

}

fun String.notContains(regex: Regex) = !contains(regex)
fun File.notExists() = !exists()
