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
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.CastExpression
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.CreateTupleExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.CreateObjectExpressionSimple
import net.akehurst.language.expressions.asm.RootExpressionSimple
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.grammarTypemodel.asm.GrammarTypeNamespaceSimple
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.transform.asm.TransformDomainDefault
import net.akehurst.language.transform.asm.TransformationRuleDefault
import net.akehurst.language.transform.asm.transformationRule
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.PropertyDeclarationStored
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.util.cached

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

    private val transformFromGrammar = cached {
        val res = TransformDomainDefault.fromGrammarModel(_grammarModel)
        _issues.addAll(res.issues)
        res.asm!!
    }

    override fun clear() {
        _issues.clear()
        _context = null
        __asm = null
        transformFromGrammar.reset()
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

                    for (ref in trs.extends) {
                        val resolved = asm.resolveReference(ref)
                        when (resolved) {
                            null -> _issues.error(null, "Cannot resolve '${ref.nameOrQName}' in context '${ref.localNamespace.qualifiedName}'")
                        }
                    }
                    handleRulesThatSimplyRedefineDefaultType(trs)
                    for (trr in trs.rules.values) {
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

    private val OptionHolder.createTypes get() = this[OPTION_CREATE_TYPES] == "true"
    private val OptionHolder.overrideDefault get() = this[OPTION_OVERRIDE_DEFAULT] == "true"

    private fun handleRulesThatSimplyRedefineDefaultType(trs: TransformRuleSet) {
        val rulesToHandle = trs.rules.values.filter { it.expression is RootExpression && (it.expression as RootExpression).isSelf.not()  }
        when {
            trs.options.overrideDefault -> {
                rulesToHandle.forEach { trr ->
                    val expr = trr.expression as RootExpression
                    val pqn = expr.name.asPossiblyQualifiedName
                    val exprType = _typeModel.findFirstByPossiblyQualifiedOrNull(pqn)?.type() ?: StdLibDefault.NothingType
                    // expression is to simply redefine the type for the grammar rule
                    // actual trr expression should be same as default
                    val defRs = transformFromGrammar.value.findDefinitionOrNullByQualifiedName(trs.qualifiedName) ?: error("Should exist!")
                    val defRule = defRs.findOwnedTrRuleForGrammarRuleNamedOrNull(trr.grammarRuleName) ?: error("Should exist!")
                    val clonedTypeDeclOrCreated = findOrCloneFromDefaultTypeForTransformRule(pqn,trs,trr)
                    val defExpr = defRule.expression
                    val newExpr = when(defExpr) {
                        is CreateObjectExpression -> {
                            CreateObjectExpressionSimple(pqn,defExpr.arguments).also { it.propertyAssignments = defExpr.propertyAssignments }
                        }
                        else -> error("Unsupported ${defExpr::class.simpleName}")
                    }
                    val newRule = transformationRule(clonedTypeDeclOrCreated.type(), newExpr)
                    newRule.grammarRuleName = trr.grammarRuleName
                    trs.setRule(newRule)
                    //also ensure properties are transferred
                    val defDecl = defRule.resolvedType.declaration
                    when {
                        clonedTypeDeclOrCreated is StructuredType -> {
                            val tocopy = defDecl.property.filter {
                                it is PropertyDeclarationStored && null == exprType.declaration.findOwnedPropertyOrNull(it.name)
                            }
                            tocopy.forEach { clonedTypeDeclOrCreated.appendPropertyStored(it.name, it.typeInstance, it.characteristics) }
                        }
                    }
                }
            }
            else -> when {
                rulesToHandle.isNotEmpty() -> {
                    _issues.error(null,"Transformation rules in '${trs.qualifiedName}' rewrite their type, but '${OPTION_OVERRIDE_DEFAULT}' is not specified")
                }
            }
        }
    }

    private fun typeFor(trs: TransformRuleSet, trr: TransformationRule): TypeInstance {
        // A TrRule needs to know the target type because this type is used
        // to define the type for a grammar Rule
        // thus only certain expressions are valid.
        val expr = trr.expression
        val (exprPqn, exprType) = when {
            trr.isResolved -> Pair(trr.resolvedType.qualifiedTypeName, trr.resolvedType)
            expr is CreateObjectExpression -> Pair(expr.possiblyQualifiedTypeName, _exprTypeResolver.typeFor(expr, StdLibDefault.AnyType))
            expr is CreateTupleExpression -> Pair(StdLibDefault.TupleType.qualifiedName, _exprTypeResolver.typeFor(expr, StdLibDefault.AnyType))
            expr is CastExpression -> Pair(expr.targetType.possiblyQualifiedName, _exprTypeResolver.typeFor(expr, StdLibDefault.AnyType))
            else -> error("Unsupported expression type: $expr")
        }
        //val trResultType = _exprTypeResolver.typeFor(trr.expression)
        // val existingDecl = _typeModel.findFirstByPossiblyQualifiedOrNull(exprPqn)
        return when (exprType) {
            StdLibDefault.NothingType -> when {
                trs.options.createTypes -> when {
                    trs.options.overrideDefault -> {
                        // use default type if it exists, else error because it should exist!
                        val clonedTypeDeclOrCreated = findOrCloneFromDefaultTypeForTransformRule(exprPqn,trs,trr)

                        // trr was resolved when created, thus type may not be in the ns
                        //val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace? ?: error("Should exist!")
                        val decl = _typeModel.findFirstByPossiblyQualifiedOrNull(exprPqn)
                        if (null == decl) {
                            val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace? ?: error("Should exist!")
                            gtns.addDefinition(exprType.declaration)
                            gtns.setTypeForGrammarRule(trr.grammarRuleName, exprType)
                        }
                        clonedTypeDeclOrCreated.type()
                    }

                    else -> {
                        val tns = _typeModel.findOrCreateNamespace(trs.qualifiedName, listOf(StdLibDefault.qualifiedName.asImport))
                        val decl = tns.findOwnedOrCreateDataTypeNamed(exprPqn.simpleName)
                        decl.type()
                    }
                }

                else -> {
                    _issues.error(null, "Type '${exprPqn}' is not found, using type 'std.Any'.")
                    StdLibDefault.AnyType
                }
            }

            else -> {
                // trr was resolved when created, thus type may not be in the ns
                //val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace? ?: error("Should exist!")
                val decl = _typeModel.findFirstByPossiblyQualifiedOrNull(exprPqn)
                if (null == decl) {
                    val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace? ?: error("Should exist!")
                    gtns.addDefinition(exprType.declaration)
                    gtns.setTypeForGrammarRule(trr.grammarRuleName, exprType)
                }
                exprType
            }

        }
    }

    private fun findOrCloneFromDefaultTypeForTransformRule(typePqn:PossiblyQualifiedName, trs:TransformRuleSet, trr: TransformationRule):TypeDefinition {
        val grmDecl = transformFromGrammar.value.typeModel?.findFirstByPossiblyQualifiedOrNull(typePqn)
        val clonedTypeDeclOrCreated = grmDecl?.cloneTo(_typeModel) ?: let {
            // need a grammarTypeNamespace
            val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace?
                ?: GrammarTypeNamespaceSimple.findOrCreateGrammarNamespace(_typeModel, trs.qualifiedName).also { gns ->
                    //TODO:add imports
                }
            val t = gtns.findOwnedOrCreateDataTypeNamed(typePqn.simpleName)
            gtns.setTypeForGrammarRule(trr.grammarRuleName, t.type())
            t
        }
        return clonedTypeDeclOrCreated
    }

    private fun overrideDefaultNamespace(ns: TransformNamespace) {
        val nsOpt = ns.options[OPTION_OVERRIDE_DEFAULT]
        if (null != nsOpt && nsOpt == "true") {
            val grmNs = transformFromGrammar.value.findNamespaceOrNull(ns.qualifiedName)
            when (grmNs) {
                null -> _issues.warn(null, "No default namespace with name '${ns.qualifiedName}' found")
                else -> {
                    for (grmTrs in grmNs.definition) {
                        when {
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
            val grmRs = transformFromGrammar.value.findNamespaceOrNull(trs.namespace.qualifiedName)?.findDefinitionOrNull(trs.name)
            when (grmRs) {
                null -> _issues.warn(null, "No default ruleset with name '${trs.qualifiedName}' found")
                else -> {
                    for (grmRl in grmRs.rules.values) {
                        when {
                            trs.rules.containsKey(grmRl.grammarRuleName) -> Unit // overridden, do not copy default
                            else -> trs.setRule(grmRl)
                        }
                    }
                }
            }
        }
    }
}