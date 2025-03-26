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
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.CastExpression
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.CreateTupleExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.CreateObjectExpressionDefault
import net.akehurst.language.expressions.processor.ExpressionTypeResolver
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
import net.akehurst.language.typemodel.asm.ParameterDefinitionSimple
import net.akehurst.language.typemodel.asm.PropertyDeclarationStored
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.util.cached

class AsmTransformSemanticAnalyser() : SemanticAnalyser<TransformModel, ContextFromGrammarAndTypeModel> {

    companion object {
        const val OPTION_CREATE_TYPES = "create-missing-types"
        const val OPTION_OVERRIDE_DEFAULT = "override-default-transform"

        private val OptionHolder.createTypes get() = this[OPTION_CREATE_TYPES] == "true"
        private val OptionHolder.overrideDefault get() = this[OPTION_OVERRIDE_DEFAULT] == "true"

    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

    private var _context: ContextFromGrammarAndTypeModel? = null
    private var __asm: TransformModel? = null
    private val _asm: TransformModel get() = __asm!!
    private val _typeModel: TypeModel get() = _context!!.typeModel
    private val _grammarModel: GrammarModel get() = _context!!.grammarModel

    private val transformFromGrammar = cached {
        val res = TransformDomainDefault.fromGrammarModel(_grammarModel)
        _issues.addAllFrom(res.issues)
        res.asm!!
    }

    override fun clear() {
        _issues.clear()
        _context = null
        __asm = null
        transformFromGrammar.reset()
    }

    override fun analyse(
        sentenceIdentity:Any?,
        asm: TransformModel,
        locationMap: Map<Any, InputLocation>?,
        options: SemanticAnalysisOptions<ContextFromGrammarAndTypeModel>
    ): SemanticAnalysisResult {
        _context = options.context
        __asm = asm
        if (null == _context) {
            _issues.warn(null, "No context, semantic analysis cannot be performed")
        } else {
            (asm as TransformDomainDefault).typeModel = _typeModel
            for (trns in asm.namespace) {
                overrideDefaultNamespace(trns)
                for (trs in trns.definition) {
                    createMissingTypeNamespaces(trs)
                }
            }
            _typeModel.resolveImports()

            for (trns in asm.namespace) {
                for (trs in trns.definition) {
                    overrideDefaultRuleSet(trs)
                    convertRulesThatSimplyRedefineDefaultType(trs)
                    createMissingTypes(trs)

                    for (ref in trs.extends) {
                        val resolved = asm.resolveReference(ref)
                        when (resolved) {
                            null -> _issues.error(null, "Cannot resolve '${ref.nameOrQName}' in context '${ref.localNamespace.qualifiedName}'")
                        }
                    }
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

    /**
     * converts nonTerm: TypeRef  =>  nonTerm: TypeRef() { ... <prop assignments from default> ...  }
     */
    private fun convertRulesThatSimplyRedefineDefaultType(trs: TransformRuleSet) {
        val rulesToConvert = trs.rules.values.filter { it.expression is RootExpression && (it.expression as RootExpression).isSelf.not() }
        when {
            trs.options.overrideDefault -> {
                rulesToConvert.forEach { trr ->
                    val expr = trr.expression as RootExpression
                    val pqn = expr.name.asPossiblyQualifiedName
                    val exprType = _typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(pqn)?.type() ?: StdLibDefault.NothingType
                    // expression is to simply redefine the type for the grammar rule
                    // actual trr expression should be same as default
                    val defRs = transformFromGrammar.value.findDefinitionByQualifiedNameOrNull(trs.qualifiedName) ?: error("Should exist!")
                    val defRule = defRs.findOwnedTrRuleForGrammarRuleNamedOrNull(trr.grammarRuleName) ?: error("Should exist!")
                    val clonedTypeDeclOrCreated = findOrCloneFromDefaultTypeForTransformRule(pqn, trs, trr)
                    val defExpr = defRule.expression
                    val newExpr = when (defExpr) {
                        is CreateObjectExpression -> {
                            CreateObjectExpressionDefault(pqn, defExpr.constructorArguments).also { it.propertyAssignments = defExpr.propertyAssignments }
                        }

                        else -> error("Unsupported ${defExpr::class.simpleName}")
                    }
                    val newRule = transformationRule(clonedTypeDeclOrCreated.type(), newExpr)
                    newRule.grammarRuleName = trr.grammarRuleName
                    trs.setRule(newRule)
                    //also ensure properties are transferred
                    val defDecl = defRule.resolvedType.resolvedDeclaration
                    when {
                        clonedTypeDeclOrCreated is StructuredType -> {
                            val tocopy = defDecl.property.filter {
                                it is PropertyDeclarationStored && null == exprType.resolvedDeclaration.findOwnedPropertyOrNull(it.name)
                            }
                            tocopy.forEach { clonedTypeDeclOrCreated.appendPropertyStored(it.name, it.typeInstance, it.characteristics, it.index) }
                        }
                    }
                }
            }

            else -> when {
                rulesToConvert.isNotEmpty() -> {
                    _issues.error(null, "Transformation rules in '${trs.qualifiedName}' rewrite their type, but '${OPTION_OVERRIDE_DEFAULT}' is not specified")
                }
            }
        }
    }

    private fun typeFor(trs: TransformRuleSet, trr: TransformationRule): TypeInstance {
        // A TrRule needs to know the target type because this type is used
        // to define the type for a grammar Rule
        // thus only certain expressions are valid.
        val expr = trr.expression
        val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace? ?: error("Should exist!")
        val exprTypeResolver = ExpressionTypeResolver(_typeModel, gtns, _issues)
        val exprType = when {
            trr.isResolved -> trr.resolvedType
            expr is CreateObjectExpression -> exprTypeResolver.typeFor(expr, StdLibDefault.AnyType)
            expr is CreateTupleExpression -> exprTypeResolver.typeFor(expr, StdLibDefault.AnyType)
            expr is CastExpression -> exprTypeResolver.typeFor(expr, StdLibDefault.AnyType)
            else -> error("Unsupported expression type: $expr")
        }
        return exprType
    }

    /**
     * find type for 'typePqn' in default-transform-from-grammar
     * if it exists, clone it into type-domain for this transform
     * else create or find grammar-type-namespace and create new datatype for 'typePqn'
     */
    private fun findOrCloneFromDefaultTypeForTransformRule(typePqn: PossiblyQualifiedName, trs: TransformRuleSet, trr: TransformationRule): TypeDefinition {
        val grmDecl = transformFromGrammar.value.typeModel?.findFirstDefinitionByPossiblyQualifiedNameOrNull(typePqn)
        val clonedTypeDeclOrCreated = grmDecl?.findInOrCloneTo(_typeModel) ?: let {
            // need a grammarTypeNamespace
            val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace? ?: error("Should exist!")
            val t = gtns.findOwnedOrCreateDataTypeNamed(typePqn.simpleName)
            // no need to setTypeForGrammarRule, it is done later
            // gtns.setTypeForGrammarRule(trr.grammarRuleName, t.type())
            t
        }
        // if type was cloned (in findInOrCloneTo), it will not have had the tyoe for grammar rule set
        when (clonedTypeDeclOrCreated.namespace) {
            is GrammarTypeNamespace -> {
                //TODO: should really use clone of type from original!
                (clonedTypeDeclOrCreated.namespace as GrammarTypeNamespace).setTypeForGrammarRule(trr.grammarRuleName, clonedTypeDeclOrCreated.type())
            }
        }

        return clonedTypeDeclOrCreated
    }

    private fun overrideDefaultNamespace(ns: TransformNamespace) {
        when {
            ns.options.overrideDefault -> {
                val grmNs = transformFromGrammar.value.findNamespaceOrNull(ns.qualifiedName)
                when (grmNs) {
                    null -> _issues.error(null, "No default namespace with name '${ns.qualifiedName}' found, can't override it")
                    else -> {
                        for (grmTrs in grmNs.definition) {
                            when {
                                ns.definitionByName.containsKey(grmTrs.name) -> Unit // overridden do not copy default
                                else -> {
                                    // create expected TransformRuleSet,
                                    // just create empty because rules maybe overridden - covered in overrideDefaultRuleSet
                                    ns.createOwnedTransformRuleSet(grmTrs.name, grmTrs.extends.map { it.cloneTo(ns) }, OptionHolderDefault())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun findOrCreateGrammarTypeNamespace(qn: QualifiedName) {
        _typeModel.findNamespaceOrNull(qn) as GrammarTypeNamespace?
            ?: GrammarTypeNamespaceSimple.findOrCreateGrammarNamespace(_typeModel, qn).also { tns ->
                transformFromGrammar.value.typeModel?.findNamespaceOrNull(tns.qualifiedName)?.let { gtns ->
                    gtns.import.forEach { tns.addImport(it) }
                }
                _typeModel.addNamespace(tns)
            }
    }

    private fun createMissingTypeNamespaces(trs: TransformRuleSet) {
        when {
            trs.options.createTypes -> findOrCreateGrammarTypeNamespace(trs.qualifiedName)
        }
    }

    private fun overrideDefaultRuleSet(trs: TransformRuleSet) {
        when {
            trs.options.overrideDefault -> {
                val grmRs = transformFromGrammar.value.findNamespaceOrNull(trs.namespace.qualifiedName)?.findDefinitionOrNull(trs.name)
                when (grmRs) {
                    null -> _issues.error(null, "No default ruleset with name '${trs.qualifiedName}' found")
                    else -> {
                        for (grmRl in grmRs.rules.values) {
                            when {
                                trs.rules.containsKey(grmRl.grammarRuleName) -> Unit  // overridden, do not copy default rule
                                else -> {
                                    // rule not overridden so use default rule
                                    trs.setRule(grmRl)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createMissingTypes(trs: TransformRuleSet) {
        when {
            trs.options.createTypes -> {
                for (trr in trs.rules.values) {
                    val expr = trr.expression
                    val exprPqn = when {
                        trr.isResolved -> trr.resolvedType.qualifiedTypeName
                        expr is CreateObjectExpression -> {
                            val gtns = _typeModel.findNamespaceOrNull(trs.qualifiedName) as GrammarTypeNamespace? ?: error("Should exist!")
                            val exprTypeResolver = ExpressionTypeResolver(_typeModel, gtns, _issues)
                            val t = gtns.findOwnedOrCreateDataTypeNamed(expr.possiblyQualifiedTypeName.simpleName)

                            val params = expr.constructorArguments.map { ass ->
                                val pName = net.akehurst.language.typemodel.api.ParameterName(ass.lhsPropertyName)
                                var pType = exprTypeResolver.typeFor(ass.rhs, AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE)
                                if (pType == StdLibDefault.NothingType) {
                                    pType = StdLibDefault.AnyType
                                }
                                ParameterDefinitionSimple(pName,pType, null)
                            }
                            t.addConstructor(params)

                            expr.propertyAssignments.forEach { ass ->
                                val propName = PropertyName(ass.lhsPropertyName)
                                var propType = exprTypeResolver.typeFor(ass.rhs, AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE)
                                if (propType == StdLibDefault.NothingType) {
                                    propType = StdLibDefault.AnyType
                                }
                                val existingProp = t.findOwnedPropertyOrNull(propName)
                                when {
                                    null == existingProp -> {
                                        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
                                        t.appendPropertyStored(propName, propType, characteristics, ass.lhsGrammarRuleIndex?: -1)
                                    }

                                    else -> when {
                                        existingProp.typeInstance == propType -> {
                                            //all ok
                                        }

                                        propType.conformsTo(existingProp.typeInstance) -> {
                                            // also OK
                                        }
                                        // existingProp.typeInstance.conformsTo(propType) -> {
                                        //     // need to change type of prop to propType
                                        //     //TODO
                                        // }
                                        else -> _issues.error(
                                            null,
                                            "Trying to create missing property '${propName}: ${propType.qualifiedTypeName}', '${t.qualifiedName}' already contains incompatible property '${existingProp.name}: ${existingProp.typeInstance.qualifiedTypeName}'."
                                        )
                                    }
                                }
                            }
                            expr.possiblyQualifiedTypeName
                        }

                        expr is CreateTupleExpression -> StdLibDefault.TupleType.qualifiedName
                        expr is CastExpression -> expr.targetType.possiblyQualifiedName
                        else -> error("Unsupported expression type: $expr")
                    }
                    findOrCloneFromDefaultTypeForTransformRule(exprPqn, trs, trr)
                }
            }
        }
    }
}