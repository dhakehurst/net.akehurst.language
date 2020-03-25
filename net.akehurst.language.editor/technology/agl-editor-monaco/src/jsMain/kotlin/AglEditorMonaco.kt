/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.editor.monaco

import kotlinx.coroutines.Job
import monaco.MarkerSeverity
import monaco.editor
import monaco.editor.IStandaloneCodeEditor
import monaco.languages
import net.akehurst.language.api.analyser.SyntaxAnalyserException
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.editor.api.*
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.editor.common.AglComponents
import net.akehurst.language.editor.common.AglEditorAbstract
import net.akehurst.language.editor.comon.AglWorkerClient
import net.akehurst.language.processor.Agl
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.asList
import kotlin.browser.document

class AglEditorMonaco(
        val element: Element,
        editorId: String,
        val languageId: String,
        goalRule: String? = null,
        options: dynamic //TODO: types for this
) : AglEditorAbstract(editorId) {

    companion object {
        private val init = js("""
            var self = {MonacoEnvironment: {}};
            self.MonacoEnvironment = {
                getWorkerUrl: function(moduleId, label) {
                    return './main.js';
                }
            }
        """)

        // https://github.com/Microsoft/monaco-editor/issues/338
        // all editors on the same page must share the same theme!
        // hence we create a global theme and modify it as needed.
        private val aglGlobalTheme = "agl-theme"
        val allAglGlobalThemeRules = mutableMapOf<String, editor.ITokenThemeRule>()

        fun initialise(document: Document, tag: String = "agl-editor"): Map<String, AglEditorMonaco> {
            val map = mutableMapOf<String, AglEditorMonaco>()
            document.querySelectorAll(tag).asList().forEach { el ->
                val element = el as Element
                //delete any current children of element
                while (element.childElementCount != 0) {
                    element.removeChild(element.firstChild!!)
                }
                val id = element.getAttribute("id")!!
                val editor = AglEditorMonaco(element, id, id, null, null)
                map[id] = editor
            }
            return map
        }
    }

    lateinit var monacoEditor: IStandaloneCodeEditor
    val languageThemePrefix = this.languageId + "-"

    override var text: String
        get() {
            try {
                return this.monacoEditor.getModel().getValue()
            } catch (t: Throwable) {
                throw RuntimeException("Failed to get text from editor")
            }
        }
        set(value) {
            try {
                this.monacoEditor.getModel().setValue(value)
            } catch (t: Throwable) {
                throw RuntimeException("Failed to set text in editor")
            }
        }

    val _tokenProvider = AglTokenProvider(this.languageThemePrefix, this.agl)
    var aglWorker = AglWorkerClient()
    lateinit var workerTokenizer: AglTokenizerByWorkerMonaco
    var parseTimeout: dynamic = null

    init {
        try {
            this.workerTokenizer = AglTokenizerByWorkerMonaco(this.agl)

            val themeData = js("""
                {
                    base: 'vs',
                    inherit: false,
                    rules: []
                }
            """)
            // https://github.com/Microsoft/monaco-editor/issues/338
            // all editors on the same page must share the same theme!
            // hence we create a global theme and modify it as needed.
            monaco.editor.defineTheme(aglGlobalTheme, themeData);

            monaco.languages.register(object : languages.ILanguageExtensionPoint {
                override val id = languageId
            })
            this.agl.goalRule = goalRule
            val languageId = this.languageId
            val initialContent = ""
            val theme = aglGlobalTheme
            val editorOptions = js("{language: languageId, value: initialContent, theme: theme, wordBasedSuggestions:false}")
            this.monacoEditor = monaco.editor.create(this.element, editorOptions, null)
            monaco.languages.setTokensProvider(this.languageId, this._tokenProvider);
            languages.registerCompletionItemProvider(this.languageId, AglCompletionProvider(this.agl))

            this.onChange {
                this.doBackgroundTryParse()
            }

            val self = this
            val resizeObserver: dynamic = js("new ResizeObserver(function(entries) { self.onResize(entries) })")
            resizeObserver.observe(this.element)

            this.aglWorker.initialise()
            this.aglWorker.processorCreateSuccess = this::processorCreateSuccess
            this.aglWorker.parseSuccess = this::parseSuccess
            this.aglWorker.parseFailure = this::parseFailure
            this.aglWorker.lineTokens = {
                this.workerTokenizer.receiveTokens(it)
                this.resetTokenization()
            }

        } catch (t: Throwable) {
            console.error(t.message)
        }
    }

    override fun finalize() {
        this.aglWorker.worker.terminate()
    }

    override fun setStyle(css: String?) {
        if (null != css && css.isNotEmpty()) {
            // https://github.com/Microsoft/monaco-editor/issues/338
            // all editors on the same page must share the same theme!
            // hence we create a global theme and modify it as needed.
            val rules: List<AglStyleRule> = Agl.styleProcessor.process(css)
            rules.forEach {
                val key = this.languageThemePrefix + it.selector;
                val value = object : editor.ITokenThemeRule {
                    override val token = key
                    override val foreground = convertColor(it.getStyle("foreground")?.value)
                    override val background = convertColor(it.getStyle("background")?.value)
                    override val fontStyle = it.getStyle("font-style")?.value
                }
                allAglGlobalThemeRules.set(key, value);
            }
            // reset the theme with the new rules
            monaco.editor.defineTheme(aglGlobalTheme, object : editor.IStandaloneThemeData {
                override val base = "vs"
                override val inherit = false
                override val rules = allAglGlobalThemeRules.values.toTypedArray()
            })
        }
    }

    override fun setProcessor(grammarStr: String?) {
        this.aglWorker.createProcessor(grammarStr)
        if (null == grammarStr || grammarStr.trim().isEmpty()) {
            this.agl.processor = null
        } else {
            try {
                this.agl.processor = Agl.processor(grammarStr)
            } catch (t: Throwable) {
                this.agl.processor = null
                console.error(t.message)
            }
        }
        this.workerTokenizer.reset()
        this.resetTokenization() //new processor so find new tokens
        this.workerTokenizer.acceptingTokens = true
        this.doBackgroundTryParse()
    }

    override fun clearErrorMarkers() {
        monaco.editor.setModelMarkers(this.monacoEditor.getModel(), "", emptyArray())
    }

    fun onChange(handler: (String) -> Unit) {
        this.monacoEditor.onDidChangeModelContent { event ->
            val text = this.text
            handler(text);
        }
    }

    fun resetTokenization() {
        this.monacoEditor.getModel().resetTokenization()
    }

    @JsName("onResize")
    private fun onResize(entries: Array<dynamic>) {
        entries.forEach { entry ->
            if (entry.target == this.element) {
                this.monacoEditor.layout()
            }
        }
    }

    fun doBackgroundTryParse() {
        this.clearErrorMarkers()
        this.aglWorker.interrupt()
        this.aglWorker.tryParse(this.text)
    }

    fun doBackgroundTryProcess() {
        tryProcess()
    }

    private fun tryParse() {
        val proc = this.agl.processor
        if (null != proc) {
            try {

                val goalRule = this.agl.goalRule
                if (null == goalRule) {
                    this.agl.sppt = proc.parse(this.text)
                } else {
                    this.agl.sppt = proc.parse(goalRule, this.text)
                }
                this.resetTokenization()
                val event = ParseEvent(true, "OK")
                this.notifyParse(event)
                this.doBackgroundTryProcess()
            } catch (e: ParseFailedException) {
                this.agl.sppt = null
                // parse failed so re-tokenize from scan
                this.resetTokenization()
                console.error("Error parsing text in " + this.editorId + " for language " + this.languageId, e.message);
                val errors = mutableListOf<editor.IMarkerData>()
                errors.add(object : editor.IMarkerData {
                    override val code: String? = null
                    override val severity = MarkerSeverity.Error
                    override val startLineNumber = e.location.line
                    override val startColumn = e.location.column
                    override val endLineNumber: Int = e.location.line
                    override val endColumn: Int = e.location.column
                    override val message = e.message!!
                    override val source: String? = null
                })
                monaco.editor.setModelMarkers(this.monacoEditor.getModel(), "", errors.toTypedArray())
                val event = ParseEvent(false, e.message!!)
                this.notifyParse(event)
            } catch (t: Throwable) {
                console.error("Error parsing text in " + this.editorId + " for language " + this.languageId, t.message);
            }
        }
    }

    private fun tryProcess() {
        val proc = this.agl.processor
        val sppt = this.agl.sppt
        if (null != proc && null != sppt) {
            try {
                this.agl.asm = proc.process(sppt)
                val event = ProcessEvent(true, "OK")
                this.notifyProcess(event)
            } catch (e: SyntaxAnalyserException) {
                this.agl.asm = null


                val event = ProcessEvent(false, e.message!!)
                this.notifyProcess(event)
            } catch (t: Throwable) {
                console.error("Error processing parse result in " + this.editorId + " for language " + this.languageId, t.message)
            }
        }
    }

    private fun convertColor(value: String?): String? {
        if (null == value) {
            return null
        } else {
            val cvs = document.createElement("canvas")
            val ctx = cvs.asDynamic().getContext("2d")
            ctx.fillStyle = value
            val col = ctx.fillStyle as String
            return col.substring(1) //leave out the #
        }
    }

    private fun setupCommands() {

    }

    private fun processorCreateSuccess() {
        this.resetTokenization()
    }

    private fun parseSuccess() {
        this.resetTokenization()
        val event = ParseEvent(true, "OK")
        this.notifyParse(event)
        this.doBackgroundTryProcess()
    }

    private fun parseFailure(message: String, location: InputLocation?) {
        console.error("Error parsing text in " + this.editorId + " for language " + this.languageId, message);

        if (null != location) {
            // parse failed so re-tokenize from scan
            this.resetTokenization()
            console.error("Error parsing text in " + this.editorId + " for language " + this.languageId, message);
            val errors = mutableListOf<editor.IMarkerData>()
            errors.add(object : editor.IMarkerData {
                override val code: String? = null
                override val severity = MarkerSeverity.Error
                override val startLineNumber = location.line
                override val startColumn = location.column
                override val endLineNumber: Int = location.line
                override val endColumn: Int = location.column
                override val message = message!!
                override val source: String? = null
            })
            monaco.editor.setModelMarkers(this.monacoEditor.getModel(), "", errors.toTypedArray())
            val event = ParseEvent(false, message!!)
            this.notifyParse(event)

        }
    }

}

