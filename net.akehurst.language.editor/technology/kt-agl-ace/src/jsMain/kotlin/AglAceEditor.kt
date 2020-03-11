package net.akehurst.language.editor.ace

import org.w3c.dom.HTMLElement

class AglAceEditor(
        val element: HTMLElement,
        val editorId: String,
        val languageId: String,
        val goalRule:String,
        initialText:String,
        options:Any
) {

    private val errorParseMarkerIds = mutableListOf<String>()
    private val errorProcessMarkerIds = mutableListOf<String>()
    //private val mode: ace.SyntaxMode

    val aceEditor: ace.Editor = ace.Editor(
            ace.VirtualRenderer(this.element, null),
            ace.Ace.createEditSession(initialText),
            options
    )

    init {
        //this.aceEditor =
    }

}