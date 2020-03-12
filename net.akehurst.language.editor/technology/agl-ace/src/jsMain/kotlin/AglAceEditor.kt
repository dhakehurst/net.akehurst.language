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

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList

class AglAceEditor(
        val element: HTMLElement,
        val editorId: String,
        val languageId: String,
        val goalRule:String?,
        initialText:String,
        options:Any?
) {

    companion object {
        fun initialise(document: Document, tag:String="agl-editor") {
            document.querySelectorAll(tag).asList().forEach { el ->
                val element = el as HTMLElement
                val id = element.getAttribute("id")!!
                val initContent = element.textContent?.trimIndent() ?: ""
                AglAceEditor(element, id, id, null, initContent, null)
            }
        }
    }

    private val errorParseMarkerIds = mutableListOf<String>()
    private val errorProcessMarkerIds = mutableListOf<String>()
    //private val mode: ace.SyntaxMode

    val aceEditor: ace.Editor = ace.Editor(
            ace.VirtualRenderer(this.element, null),
            ace.Ace.createEditSession(initialText),
            options
    )

    init {
        //this.aceEditor.commands.addCommand(autocomplete.Autocomplete.startCommand)
    }

}