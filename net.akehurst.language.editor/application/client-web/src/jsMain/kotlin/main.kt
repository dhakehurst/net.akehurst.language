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

package net.akehurst.language.editor.application.client.web

import net.akehurst.language.api.analyser.AsmElementProperty
import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.editor.ace.AglEditorAce
import net.akehurst.language.editor.api.AglEditor
import net.akehurst.language.editor.information.Examples
import net.akehurst.language.editor.information.examples.Datatypes
import net.akehurst.language.editor.information.examples.GraphvizDot
import net.akehurst.language.editor.information.examples.SText
import net.akehurst.language.editor.monaco.AglEditorMonaco
import net.akehurst.language.editor.technology.gui.widgets.TabView
import net.akehurst.language.editor.technology.gui.widgets.TreeView
import net.akehurst.language.editor.technology.gui.widgets.TreeViewFunctions
import net.akehurst.language.processor.Agl
import net.akehurst.language.processor.AglLanguage
import org.w3c.dom.HTMLElement
import kotlin.browser.document

var demo: Demo? = null
fun main() {
    val editorChoiceAce = document.querySelector("#editor-choice-ace")!!
    val editorChoiceMonaco = document.querySelector("#editor-choice-monaco")!!

    TabView.initialise(document)
    initialiseExamples()

    createDemo(true)

    editorChoiceAce.addEventListener("click", {
        createDemo(true)
    })
    editorChoiceMonaco.addEventListener("click", {
        createDemo(false)
    })

}

fun initialiseExamples() {
    val exampleSelect = document.querySelector("select#example") as HTMLElement
    Examples.add(Datatypes.example)
    Examples.add(GraphvizDot.example)
    Examples.add(SText.example)

    Examples.map.forEach { eg ->
        val option = document.createElement("option")
        exampleSelect.appendChild(option);
        option.setAttribute("value", eg.value.id);
        option.textContent = eg.value.label;
    }
}


fun createDemo(isAce: Boolean) {
    if(null!=demo) {
        demo!!.finalize()
    }
    val editors = if (isAce) {
        AglEditorAce.initialise(document)
    } else {
        AglEditorMonaco.initialise(document)
    }

    demo = Demo(editors)
    demo!!.configure()
}

class Demo(
        val editors: Map<String, AglEditor>
) {
    val trees = TreeView.initialise(document)

    val exampleSelect = document.querySelector("select#example") as HTMLElement
    val sentenceEditor = editors["sentence-text"]!!
    val grammarEditor = editors["language-grammar"]!!
    val styleEditor = editors["language-style"]!!
    val formatEditor = editors["language-format"]!!

    fun configure() {
        this.connectEditors()
        this.connectTrees()
        this.configExampleSelector()
    }

    fun connectEditors() {
        grammarEditor.setProcessor("@Agl.grammarProcessor@")
        styleEditor.setProcessor("@Agl.styleProcessor@")
        formatEditor.setProcessor("@Agl.formatProcessor@")

        grammarEditor.setStyle(AglLanguage.grammar.style)
        styleEditor.setStyle(AglLanguage.style.style)

        grammarEditor.onParse { event ->
            if (event.success) {
                try {
                    sentenceEditor.setProcessor(grammarEditor.text)
                } catch (t: Throwable) {
                    sentenceEditor.setProcessor(null)
                    console.error(grammarEditor.editorId + ": " + t.message)
                }
            } else {
                sentenceEditor.setProcessor(null)
                console.error(grammarEditor.editorId + ": " + event.message)
            }
        }

        styleEditor.onParse { event ->
            if (event.success) {
                try {
                    sentenceEditor.setStyle(styleEditor.text)
                } catch (t: Throwable) {
                    console.error(sentenceEditor.editorId + ": " + t.message)
                }
            } else {
            }
        }
    }

    fun connectTrees() {
        trees["parse"]!!.treeFunctions = TreeViewFunctions<dynamic>(
                label = {
                    when (it.isBranch) {
                        false -> "${it.name} = ${it.nonSkipMatchedText}"
                        true -> it.name
                        else -> error("error")
                    }
                },
                hasChildren = { it.isBranch },
                children = { it.children }
        ) as TreeViewFunctions<Any>

        sentenceEditor.onParse { event ->
            if (event.success) {
                trees["parse"]!!.root = event.tree
            } else {
            }
        }

        trees["asm"]!!.treeFunctions = TreeViewFunctions<dynamic>(
                label = {
                    when {
                        it is Array<*> -> ": List"
                        it.isAsmElementSimple -> ": " + it.typeName
                        it.isAsmElementProperty -> {
                            val v = it.value
                            when (v) {
                                is AsmElementSimple -> "${it.name} : ${v.typeName}"
                                is List<*> -> "${it.name} : List"
                                else -> "${it.name} = ${v}"
                            }
                        }
                        else -> it.toString()
                    }
                },
                hasChildren = {
                    when {
                        it is Array<*> -> true
                        it.isAsmElementSimple -> it.properties.size != 0
                        it.isAsmElementProperty -> {
                            val v = it.value
                            when {
                                v is Array<*> -> true
                                v.isAsmElementSimple -> true
                                else -> false
                            }
                        }
                        else -> false
                    }
                },
                children = {
                    when {
                        it is Array<*> -> it
                        it.isAsmElementSimple -> it.properties
                        it.isAsmElementProperty -> {
                            val v = it.value
                            when {
                                v is Array<*> -> v
                                v.isAsmElementSimple -> v.properties
                                else -> emptyArray<dynamic>()
                            }
                        }
                        else -> emptyArray<dynamic>()
                    }
                }
        )

        sentenceEditor.onProcess { event ->
            if (event.success) {
                trees["asm"]!!.root = event.tree
            } else {
                console.error(event.message)
                trees["asm"]!!.root = event.tree
            }
        }
    }

    fun configExampleSelector() {
        exampleSelect.addEventListener("change", { _ ->
            val egName = js("event.target.value") as String
            val eg = Examples[egName]
            grammarEditor.text = eg.grammar
            styleEditor.text = eg.style
            formatEditor.text = eg.format
            sentenceEditor.text = eg.sentence
        })
        // select initial example
        val eg = Datatypes.example
        grammarEditor.text = eg.grammar
        styleEditor.text = eg.style
        formatEditor.text = eg.format
        sentenceEditor.text = eg.sentence
    }

    fun finalize() {
        editors.values.forEach {
            it.finalize()
        }
    }
}


