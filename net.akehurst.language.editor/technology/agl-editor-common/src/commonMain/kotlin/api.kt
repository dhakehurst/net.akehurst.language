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

package net.akehurst.language.editor.api

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.js.JsName

interface AglEditor {

    val editorId:String
    var text:String

    @JsName("setProcessor")
    fun setProcessor(grammarStr: String?)

    @JsName("setStyle")
    fun setStyle(css: String?)

    @JsName("onParse")
    fun onParse(function: (ParseEvent) -> Unit)

    @JsName("onProcess")
    fun onProcess(function: (ProcessEvent) -> Unit)

    @JsName("clearErrorMarkers")
    fun clearErrorMarkers()

    @JsName("finalize")
    fun finalize()
}

class ParseEvent(
        val success: Boolean,
        val message: String,
        val tree:Any?
)

class ProcessEvent(
        val success: Boolean,
        val message: String,
        val tree:Any
)
