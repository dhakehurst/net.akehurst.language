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

package net.akehurst.language.editor.common

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree

open class AglWorkerMessage(
        val action: String
)

class MessageProcessorCreate(
        val languageId:String,
        val editorId:String,
        val grammarStr: String?
) : AglWorkerMessage("MessageProcessorCreate")

class MessageProcessorCreateSuccess(
        val languageId:String,
        val editorId:String,
        val message: String
) : AglWorkerMessage("MessageProcessorCreateSuccess")

class MessageProcessorCreateFailure(
        val languageId:String,
        val editorId:String,
        val message: String
) : AglWorkerMessage("MessageProcessorCreateFailure")

class MessageParseRequest(
        val languageId:String,
        val editorId:String,
        val text: String
) : AglWorkerMessage("MessageParseRequest")

class MessageParseSuccess(
        val languageId:String,
        val editorId:String,
        val tree: Any
) : AglWorkerMessage("MessageParseSuccess")

class MessageParseFailure(
        val languageId:String,
        val editorId:String,
        val message: String,
        val location: InputLocation?,
        val tree: Any?
) : AglWorkerMessage("MessageParseFailure")

class MessageParserInterruptRequest(
        val languageId:String,
        val editorId:String,
        val reason: String
) : AglWorkerMessage("MessageParserInterruptRequest")

class MessageLineTokens(
        val languageId:String,
        val editorId:String,
        val lineTokens: Array<Array<AglToken>>
) : AglWorkerMessage("MessageLineTokens")

class MessageSetStyle(
        val languageId:String,
        val editorId:String,
        val css: String
) : AglWorkerMessage("MessageSetStyle")

class MessageSetStyleResult(
        val languageId:String,
        val editorId:String,
        val success: Boolean,
        val message: String
) : AglWorkerMessage("MessageSetStyleResult")

class MessageProcessSuccess(
        val languageId:String,
        val editorId:String,
        val asm:Any
) : AglWorkerMessage("MessageProcessSuccess")

class MessageProcessFailure(
        val languageId:String,
        val editorId:String,
        val message:String
) : AglWorkerMessage("MessageProcessFailure")