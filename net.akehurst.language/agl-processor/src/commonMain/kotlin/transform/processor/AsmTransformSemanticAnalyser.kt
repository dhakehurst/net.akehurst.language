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

package net.akehurst.language.transform.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.base.api.Option
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.transform.asm.TransformModelDefault
import net.akehurst.language.transform.asm.TransformationRuleDefault
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

class AsmTransformSemanticAnalyser() : SemanticAnalyser<TransformModel, ContextFromGrammarAndTypeModel> {

    companion object {
        const val OPTION_CREATE_TYPES = "create-missing-types"
        const val OPTION_OVERRIDE_DEFAULT = "override-default-transform"
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

    private var _context: ContextFromGrammarAndTypeModel? = null
    private var __asm: TransformModel? = null
    private val _asm: TransformModel get() = __asm!!
    private val _typeModel: TypeModel get() = _context!!.typeModel
    private val _grammarModel: GrammarModel get() = _context!!.grammarModel
    private val _options = mutableMapOf<String, Option>()

    private val transformFromGrammar by lazy {
        TransformModelDefault.fromGrammarModel(_grammarModel)
    }

    override fun clear() {
        _issues.clear()
        _context = null
        __asm = null
    }

    override fun analyse(
        asm: TransformModel,
        locationMap: Map<Any, InputLocation>?,
        context: ContextFromGrammarAndTypeModel?,
        options: SemanticAnalysisOptions<ContextFromGrammarAndTypeModel>
    ): SemanticAnalysisResult {
        _context = context
        __asm = asm
        if (null == context) {
            _issues.warn(null, "No context, semantic analysis cannot be performed")
        } else {
            (asm as TransformModelDefault).typeModel = context.typeModel
            for (trns in asm.namespace) {
                for (trs in trns.definition) {

                    val opt = trs.options.lastOrNull { it.name == OPTION_OVERRIDE_DEFAULT }
                    if (null != opt && opt.value=="true") {
                        overrideDefaultRuleSet(trs)
                    }

                    for ((grn, trr) in trs.rules) {
                        val ti = typeFor(trs,trr)
                        (trr as TransformationRuleDefault).resolveTypeAs(ti)
                    }
                }
            }
            // in case new namespaces/types were created TODO: maybe only if OPTION_CREATE_TYPES specified
            _typeModel.resolveImports()
        }
        return SemanticAnalysisResultDefault(_issues)
    }

    private val TransformationRule.optionCreateTypes: Boolean get() {
        val opt = _asm.options.lastOrNull { it.name == OPTION_CREATE_TYPES }
        return when {
            null == opt -> false
            else -> opt.value == "true"
        }
    }

    private fun typeFor(trs:TransformRuleSet, trr: TransformationRule): TypeInstance {
        val existingDecl = _typeModel.findFirstByPossiblyQualifiedOrNull(trr.possiblyQualifiedTypeName)
        return when(existingDecl) {
            null -> when {
                trr.optionCreateTypes -> {
                    val tns = _typeModel.findOrCreateNamespace(trs.qualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName.asImport))
                    val decl = tns.findOwnedOrCreateDataTypeNamed(trr.possiblyQualifiedTypeName.simpleName)
                    decl.type()
                }
                else -> {
                    _issues.error(null, "Type '${trr.possiblyQualifiedTypeName}' is not found, using type 'std.Any'.")
                    SimpleTypeModelStdLib.AnyType
                }
            }
            else -> existingDecl.type()
        }
    }

    private fun overrideDefaultRuleSet(overridingRuleSet: TransformRuleSet) {
        val grmRs = transformFromGrammar.asm!!.findNamespaceOrNull(overridingRuleSet.namespace.qualifiedName)?.findDefinitionOrNull(overridingRuleSet.name)
        when (grmRs) {
            null -> _issues.warn(null, "No default ruleset with name '${overridingRuleSet.qualifiedName}' found")
            else -> {
                for(grmRl in grmRs.rules.values) {
                    when {
                        overridingRuleSet.rules.containsKey(grmRl.grammarRuleName) -> Unit // overriden, do not copy
                        else -> {
                            overridingRuleSet.addRule(grmRl)
                        }
                    }
                }
            }
        }
    }
}