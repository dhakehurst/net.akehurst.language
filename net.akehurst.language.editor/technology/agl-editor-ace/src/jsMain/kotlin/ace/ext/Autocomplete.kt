/*
 * Based on [https://github.com/daemontus/kotlin-ace-wrapper]
 */
package ace.ext

@JsModule("net.akehurst.language.editor-kotlin-ace-loader!?id=ace/autocomplete&name=Autocomplete")
@JsNonModule
external object Autocomplete {

    val startCommand: dynamic
}