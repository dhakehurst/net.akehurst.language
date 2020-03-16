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
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.style.AglStyle
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.editor.api.*
import net.akehurst.language.processor.Agl
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.asList

class AglComponentsAce(
        aglEditor: AglEditor,
        goalRule: String?
) : AglComponents(aglEditor, goalRule) {
    var nextCssClassNum = 1
    val cssClassPrefix = "tok"
    val tokenToClassMap = mutableMapOf<String, String>();
}

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
        val editorId: String,
        val languageId: String,
        goalRule: String?,
        options: dynamic //TODO: types for this
) : AglEditor {

    companion object {
        fun initialise(document: Document, tag: String = "agl-editor"): Map<String, AglEditorAce> {
            val map = mutableMapOf<String, AglEditorAce>()
            document.querySelectorAll(tag).asList().forEach { el ->
                val element = el as Element
                val id = element.getAttribute("id")!!
                val editor = AglEditorAce(element, id, id, null, null)
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
    val agl = AglComponentsAce(this, goalRule)

    var text: String
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

    private val _onParseHandler = mutableListOf<(ParseEvent) -> Unit>()
    private val _onProcessHandler = mutableListOf<(ProcessEvent) -> Unit>()


    init {
        this.aceEditor.getSession().bgTokenizer = AglBackgroundTokenizer(AglTokenizer(this.agl), this.aceEditor)
        this.aceEditor.getSession().bgTokenizer.setDocument(this.aceEditor.getSession().getDocument())
        this.aceEditor.commands.addCommand(ace.ext.Autocomplete.startCommand)
        this.aceEditor.completers = arrayOf(AglCodeCompleter(this.languageId,this.agl))
        //this.aceEditor.commands.addCommand(autocomplete.Autocomplete.startCommand)

        this.aceEditor.on("change") { event ->
            this.agl.sppt = null
        }
        this.aceEditor.on("input") { event ->
            this.doBackgroundTryParse()
        }

        val self = this
        val resizeObserver: dynamic = js("new ResizeObserver(function(entries) { self.onResize(entries) })")
        resizeObserver.observe(this.element)
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

    private fun mapTokenTypeToClass(tokenType: String): String {
        var cssClass = this.agl.tokenToClassMap.get(tokenType);
        if (null == cssClass) {
            cssClass = this.agl.cssClassPrefix + this.agl.nextCssClassNum++;
            this.agl.tokenToClassMap.set(tokenType, cssClass);
        }
        return cssClass
    }

    @JsName("setStyle")
    fun setStyle(css: String?) {
        if (null != css && css.isNotEmpty()) {
            val rules: List<AglStyleRule> = Agl.styleProcessor.process(css)
            var mappedCss = ""
            rules.forEach { rule ->
                val cssClass = '.' + this.languageId + ' ' + ".ace_" + this.mapTokenTypeToClass(rule.selector);
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
            val cssText:String = mappedCss
            val module = js(" { cssClass: this.languageId, cssText: cssText, _v: Date.now() }") // _v:Date added in order to force use of new module definition
            // remove the current style element for 'languageId' (which is used as the theme name) from the container
            // else the theme css is not reapplied
            val curStyle = this.element.ownerDocument?.querySelector("style#" + this.languageId)
            if (null!=curStyle) {
                curStyle.parentElement?.removeChild(curStyle);
            }

            // the use of an object instead of a string is undocumented but seems to work
            this.aceEditor.setOption("theme", module); //not sure but maybe this is better than setting on renderer direct
        } else {

        }
    }

    @JsName("format")
    fun format() {
        val proc = this.agl.processor
        if (null!=proc) {
            val pos = this.aceEditor.getSelection().getCursor();
            val formattedText:String = proc.formatText<AsmElementSimple>(this.text as CharSequence);
            this.aceEditor.setValue(formattedText, -1);
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

    fun onParse(handler: (ParseEvent) -> Unit) {
        this._onParseHandler.add(handler)
    }

    fun notifyParse(event: ParseEvent) {
        this._onParseHandler.forEach {
            it.invoke(event)
        }
    }

    fun onProcess(handler: (ProcessEvent) -> Unit) {
        this._onProcessHandler.add(handler)
    }

    fun notifyProcess(event: ProcessEvent) {
        this._onProcessHandler.forEach {
            it.invoke(event)
        }
    }

    override fun doBackgroundTryParse() {
        //TODO: background!
        tryParse()
    }

    fun doBackgroundTryProcess() {
        //TODO: background!
        tryProcess()
    }

    fun resetTokenization() {
        this.aceEditor.renderer.updateText();
        this.aceEditor.getSession().bgTokenizer.start(0);
    }

    private fun tryParse() {
        val proc = this.agl.processor
        if (null != proc) {
            try {
                this.aceEditor.getSession().clearAnnotations(); //assume there are no parse errors or there would be no sppt!
                this.errorProcessMarkerIds.forEach { id -> this.aceEditor.getSession().removeMarker(id) }
                val goalRule = this.agl.goalRule
                if (null == goalRule) {
                    this.agl.sppt = proc.parse(this.text)
                } else {
                    this.agl.sppt = proc.parse(goalRule, this.text)
                }
                this.resetTokenization()
                val event = ParseEvent(true, "OK", this.agl.sppt)
                this.notifyParse(event)
                this.doBackgroundTryProcess()
            } catch (e: ParseFailedException) {
                this.agl.sppt = null
                // parse failed so re-tokenize from scan
                this.resetTokenization()
                console.error("Error parsing text in " + this.editorId + " for language " + this.languageId, e.message);
                val errors = listOf(
                        AglErrorAnnotation(
                                e.location.line,
                                e.location.column - 1,
                                "Syntax Error",
                                "error",
                                e.message
                        ))
                this.aceEditor.getSession().setAnnotations(errors);
                val event = ParseEvent(false, e.message!!, this.agl.sppt)
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
                val event = ProcessEvent(true, "OK", this.agl.asm)
                this.notifyProcess(event)
            } catch (e: SyntaxAnalyserException) {
                this.agl.asm = null


                val event = ProcessEvent(false, e.message!!, null)
                this.notifyProcess(event)
            } catch (t: Throwable) {
                console.error("Error processing parse result in " + this.editorId + " for language " + this.languageId, t.message)
            }
        }
    }

}