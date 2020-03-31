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

package net.akehurst.language.editor.ace

import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.api.analyser.SyntaxAnalyserException
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.style.AglStyle
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.editor.api.ParseEvent
import net.akehurst.language.editor.api.ProcessEvent
import net.akehurst.language.editor.common.AglEditorAbstract
import net.akehurst.language.editor.common.AglStyleHandler
import net.akehurst.language.editor.comon.AglWorkerClient
import net.akehurst.language.processor.Agl
import org.w3c.dom.*
import kotlin.browser.window

class AglErrorAnnotation(
        val line: Int,
        val column: Int,
        val text: String,
        val type: String,
        val raw: String?
) {
    val row = line - 1
}

class AglEditorAce(
        val element: Element,
        languageId: String,
        editorId: String,
        options: dynamic, //TODO: types for this
        workerScriptName: String
) : AglEditorAbstract(languageId, editorId) {

    companion object {
        fun initialise(document: Document, workerScriptName: String, tag: String = "agl-editor"): Map<String, AglEditorAce> {
            val map = mutableMapOf<String, AglEditorAce>()
            document.querySelectorAll(tag).asList().forEach { el ->
                val element = el as Element
                //delete any current children of element
                while (element.childElementCount != 0) {
                    element.removeChild(element.firstChild!!)
                }
                val id = element.getAttribute("id")!!
                val editor = AglEditorAce(element, id, id, null, workerScriptName)
                map[id] = editor
            }
            return map
        }
    }

    private val errorParseMarkerIds = mutableListOf<Int>()
    private val errorProcessMarkerIds = mutableListOf<Int>()
    //private val mode: ace.SyntaxMode

    val aceEditor: ace.Editor = ace.Editor(
            ace.VirtualRenderer(this.element, null),
            ace.Ace.createEditSession(""),
            options
    )
    override var text: String
        get() {
            try {
                return this.aceEditor.getValue()
            } catch (t: Throwable) {
                throw RuntimeException("Failed to get text from editor")
            }
        }
        set(value) {
            try {
                this.aceEditor.setValue(value, -1)
            } catch (t: Throwable) {
                throw RuntimeException("Failed to set text in editor")
            }
        }

    var aglWorker = AglWorkerClient(workerScriptName)
    lateinit var workerTokenizer: AglTokenizerByWorkerAce
    var parseTimeout: dynamic = null

    init {
        this.workerTokenizer = AglTokenizerByWorkerAce(this.agl)

        this.aceEditor.getSession().bgTokenizer = AglBackgroundTokenizer(this.workerTokenizer, this.aceEditor)
        this.aceEditor.getSession().bgTokenizer.setDocument(this.aceEditor.getSession().getDocument())
        this.aceEditor.commands.addCommand(ace.ext.Autocomplete.startCommand)
        this.aceEditor.completers = arrayOf(AglCodeCompleter(this.languageId, this.agl))
        //this.aceEditor.commands.addCommand(autocomplete.Autocomplete.startCommand)

        this.aceEditor.on("change") { event ->
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
                val cssClass = '.' + this.languageId + ' ' + ".ace_" + this.agl.styleHandler.mapClass(rule.selector);
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
            val module = js(" { cssClass: this.languageId, cssText: cssText, _v: Date.now() }") // _v:Date added in order to force use of new module definition
            // remove the current style element for 'languageId' (which is used as the theme name) from the container
            // else the theme css is not reapplied
            val curStyle = this.element.ownerDocument?.querySelector("style#" + this.languageId)
            if (null != curStyle) {
                curStyle.parentElement?.removeChild(curStyle);
            }

            // the use of an object instead of a string is undocumented but seems to work
            this.aceEditor.setOption("theme", module); //not sure but maybe this is better than setting on renderer direct
            this.aglWorker.setStyle(languageId, editorId, css)
        }
    }

    @JsName("format")
    fun format() {
        val proc = this.agl.processor
        if (null != proc) {
            val pos = this.aceEditor.getSelection().getCursor();
            val formattedText: String = proc.formatText<AsmElementSimple>(this.text as CharSequence);
            this.aceEditor.setValue(formattedText, -1);
        }
    }

    override fun setProcessor(grammarStr: String?) {
        this.clearErrorMarkers()
        this.aglWorker.createProcessor(languageId, editorId, grammarStr)
        if (null == grammarStr || grammarStr.trim().isEmpty()) {
            this.agl.processor = null
        } else {
            try {
                when (grammarStr) {
                    "@Agl.grammarProcessor@" -> this.agl.processor = Agl.grammarProcessor
                    "@Agl.styleProcessor@" -> this.agl.processor = Agl.styleProcessor
                    "@Agl.formatProcessor@" -> this.agl.processor = Agl.formatProcessor
                    else -> this.agl.processor = Agl.processor(grammarStr)
                }
            } catch (t: Throwable) {
                this.agl.processor = null
                console.error(t.message)
            }
        }
        this.workerTokenizer.reset()
        this.resetTokenization() //new processor so find new tokens, first by scan
    }

    private fun processorCreateSuccess(message: String) {
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
                this.aceEditor.resize(true)
            }
        }
    }

    private fun setupCommands() {
        /*
        this.aceEditor.commands.addCommand({
            name: 'format',
            bindKey: {win: 'Ctrl-F', mac: 'Command-F'},
            exec: (editor) => this.format(),
            readOnly: false
        })
         */
    }

    fun resetTokenization() {
        this.aceEditor.renderer.updateText();
        this.aceEditor.getSession().bgTokenizer.start(0);
    }

    fun doBackgroundTryParse() {
        this.clearErrorMarkers()
        this.aglWorker.interrupt(languageId, editorId)
        this.aglWorker.tryParse(languageId, editorId, this.text)
    }

    private fun foregroundParse() {
        val proc = this.agl.processor
        if (null != proc) {
            try {
                val goalRule = this.agl.goalRule
                val sppt = if (null == goalRule) {
                    proc.parse(this.text)
                } else {
                    proc.parse(goalRule, this.text)
                }
                this.parseSuccess(sppt)
            } catch (e: ParseFailedException) {
                this.parseFailure(e.message!!, e.location, e.longestMatch)
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

    override fun clearErrorMarkers() {
        this.aceEditor.getSession().clearAnnotations(); //assume there are no parse errors or there would be no sppt!
        this.errorParseMarkerIds.forEach { id -> this.aceEditor.getSession().removeMarker(id) }
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
            val errors = listOf(
                    AglErrorAnnotation(
                            location.line,
                            location.column - 1,
                            "Syntax Error",
                            "error",
                            message
                    ))
            this.aceEditor.getSession().setAnnotations(errors.toTypedArray())
            errors.forEach { err ->
                val range = ace.Range(err.row, err.column, err.row, err.column + 1)
                val cls = "ace_marker_text_error"
                val errMrkId = this.aceEditor.getSession().addMarker(range, cls, "text")
                this.errorParseMarkerIds.add(errMrkId)
            }
            val event = ParseEvent(false, message, tree)
            this.notifyParse(event)
        }
    }
}