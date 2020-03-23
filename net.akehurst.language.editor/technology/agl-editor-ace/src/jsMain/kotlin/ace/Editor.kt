/*
 * Based on [https://github.com/daemontus/kotlin-ace-wrapper]
 */
package ace

@JsModule("net.akehurst.language.editor-kotlin-ace-loader!?id=ace/editor&name=Editor")
@JsNonModule
external class Editor(
        renderer: VirtualRenderer,
        session: EditSession,
        options:Any?
)  {
    val commands: dynamic
    var completers: Array<dynamic> //TODO:
    val renderer: dynamic

    fun getValue(): String
    fun setValue(value: String, cursorPos: Int)
    fun getSession(): EditSession
    fun setOption(option: String, module: dynamic)
    fun on(eventName: String, function: (dynamic) -> Unit)
    fun resize(force: Boolean)
    fun getSelection(): dynamic //TODO:

}