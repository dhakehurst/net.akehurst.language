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

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.CompletionProviderOptions
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SyntaxAnalysisOptions
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.scanner.api.ScanOptions
import net.akehurst.language.scanner.common.ScanOptionsDefault
import net.akehurst.language.sentence.api.InputLocation

class ProcessOptionsDefault<AsmType : Any, ContextType : Any>(
    override val scan: ScanOptions = ScanOptionsDefault(),
    override val parse: ParseOptions = ParseOptionsDefault(),
    override val syntaxAnalysis: SyntaxAnalysisOptions<AsmType> = SyntaxAnalysisOptionsDefault(),
    override val semanticAnalysis: SemanticAnalysisOptions<ContextType> = SemanticAnalysisOptionsDefault(),
    override val completionProvider: CompletionProviderOptions<ContextType> = CompletionProviderOptionsDefault()
) : ProcessOptions<AsmType, ContextType> {
    override fun clone()= ProcessOptionsDefault<AsmType, ContextType>(
        scan = scan.clone(),
        parse = parse.clone(),
        syntaxAnalysis = syntaxAnalysis.clone(),
        semanticAnalysis = semanticAnalysis.clone(),
        completionProvider = completionProvider.clone()
    )
}

class SyntaxAnalysisOptionsDefault<AsmType : Any>(
    override var enabled: Boolean = true
) : SyntaxAnalysisOptions<AsmType> {
    override fun clone()= SyntaxAnalysisOptionsDefault<AsmType>(
        enabled = this.enabled
    )
}

class SemanticAnalysisOptionsDefault<ContextType : Any>(
    override var enabled: Boolean = true,
    override var locationMap: LocationMap = LocationMapDefault(),
    override var context: ContextType? = null,
    override var buildScope: Boolean = true,
    override var replaceIfItemAlreadyExistsInScope: Boolean = false,
    override var ifItemAlreadyExistsInScopeIssueKind: LanguageIssueKind? = LanguageIssueKind.ERROR,
    override var checkReferences: Boolean = true,
    override var resolveReferences: Boolean = true,
    override val other: Map<String, Any> = mutableMapOf()
) : SemanticAnalysisOptions<ContextType> {
    override fun clone()= SemanticAnalysisOptionsDefault<ContextType>(
        enabled = this.enabled,
        locationMap = this.locationMap,
        context = this.context,
        buildScope = this.buildScope,
        replaceIfItemAlreadyExistsInScope = this.replaceIfItemAlreadyExistsInScope,
        ifItemAlreadyExistsInScopeIssueKind = this.ifItemAlreadyExistsInScopeIssueKind,
        checkReferences = this.checkReferences,
        resolveReferences = this.resolveReferences,
        other = this.other
    )
}

class CompletionProviderOptionsDefault<ContextType : Any>(
    override var context: ContextType? = null,
    override var depth: Int = 0,
    override var path: List<Pair<Int, Int>> = emptyList(),
    override var showOptionalItems: Boolean = true,
    override var provideValuesForPatternTerminals: Boolean = false,
    override val other: Map<String, Any> = mutableMapOf()
) : CompletionProviderOptions<ContextType> {
    override fun clone() = CompletionProviderOptionsDefault<ContextType>(
        context = this.context,
        depth = this.depth,
        path = this.path,
        showOptionalItems = this.showOptionalItems,
        provideValuesForPatternTerminals = this.provideValuesForPatternTerminals,
        other = other
    )
}
