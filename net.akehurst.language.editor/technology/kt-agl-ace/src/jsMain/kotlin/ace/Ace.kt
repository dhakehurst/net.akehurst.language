/*
 * Based on [https://github.com/daemontus/kotlin-ace-wrapper]
 */
package ace

@JsModule("ace-builds")
external object Ace {

    fun createEditSession(text: String): EditSession


}