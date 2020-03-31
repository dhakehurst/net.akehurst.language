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
import net.akehurst.language.api.style.AglStyle
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
import kotlin.browser.window

class AglEditorMonaco(
        val element: Element,
        editorId: String,
        languageId: String,
        goalRule: String? = null,
        options: dynamic, //TODO: types for this
        workerScriptName:String
) : AglEditorAbstract(languageId, editorId) {

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

        fun initialise(document: Document, workerScriptName:String, tag: String = "agl-editor"): Map<String, AglEditorMonaco> {
            val map = mutableMapOf<String, AglEditorMonaco>()
            document.querySelectorAll(tag).asList().forEach { el ->
                val element = el as Element
                //delete any current children of element
                while (element.childElementCount != 0) {
                    element.removeChild(element.firstChild!!)
                }
                val id = element.getAttribute("id")!!
                val editor = AglEditorMonaco(element, id, id, null, null,workerScriptName)
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

    var aglWorker = AglWorkerClient(workerScriptName)
    lateinit var workerTokenizer: AglTokenizerByWorkerMonaco
    var parseTimeout: dynamic = null

    init {
        try {
            this.workerTokenizer = AglTokenizerByWorkerMonaco(this, this.agl)

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
            monaco.languages.setTokensProvider(this.languageId, this.workerTokenizer);
            languages.registerCompletionItemProvider(this.languageId, AglCompletionProvider(this.agl))

            this.onChange {
                this.workerTokenizer.reset()
                window.clearTimeout(parseTimeout)
                this.parseTimeout = window.setTimeout({
                    this.workerTokenizer.acceptingTokens = true
                    this.doBackgroundTryParse()
                }, 500)
            }

            val self = this
            val resizeObserver: dynamic = js("new ResizeObserver(function(entries) { self.onResize(entries) })")
            resizeObserver.observe(this.element)

            this.aglWorker.initialise()
            this.aglWorker.setStyleResult = { success, message ->
                if (success) {
                    this.resetTokenization()
                } else {
                    console.error("Error: $message")
                }
            }
            this.aglWorker.processorCreateSuccess = this::processorCreateSuccess
            this.aglWorker.processorCreateFailure = { msg -> console.error("Failed to create processor $msg") }
            this.aglWorker.parseSuccess = this::parseSuccess
            this.aglWorker.parseFailure = this::parseFailure
            this.aglWorker.lineTokens = {
                console.asDynamic().debug("Debug: new line tokens from successful parse of ${editorId}")
                this.workerTokenizer.receiveTokens(it)
                this.resetTokenization()
            }
            this.aglWorker.processSuccess = { tree ->
                this.notifyProcess(ProcessEvent(true, "OK", tree))
            }
            this.aglWorker.processFailure = { message ->
                this.notifyProcess(ProcessEvent(false, message, "No Asm"))
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
            this.agl.styleHandler.reset()
            val rules: List<AglStyleRule> = Agl.styleProcessor.process(css)
            var mappedCss = ""
            rules.forEach { rule ->
                val cssClass = '.' + this.languageId + ' ' + ".monaco_" + this.agl.styleHandler.mapClass(rule.selector);
                val mappedRule = AglStyleRule(cssClass)
                mappedRule.styles = rule.styles.values.associate { oldStyle ->
                    val style = when (oldStyle.name) {
                        "foreground" -> AglStyle("color", oldStyle.value)
                        "background" -> AglStyle("background-color", oldStyle.value)
                        "font-style" -> when (oldStyle.value) {
                            "bold" -> AglStyle("font-weight", oldStyle.value)
                            "italic" -> AglStyle("font-style", oldStyle.value)
                            else -> oldStyle
                        }
                        else -> oldStyle
                    }
                    Pair(style.name, style)
                }.toMutableMap()
                mappedCss = mappedCss + "\n" + mappedRule.toCss()
            }
            val cssText: String = mappedCss
            // remove the current style element for 'languageId' (which is used as the theme name) from the container
            // else the theme css is not reapplied
            val curStyle = this.element.ownerDocument?.querySelector("style#" + this.languageId)
            if (null != curStyle) {
                curStyle.parentElement?.removeChild(curStyle)
            }
            //add style element
            val styleElement = this.element.ownerDocument?.createElement("style")!!
            styleElement.setAttribute("id", this.languageId)
            styleElement.textContent = cssText
            this.element.ownerDocument?.querySelector("head")?.appendChild(
                    styleElement
            )
            this.aglWorker.setStyle(languageId, editorId, css)
        }
    }

    override fun setProcessor(grammarStr: String?) {
        this.clearErrorMarkers()
        this.aglWorker.createProcessor(languageId, editorId,grammarStr)
        if (null == grammarStr || grammarStr.trim().isEmpty()) {
            this.agl.processor = null
        } else {
            try {
                when (grammarStr) {
                    "@Agl.grammarProcessor@" -> this.agl.processor  = Agl.grammarProcessor
                    "@Agl.styleProcessor@" -> this.agl.processor =  Agl.styleProcessor
                    "@Agl.formatProcessor@" -> this.agl.processor =  Agl.formatProcessor
                    else -> this.agl.processor =  Agl.processor(grammarStr)
                }
            } catch (t: Throwable) {
                this.agl.processor = null
                console.error(t.message)
            }
        }
        this.workerTokenizer.reset()
        this.resetTokenization() //new processor so find new tokens, first by scan
    }

    private fun processorCreateSuccess(message:String) {
        when (message) {
            "OK" -> {
                console.asDynamic().debug("Debug: New Processor created for ${editorId}")
                this.workerTokenizer.acceptingTokens = true
                this.doBackgroundTryParse()
                this.resetTokenization()
            }
            "reset" -> {
                console.asDynamic().debug("Debug: reset Processor for ${editorId}")
            }
            else -> {
                console.error("Error: unknown result message from create Processor for ${editorId}: $message")
            }
        }
    }

    @JsName("onResize")
    private fun onResize(entries: Array<dynamic>) {
        entries.forEach { entry ->
            if (entry.target == this.element) {
                this.monacoEditor.layout()
            }
        }
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

    fun doBackgroundTryParse() {
        this.clearErrorMarkers()
        this.aglWorker.interrupt(languageId, editorId)
        this.aglWorker.tryParse(languageId, editorId,this.text)
    }

    private fun tryParse() {
        val proc = this.agl.processor
        if (null != proc) {
            try {

                val goalRule = this.agl.goalRule
                val sppt = if (null == goalRule) {
                    proc.parse(this.text)
                } else {
                    proc.parse(goalRule, this.text)
                }
                this.agl.sppt = sppt
                this.resetTokenization()
                val event = ParseEvent(true, "OK", sppt)
                this.notifyParse(event)
                //this.doBackgroundTryProcess()
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
                val event = ParseEvent(false, e.message!!, e.longestMatch)
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
                val event = ProcessEvent(true, "OK", this.agl.asm!!)
                this.notifyProcess(event)
            } catch (e: SyntaxAnalyserException) {
                this.agl.asm = null
                val event = ProcessEvent(false, e.message!!, "No Asm")
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

    private fun parseSuccess(tree: Any) {
        this.resetTokenization()
        val event = ParseEvent(true, "OK", tree)
        this.notifyParse(event)
    }

    private fun parseFailure(message: String, location: InputLocation?, tree: Any?) {
        console.error("Error parsing text in ${this.editorId}: $message")
        // parse failed so re-tokenize from scan
        this.workerTokenizer.reset()
        this.resetTokenization()

        if (null != location) {
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
            val event = ParseEvent(false, message, tree)
            this.notifyParse(event)

        }
    }

}

