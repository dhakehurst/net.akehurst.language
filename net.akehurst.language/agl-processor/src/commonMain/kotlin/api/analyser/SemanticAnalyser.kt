/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.analyser

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.SemanticAnalysisResult
import kotlin.js.JsExport

class SemanticAnalyserException(message: String, cause: Throwable?) : RuntimeException(message, cause)

/**
 *
 * A Semantic Analyser, language specific functionality
 *
 */
interface SemanticAnalyser<in AsmType, in ContextType> {

    fun clear()

    fun analyse(asm: AsmType, locationMap: Map<Any,InputLocation>?=null, context:ContextType?=null): SemanticAnalysisResult
}

