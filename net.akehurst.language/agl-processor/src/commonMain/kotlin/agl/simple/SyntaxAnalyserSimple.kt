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

package net.akehurst.language.agl.simple


import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserFromAsmTransformAbstract
import net.akehurst.language.asm.api.*
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.collections.lazyMap
import net.akehurst.language.typemodel.api.*

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
class SyntaxAnalyserSimple(
    //grammarNamespaceQualifiedName: QualifiedName,
    typeModel: TypeModel,
    asmTransformModel: TransformModel,
    relevantTrRuleSet: QualifiedName
    //scopeModel: CrossReferenceModel
) : SyntaxAnalyserFromAsmTransformAbstract<Asm>(typeModel, asmTransformModel, relevantTrRuleSet) {
    // : SyntaxAnalyserSimpleAbstract<Asm>(grammarNamespaceQualifiedName, typeModel, asmTransformModel) {//, scopeModel) {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"
    }

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<Asm>> = lazyMap { embGramQName ->
        val ruleSetQname = embGramQName
        SyntaxAnalyserSimple(typeModel, asmTransformModel, ruleSetQname)//, this.scopeModel) //TODO: needs embedded asmTransform
    }

}