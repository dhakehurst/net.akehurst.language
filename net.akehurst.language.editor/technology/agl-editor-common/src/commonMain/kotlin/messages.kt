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
        val grammarStr: String?
) : AglWorkerMessage("MessageProcessorCreate")

class MessageParseRequest(
        val text: String
) : AglWorkerMessage("MessageParseRequest")

class MessageParseResponseSuccess(

) : AglWorkerMessage("MessageParseResponseSuccess")

class MessageParseResponseFailure(
        val message: String,
        val location: InputLocation?
) : AglWorkerMessage("MessageParseResponseFailure")

class MessageParserInterruptRequest(
        val reason: String
) : AglWorkerMessage("MessageParserInterruptRequest")

class MessageLineTokens(
        val lineTokens: Array<Array<AglToken>>
) : AglWorkerMessage("MessageLineTokens")

class MessageSetStyle(
        val css: String
) : AglWorkerMessage("MessageSetStyle")