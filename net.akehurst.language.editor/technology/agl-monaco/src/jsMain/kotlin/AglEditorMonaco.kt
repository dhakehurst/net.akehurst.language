package net.akehurst.language.editor.monaco

import monaco.editor.IStandaloneCodeEditor
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.asList


class AglEditorMonaco(
        val element: Element,
        val languageId: String
) {

    companion object {
        private val init = js("""
            var self = {MonacoEnvironment: {}};
            self.MonacoEnvironment = {
                getWorkerUrl: function(moduleId, label) {
                    return './main.js';
                }
            }
        """)
        private val aglGlobalTheme = "agl-theme"

        fun initialise(document: Document) {
            document.querySelectorAll("agl-monaco").asList().forEach { el ->
                val element = el as Element
                val id = element.getAttribute("id")!!
                AglEditorMonaco(element, id)
            }
        }
    }

    lateinit var monacoEditor: IStandaloneCodeEditor
    val languageThemePrefix = this.languageId + "-"


    init {
        try {
            val themeData = js("""
                {
                    base: 'vs',
                    inherit: false,
                    rules: []
                }
            """)
            monaco.editor.defineTheme(aglGlobalTheme, themeData);

            val languageId = this.languageId
            val initialContent = ""
            val theme = aglGlobalTheme
            val editorOptions = js("{language: languageId, value: initialContent, theme: theme}")
            this.monacoEditor = monaco.editor.create(this.element, editorOptions, null)
        } catch (t: Throwable) {
            println(t.message)
        }
    }
}