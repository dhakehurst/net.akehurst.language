/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.processor

import net.akehurst.language.api.processor.*
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.scanner.api.ScanOptions
import net.akehurst.language.scanner.common.ScanOptionsDefault

class ProcessOptionsDefault<AsmType : Any, ContextType : Any>(
    override val scan: ScanOptions = ScanOptionsDefault(),
    override val parse: ParseOptions = ParseOptionsDefault(),
    override val syntaxAnalysis: SyntaxAnalysisOptions<AsmType> = SyntaxAnalysisOptionsDefault(),
    override val semanticAnalysis: SemanticAnalysisOptions<ContextType> = SemanticAnalysisOptionsDefault(),
    override val completionProvider: CompletionProviderOptions<ContextType> = CompletionProviderOptionsDefault()
) : ProcessOptions<AsmType, ContextType>

class SyntaxAnalysisOptionsDefault<AsmType : Any>(
    override var active: Boolean = true
) : SyntaxAnalysisOptions<AsmType>

class SemanticAnalysisOptionsDefault<ContextType : Any>(
    override var active: Boolean = true,
    override var locationMap: Map<Any, InputLocation> = emptyMap(),
    override var context: ContextType? = null,
    override var buildScope: Boolean = true,
    override var replaceIfItemAlreadyExistsInScope: Boolean = false,
    override var ifItemAlreadyExistsInScopeIssueKind: LanguageIssueKind? = LanguageIssueKind.ERROR,
    override var checkReferences: Boolean = true,
    override var resolveReferences: Boolean = true,
    override val other: Map<String, Any> = mutableMapOf()
) : SemanticAnalysisOptions<ContextType>

class CompletionProviderOptionsDefault<ContextType : Any>(
    override var context: ContextType? = null,
    override val other: Map<String, Any> = mutableMapOf()
) : CompletionProviderOptions<ContextType>
