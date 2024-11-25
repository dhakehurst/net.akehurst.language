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

import net.akehurst.language.agl.expressions.processor.ExpressionTypeResolver
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.expressions.api.CastExpression
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.CreateTupleExpression
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.transform.asm.TransformDomainDefault
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
    private val _exprTypeResolver get() = ExpressionTypeResolver(_typeModel, _issues)

    private val transformFromGrammar by lazy {
        TransformDomainDefault.fromGrammarModel(_grammarModel)
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
            (asm as TransformDomainDefault).typeModel = context.typeModel
            for (trns in asm.namespace) {
                overrideDefaultNamespace(trns) //if option is set

                for (trs in trns.definition) {
                    overrideDefaultRuleSet(trs)  //if option is set

                    for(ref in trs.extends) {
                         val resolved = asm.resolveReference(ref)
                        when(resolved) {
                            null -> _issues.error(null,"Cannot resolve '${ref.nameOrQName}' in context '${ref.localNamespace.qualifiedName}'")
                        }
                    }

                    for ((grn, trr) in trs.rules) {
                        val ti = typeFor(trs, trr) // always fetch the type, so that it gets created if need be TODO: maybe do type creation in separate phase!
                        (trr as TransformationRuleDefault).resolveTypeAs(ti)
                    }
                }
            }
            // in case new namespaces/types were created TODO: maybe only if OPTION_CREATE_TYPES specified
            _typeModel.resolveImports()
        }
        return SemanticAnalysisResultDefault(_issues)
    }

    private val TransformRuleSet.optionCreateTypes: Boolean
        get() {
            val opt = this.options[OPTION_CREATE_TYPES]
            return when {
                null == opt -> false
                else -> opt == "true"
            }
        }

    private fun typeFor(trs: TransformRuleSet, trr: TransformationRule): TypeInstance {
        // A TrRule needs to know the target type because this type is used
        // to define the type for a grammar Rule
        // thus only certain expressions are valid.
        val expr = trr.expression
        val exprPqn = when {
            trr.isResolved -> trr.resolvedType.qualifiedTypeName
            expr is CreateObjectExpression -> expr.possiblyQualifiedTypeName
            expr is CreateTupleExpression -> SimpleTypeModelStdLib.TupleType.qualifiedName
            expr is CastExpression -> expr.targetTypeName
            else -> error("Unsupported expression type: $expr")
        }
        //val trResultType = _exprTypeResolver.typeFor(trr.expression)
        val existingDecl = _typeModel.findFirstByPossiblyQualifiedOrNull(exprPqn)
        return when (existingDecl) {
            null -> when {
                trs.optionCreateTypes -> {
                    val tns = _typeModel.findOrCreateNamespace(trs.qualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName.asImport))
                    val decl = tns.findOwnedOrCreateDataTypeNamed(exprPqn.simpleName)
                    decl.type()
                }

                else -> {
                    _issues.error(null, "Type '${exprPqn}' is not found, using type 'std.Any'.")
                    SimpleTypeModelStdLib.AnyType
                }
            }

            else -> existingDecl.type()
        }
    }

    private fun overrideDefaultNamespace(ns: TransformNamespace) {
        val nsOpt = ns.options[OPTION_OVERRIDE_DEFAULT]
        if (null != nsOpt && nsOpt == "true") {
            val grmNs = transformFromGrammar.asm!!.findNamespaceOrNull(ns.qualifiedName)
            when (grmNs) {
                null -> _issues.warn(null, "No default namespace with name '${ns.qualifiedName}' found")
                else -> {
                    for (grmTrs in grmNs.definition) {
                        when{
                            ns.definitionByName.containsKey(grmTrs.name) -> Unit // overriden do not copy default
                            else -> grmTrs.cloneTo(ns)
                        }
                    }
                }
            }
        }
    }

    private fun overrideDefaultRuleSet(trs: TransformRuleSet) {
        val opt = trs.options[OPTION_OVERRIDE_DEFAULT]
        if (null != opt && opt == "true") {
            val grmRs = transformFromGrammar.asm!!.findNamespaceOrNull(trs.namespace.qualifiedName)?.findDefinitionOrNull(trs.name)
            when (grmRs) {
                null -> _issues.warn(null, "No default ruleset with name '${trs.qualifiedName}' found")
                else -> {
                    for (grmRl in grmRs.rules.values) {
                        when {
                            trs.rules.containsKey(grmRl.grammarRuleName) -> Unit // overriden, do not copy default
                            else -> trs.addRule(grmRl)
                        }
                    }
                }
            }
        }
    }
}