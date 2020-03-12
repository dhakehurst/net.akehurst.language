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

package net.akehurst.language.editor.application.client.monaco

import net.akehurst.language.editor.information.Examples
import net.akehurst.language.editor.information.examples.Datatypes
import kotlin.browser.*

import net.akehurst.language.editor.technology.tabview.TabView
import net.akehurst.language.editor.monaco.AglEditorMonaco
import net.akehurst.language.processor.Agl
import net.akehurst.language.processor.AglLanguage
import org.w3c.dom.HTMLElement

fun initialiseExamples(selectEl: HTMLElement) {
    Examples.add(Datatypes.example)
    Examples.map.forEach { eg ->
        val option = document.createElement("option")
        selectEl.appendChild(option);
        option.setAttribute("value", eg.value.id);
        option.textContent = eg.value.label;
    }
}


fun main() {
    val exampleSelect = document.querySelector("select#example") as HTMLElement;
    initialiseExamples(exampleSelect)
    TabView.initialise(document)
    val editors = AglEditorMonaco.initialise(document)

    val sentenceEditor = editors["sentence-text"]!!
    val grammarEditor = editors["language-grammar"]!!
    val styleEditor = editors["language-style"]!!
    val formatEditor = editors["language-format"]!!

    grammarEditor.agl.processor = Agl.grammarProcessor
    styleEditor.agl.processor = Agl.styleProcessor
    formatEditor.agl.processor = Agl.formatProcessor

    grammarEditor.setStyle(AglLanguage.grammar.style)
    styleEditor.setStyle(AglLanguage.style.style)

    grammarEditor.onChange {
        try {
            sentenceEditor.agl.processor = Agl.processor(grammarEditor.text)
        } catch (t: Throwable) {
            sentenceEditor.agl.processor = null
            console.error(grammarEditor.editorId + ": " + t.message)
        }
    }

    styleEditor.onChange {
        try {
            sentenceEditor.setStyle(styleEditor.text)
        } catch (t: Throwable) {
            console.error(sentenceEditor.editorId + ": " + t.message)
        }
    }

    exampleSelect.addEventListener("change", { event ->
        val egName = js("event.target.value") as String
        val eg = Examples[egName]
        grammarEditor.text = eg.grammar
        styleEditor.text = eg.style
        formatEditor.text = eg.format
        sentenceEditor.text = eg.sentence
    })
    // select initial example
    val eg = Examples["datatypes"]
    grammarEditor.text = eg.grammar
    styleEditor.text = eg.style
    formatEditor.text = eg.format
    sentenceEditor.text = eg.sentence
}