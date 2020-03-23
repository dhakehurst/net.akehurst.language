/*
 * Based on [https://github.com/daemontus/kotlin-ace-wrapper]
 */
package ace

/**
 * https://ace.c9.io/#nav=api&api=edit_session
 */
@JsModule("net.akehurst.language.editor-kotlin-ace-loader!?id=ace/edit_session&name=EditSession")
@JsNonModule
external class EditSession {
    var bgTokenizer: BackgroundTokenizer

    fun clearAnnotations()

    fun removeMarker(id:Int)
    fun setAnnotations(errors: Any)
    fun getDocument(): dynamic
    fun addMarker(range: Range, cls: String, type: String): Int
}

@JsModule("net.akehurst.language.editor-kotlin-ace-loader!?id=ace/range&name=Range")
@JsNonModule
external class Range(
        startRow:Int,
        startColumn:Int,
        endRow:Int,
        endColumn:Int
) {

}