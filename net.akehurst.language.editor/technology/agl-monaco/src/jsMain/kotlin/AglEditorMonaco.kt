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

import monaco.editor
import monaco.editor.IStandaloneCodeEditor
import monaco.languages
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.processor.Agl
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.ResizeQuality
import org.w3c.dom.asList
import kotlin.browser.document

class AglComponents(
        val aglEditor: AglEditorMonaco
) {
    var processor: LanguageProcessor? = null
    var sppt: SharedPackedParseTree? = null
}

class AglEditorMonaco(
        val element: Element,
        val editorId: String,
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
        // https://github.com/Microsoft/monaco-editor/issues/338
        // all editors on the same page must share the same theme!
        // hence we create a global theme and modify it as needed.
        private val aglGlobalTheme = "agl-theme"
        val allAglGlobalThemeRules = mutableMapOf<String, editor.ITokenThemeRule>()

        fun initialise(document: Document, tag: String = "agl-editor"): Map<String, AglEditorMonaco> {
            val map = mutableMapOf<String, AglEditorMonaco>()
            document.querySelectorAll(tag).asList().forEach { el ->
                val element = el as Element
                val id = element.getAttribute("id")!!
                val editor = AglEditorMonaco(element, id, id)
                map[id] = editor
            }
            return map
        }
    }

    lateinit var monacoEditor: IStandaloneCodeEditor
    val languageThemePrefix = this.languageId + "-"
    val agl = AglComponents(this)

    var text: String
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
                throw RuntimeException("Failed to get text from editor")
            }
        }

    val _scanTokenProvider = AglTokenProvider(this.languageThemePrefix, this.agl)

    init {
        try {
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

            val languageId = this.languageId
            val initialContent = ""
            val theme = aglGlobalTheme
            val editorOptions = js("{language: languageId, value: initialContent, theme: theme}")
            this.monacoEditor = monaco.editor.create(this.element, editorOptions, null)
            monaco.languages.setTokensProvider(this.languageId, this._scanTokenProvider);

            val self = this
            val resizeObserver: dynamic = js("new ResizeObserver(function(entries) { self.onResize(entries) })")
            resizeObserver.observe(this.element)
        } catch (t: Throwable) {
            println(t.message)
        }
    }

    fun onChange(handler: (String) -> Unit) {
        this.monacoEditor.onDidChangeModelContent { event ->
            val text = this.text
            handler(text);
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

    private fun convertColor(value: String?) :String? {
        if (null==value) {
            return null
        } else {
            val cvs = document.createElement("canvas")
            val ctx = cvs.asDynamic().getContext("2d")
            ctx.fillStyle = value
            val col = ctx.fillStyle as String
            return col.substring(1) //leave out the #
        }
    }

    fun setStyle(css: String) {
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

    private fun setupCommands() {

    }


}

class AglTokenProvider(
        val tokenPrefix: String,
        val agl: AglComponents
) : languages.TokensProvider {

    class ScanState(
            val lineNumber: Int,
            val leftOverText: String
    ) : languages.IState {
        override fun clone(): languages.IState {
            return ScanState(lineNumber, leftOverText)
        }

        override fun equals(other: Any?): Boolean {
            return when (other) {
                is ScanState -> other.lineNumber == this.lineNumber
                else -> false
            }
        }
    }

    override fun getInitialState(): languages.IState {
        return ScanState(0, "")
    }

    override fun tokenize(line: String, state: languages.IState): languages.ILineTokens {
        if (null == this.agl.sppt) {
            return this.getLineTokensByScan(line, state)
        } else {
            TODO()
//            return this.getLineTokensByParse(line, state)
        }
    }

    private fun getLineTokensByScan(line: String, pState: languages.IState): languages.ILineTokens {
        val state = pState as ScanState
        val proc = this.agl.processor
        val nextLineNumber = state.lineNumber + 1
        if (null != proc) {
            val text = state.leftOverText + line
            val leafs = proc.scan(text);
            val tokens = leafs.map { leaf ->
                object : languages.IToken {
                    override val scopes = tokenPrefix + leaf.name
                    override val startIndex = leaf.startPosition
                }
            }
            val endState = if (leafs.isEmpty()) {
                ScanState(nextLineNumber, text)
            } else {
                val lastLeaf = leafs.last()
                val endOfLastLeaf = lastLeaf.location.column + lastLeaf.location.length
                val leftOverText = line.substring(endOfLastLeaf, line.length)
                ScanState(nextLineNumber, leftOverText)
            }
            return object : languages.ILineTokens {
                override val endState = endState
                override val tokens: Array<languages.IToken> = tokens.toTypedArray()
            }
        } else {
            return object : languages.ILineTokens {
                override val endState = ScanState(nextLineNumber, "")
                override val tokens = arrayOf<languages.IToken>(
                        object : languages.IToken {
                            override val scopes = ""
                            override val startIndex = 0
                        }
                )
            }
        }
    }
}