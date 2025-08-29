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


import net.akehurst.kotlinx.collections.lazyMap
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserFromAsmTransformAbstract
import net.akehurst.language.api.syntaxAnalyser.AsmFactory
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.simple.AsmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.ObjectGraphAsmSimple
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.types.api.TypesDomain

class AsmFactorySimple(
    typesDomain: TypesDomain,
    issues: IssueHolder
) : ObjectGraphAsmSimple(typesDomain, issues), AsmFactory<Asm, AsmValue> {

    override fun constructAsm(): Asm = AsmSimple()

    override fun rootList(asm: Asm): List<AsmValue> = asm.root
    override fun addRoot(asm: Asm, root: AsmValue) =(asm as AsmSimple).addRoot(root)
    override fun removeRoot(asm: Asm, root: AsmValue) = (asm as AsmSimple).removeRoot(root)
}

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
class SyntaxAnalyserSimple(
    typesDomain: TypesDomain,
    asmTransformDomain: AsmTransformDomain,
    relevantTrRuleSet: QualifiedName
) : SyntaxAnalyserFromAsmTransformAbstract<Asm, AsmValue>(
    typesDomain,
    asmTransformDomain,
    relevantTrRuleSet,
    AsmFactorySimple(typesDomain, IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS))
) {
    //companion object {
    //    private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
   //     const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"
    //}

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<Asm>> = lazyMap { embGramQName ->
        val ruleSetQname = embGramQName
        SyntaxAnalyserSimple(typesDomain, asmTransformDomain, ruleSetQname)//, this.scopeModel) //TODO: needs embedded asmTransform
    }

}