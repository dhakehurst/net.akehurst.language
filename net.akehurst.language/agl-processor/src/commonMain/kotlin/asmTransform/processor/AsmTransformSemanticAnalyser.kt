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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.api.GrammarRule
import net.akehurst.language.base.api.isSimpleName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.agl.simple.Grammar2TypesDomainMapping
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet
import net.akehurst.language.asmTransform.api.AsmTransformNamespace
import net.akehurst.language.asmTransform.api.AsmTransformRuleSet
import net.akehurst.language.asmTransform.api.AsmTransformationRule
import net.akehurst.language.asmTransform.api.ModifyObjectRule
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import net.akehurst.language.asmTransform.asm.AsmTransformationRuleDefault
import net.akehurst.language.asmTransform.asm.asmTransformationRule
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
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammarTypemodel.api.GrammarTypesNamespace
import net.akehurst.language.grammarTypemodel.asm.GrammarTypesNamespaceSimple
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.ParameterDefinitionSimple
import net.akehurst.language.types.asm.PropertyDeclarationStored
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.util.cached

class AsmTransformSemanticAnalyser() : SemanticAnalyser<AsmTransformDomain, SentenceContext> {

    companion object {
        const val OPTION_CREATE_TYPES = "create-missing-types"
        const val OPTION_OVERRIDE_DEFAULT = "override-default-transform"

        private val OptionHolder.createTypes:Boolean get() = this[OPTION_CREATE_TYPES] ?: false
        private val OptionHolder.overrideDefault:Boolean get() = this[OPTION_OVERRIDE_DEFAULT]  ?: false

    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _resolvedReferences = mutableListOf<ResolvedReference>()

    private var _context: SentenceContext? = null
    private var _locationMap: LocationMap? = null
    // written rules for a grammar rule the grammar does not have (error given), their type is not computed
    private val _rulesForUnknownGrammarRule = mutableSetOf<AsmTransformationRule>()
    // type-only rules that cannot be converted (error given), they are not typed
    private val _typeOnlyRulesInError = mutableSetOf<AsmTransformationRule>()
    private var __asm: AsmTransformDomain? = null
    private val _asm: AsmTransformDomain get() = __asm!!
    //TODO: rework to allow use of proper items in context
    private val _typesDomain: TypesDomain get() = _context!!.findItemsNamedConformingTo("types") { _ -> true }.first().item as TypesDomain
    private val _grammarDomain: GrammarDomain get() = _context!!.findItemsNamedConformingTo("grammar") { _ -> true }.first().item as GrammarDomain

    // a type-only rule 'r: T' (with '#override-default-transform') renames the type of 'r' to 'T' (AT-Q2 B),
    // so the default transform is generated with 'T' as the type name of 'r', making 'T' the type wherever the default uses the type of 'r'
    private val transformFromGrammar = cached {
        val res = AsmTransformDomainDefault.fromGrammarDomain(_grammarDomain, configuration = TypeNamesForTypeOnlyRules(typeOnlyRuleNames()))
        _issues.addAllFrom(res.allIssues)
        res.asm!!
    }

    override fun clear() {
        _issues.clear()
        _resolvedReferences.clear()
        _context = null
        _locationMap = null
        _rulesForUnknownGrammarRule.clear()
        _typeOnlyRulesInError.clear()
        __asm = null
        transformFromGrammar.reset()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: AsmTransformDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        _context = options.sentenceContext
        _locationMap = locationMap
        __asm = asm
        if (null == _context) {
            _issues.warn(null, "No sentence context provided, semantic analysis cannot be performed")
        } else {
            (asm as AsmTransformDomainDefault).typesDomain = _typesDomain
            for (trns in asm.namespace) {
                for (trs in trns.definition) {
                    checkRulesNameGrammarRules(trs)
                }
            }
            for (trns in asm.namespace) {
                overrideDefaultNamespace(trns)
                for (trs in trns.definition) {
                    createMissingTypeNamespaces(trs)
                }
            }
            _typesDomain.resolveImports()

            for (trns in asm.namespace) {
                for (trs in trns.definition) {
                    overrideDefaultRuleSet(trs)
                    checkEveryRuleHasATransformRule(trs)
                    convertRulesThatSimplyRedefineDefaultType(trs)
                    resolveModifyRules(trs)
                    createMissingTypes(trs)

                    for (ref in trs.extends) {
                        val resolved = asm.resolveReference(ref)
                        when (resolved) {
                            null -> _issues.error(_locationMap?.get(trs), "Cannot resolve '${ref.nameOrQName}' in the context of '${ref.localNamespace.qualifiedName}'")
                        }
                    }
                    for (trr in trs.rules.values) {
                        if (trr in _rulesForUnknownGrammarRule || trr in _typeOnlyRulesInError) {
                            trr.resolveTypeAs(StdLibDefault.NothingType)
                            continue
                        }
                        val ti = typeFor(trs, trr) // always fetch the type, so that it gets created if need be TODO: maybe do type creation in separate phase!
                        trr.resolveTypeAs(ti)
                    }
                    checkCreatedObjectProperties(trs)
                }
            }
            // in case new namespaces/types were created TODO: maybe only if OPTION_CREATE_TYPES specified
            _typesDomain.resolveImports()
        }
        return SemanticAnalysisResultDefault(_resolvedReferences, _issues)
    }

    /**
     * maps a grammar rule's type name to the name given by a type-only rule 'r: T',
     * other rules get the default name
     */
    private class TypeNamesForTypeOnlyRules(
        val names: Map<Pair<QualifiedName, GrammarRuleName>, SimpleName>
    ) : Grammar2TypesDomainMapping by Grammar2TransformRuleSet.defaultConfiguration {
        override fun typeNameFor(rule: GrammarRule): SimpleName =
            names[Pair(rule.grammar.qualifiedName, rule.name)] ?: Grammar2TransformRuleSet.defaultConfiguration.typeNameFor(rule)
    }

    /**
     * the type-only rules 'r: T' with a simple name 'T' in rule sets with '#override-default-transform'.
     * A rule is left out (with an error) if 'T' is the default type name of another rule of the grammar,
     * because the two types would become one.
     */
    private fun typeOnlyRuleNames(): Map<Pair<QualifiedName, GrammarRuleName>, SimpleName> {
        val result = mutableMapOf<Pair<QualifiedName, GrammarRuleName>, SimpleName>()
        for (trns in _asm.namespace) {
            for (trs in trns.definition) {
                if (trs.options.overrideDefault.not()) continue
                val grammar = _grammarDomain.findDefinitionByQualifiedNameOrNull(trs.qualifiedName) ?: continue
                for (trr in typeOnlyRules(trs)) {
                    val name = (trr.expression as RootExpression).name
                    if (name.isSimpleName.not()) continue // not expected, a qualified name is parsed as a navigation
                    val tn = SimpleName(name)
                    val other = grammar.resolvedGrammarRule.firstOrNull {
                        it.name != trr.grammarRuleName && it.isLeaf.not() && it.isSkip.not() && Grammar2TransformRuleSet.defaultConfiguration.typeNameFor(it) == tn
                    }
                    when (other) {
                        null -> result[Pair(grammar.qualifiedName, trr.grammarRuleName)] = tn
                        else -> {
                            _typeOnlyRulesInError.add(trr)
                            _issues.error(
                                locationOfRuleName(trr),
                                "Cannot change the type of rule '${trr.grammarRuleName.value}' to '${tn.value}', it is the type of rule '${other.name.value}'"
                            )
                        }
                    }
                }
            }
        }
        return result
    }

    /** the written rules 'r: T' that only give the type */
    private fun typeOnlyRules(trs: AsmTransformRuleSet) =
        trs.rules.values.filter { it.expression is RootExpression && (it.expression as RootExpression).isSelf.not() && it !in _rulesForUnknownGrammarRule }

    /**
     * every written rule must be for a grammar rule of the grammar the rule set is for (AT-Q1).
     * Checked before the default rules are added, so only the written rules are checked.
     */
    private fun checkRulesNameGrammarRules(trs: AsmTransformRuleSet) {
        val grammar = _grammarDomain.findDefinitionByQualifiedNameOrNull(trs.qualifiedName)
        when (grammar) {
            // the namespace and name of a rule set are those of its grammar (AT-Q3)
            null -> _issues.error(
                _locationMap?.get(trs),
                "No grammar '${trs.qualifiedName.value}' for asm-transform '${trs.name.value}' in namespace '${trs.namespace.qualifiedName.value}', the namespace and name of an asm-transform must be those of its grammar (grammars: ${_grammarDomain.allDefinitions.joinToString { it.qualifiedName.value }})"
            )

            else -> trs.rules.values.forEach { trr ->
                if (null == grammar.findAllResolvedGrammarRule(trr.grammarRuleName)) {
                    _rulesForUnknownGrammarRule.add(trr)
                    _issues.error(
                        locationOfRuleName(trr),
                        "Grammar '${grammar.qualifiedName.value}' has no rule '${trr.grammarRuleName.value}' (in the transformation rule for '${trr.grammarRuleName.value}')"
                    )
                }
            }
        }
    }

    /** the location of the grammar rule name at the start of a written transformation rule, null if not known */
    private fun locationOfRuleName(trr: AsmTransformationRule): InputLocation? =
        _locationMap?.getPropertyLocation(trr, "grammarRuleName")

    /**
     * Without '#override-default-transform' the transform has only the rules written,
     * so every (non leaf, non skip) rule of the grammar needs a transformation rule.
     */
    private fun checkEveryRuleHasATransformRule(trs: AsmTransformRuleSet) {
        when {
            trs.options.overrideDefault -> Unit
            else -> {
                val grammar = _grammarDomain.findDefinitionByQualifiedNameOrNull(trs.qualifiedName)
                when (grammar) {
                    null -> Unit // no grammar for this rule set, nothing to check
                    else -> {
                        val extendedRules = trs.extends.mapNotNull { _asm.resolveReference(it) }.flatMap { it.rules.keys }.toSet()
                        val missing = grammar.resolvedGrammarRule
                            .filter { it.isLeaf.not() && it.isSkip.not() }
                            .filter { trs.rules.containsKey(it.name).not() && extendedRules.contains(it.name).not() }
                        missing.forEach { gr ->
                            _issues.error(
                                _locationMap?.get(trs),
                                "No transformation rule for grammar rule '${gr.name.value}' in '${trs.qualifiedName.value}', write one or use '#${OPTION_OVERRIDE_DEFAULT}'"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * a type-only rule 'r: T' keeps the default rule of 'r' and changes only its type (AT-Q2 B):
     * - 'T' a simple name: the type of 'r' is renamed to 'T' (done when the default transform is generated, see [transformFromGrammar]),
     *   so e.g. for a choice 'T' is the supertype of the alternatives, and the rule stays 'child[0]'
     * ('T' is always a simple name: a qualified name is parsed as a navigation expression, not as a type-only rule)
     * - an error if the default has no type of its own to rename (e.g. a leaf, or a choice of leaves, is a String)
     */
    private fun convertRulesThatSimplyRedefineDefaultType(trs: AsmTransformRuleSet) {
        val rulesToConvert = typeOnlyRules(trs).filter { it !in _typeOnlyRulesInError }
        when {
            trs.options.overrideDefault -> {
                findOrCreateGrammarTypeNamespace(trs.qualifiedName) //ensure type namespace exists
                rulesToConvert.forEach { trr ->
                    val expr = trr.expression as RootExpression
                    val pqn = expr.name.asPossiblyQualifiedName
                    val defRs = transformFromGrammar.value.findDefinitionByQualifiedNameOrNull(trs.qualifiedName)
                    val defRule = defRs?.findOwnedTrRuleForGrammarRuleNamedOrNull(trr.grammarRuleName)
                    // no default rule: the grammar has no such rule (error given in checkRulesNameGrammarRules) or there is no default rule set (error given in overrideDefaultRuleSet)
                    if (null == defRule) return@forEach
                    val defDecl = defRule.resolvedType.resolvedDefinitionOrNull
                    val defExpr = defRule.expression
                    when {
                        // the default type was generated with the name given by the rule, use the default rule and its type
                        expr.name.isSimpleName && null != defDecl && defDecl.name == pqn.simpleName && (defDecl is DataType || defDecl is UnionType) -> {
                            trs.setRule(defRule)
                            findOrCloneFromDefaultTypeForTransformRule(defRule.resolvedType.qualifiedTypeName, trs, defRule)
                        }

                        defExpr is CreateObjectExpression -> retypeCreatedObject(trs, trr, pqn, defRule, defExpr)

                        else -> {
                            _typeOnlyRulesInError.add(trr)
                            _issues.error(
                                locationOfRuleName(trr),
                                "Cannot change only the type of rule '${trr.grammarRuleName.value}' to '${expr.name}', its default type is '${displayName(defRule.resolvedType)}', which is not a type of its own, use 'child[0] as ${expr.name}' or '${expr.name}() { ... }'"
                            )
                        }
                    }
                }
            }

            else -> when {
                rulesToConvert.isNotEmpty() -> {
                    _issues.error(locationOfRuleName(rulesToConvert.first()), "Transformation rules in '${trs.qualifiedName}' rewrite their type, but '${OPTION_OVERRIDE_DEFAULT}' is not specified")
                }
            }
        }
    }

    /** nonTerm: a.b.T  =>  nonTerm: a.b.T() { ... <prop assignments from default> ...  } */
    private fun retypeCreatedObject(trs: AsmTransformRuleSet, trr: AsmTransformationRule, pqn: PossiblyQualifiedName, defRule: AsmTransformationRule, defExpr: CreateObjectExpression) {
        val exprType = _typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(pqn)?.type() ?: StdLibDefault.NothingType
        val clonedTypeDeclOrCreated = findOrCloneFromDefaultTypeForTransformRule(pqn, trs, trr)
        val newExpr = CreateObjectExpressionDefault(pqn, defExpr.constructorArguments).also { it.propertyAssignments = defExpr.propertyAssignments }
        val newRule = asmTransformationRule(clonedTypeDeclOrCreated.type(), newExpr)
        newRule.grammarRuleName = trr.grammarRuleName
        trs.setRule(newRule)
        //also ensure properties are transferred
        val defDecl = defRule.resolvedType.resolvedDefinition
        when {
            clonedTypeDeclOrCreated is StructuredType -> {
                val tocopy = defDecl.property.filter {
                    it is PropertyDeclarationStored && null == exprType.resolvedDefinition.findOwnedPropertyOrNull(it.name)
                }
                tocopy.forEach { clonedTypeDeclOrCreated.appendPropertyStored(it.name, it.typeInstance, it.characteristics, it.index) }
            }
        }
    }

    /**
     * nonTerm: { T -> p := e ... }
     * the object from child[0] is modified, its properties 'p' are set, 'T' is the type of the grammar rule.
     * 'T' must exist (or is created with '#create-missing-types'), as must its properties 'p'.
     */
    private fun resolveModifyRules(trs: AsmTransformRuleSet) {
        val modifyRules = trs.rules.values.filterIsInstance<ModifyObjectRule>().filter { it !in _rulesForUnknownGrammarRule }
        if (modifyRules.isEmpty()) return
        val gtns = findOrCreateGrammarTypeNamespace(trs.qualifiedName)
        val exprTypeResolver = ExpressionTypeResolver(_typesDomain, gtns, _issues)
        modifyRules.forEach { trr ->
            val pqn = trr.possiblyQualifiedTypeName
            val existing = _typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(pqn)
                ?: transformFromGrammar.value.typesDomain?.findFirstDefinitionByPossiblyQualifiedNameOrNull(pqn)?.let { findOrCloneFromDefaultTypeForTransformRule(pqn, trs, trr) }
            val typeDecl = when {
                null != existing -> existing
                trs.options.createTypes -> findOrCloneFromDefaultTypeForTransformRule(pqn, trs, trr)
                else -> null
            }
            when (typeDecl) {
                null -> _issues.error(
                    locationOfRuleName(trr),
                    "Type '${pqn.value}' not found (in the transformation rule for '${trr.grammarRuleName.value}'), use '#${OPTION_CREATE_TYPES}' to create it"
                )

                else -> {
                    trr.modifyStatements.forEach { ms ->
                        val propName = PropertyName(ms.variable.name)
                        when {
                            null != typeDecl.findAllPropertyOrNull(propName) -> Unit
                            trs.options.createTypes && typeDecl is StructuredType -> {
                                var propType = exprTypeResolver.typeFor(ms.rhs, AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE)
                                if (propType == StdLibDefault.NothingType) {
                                    propType = StdLibDefault.AnyType
                                }
                                typeDecl.appendPropertyStored(propName, propType, setOf(PropertyCharacteristic.COMPOSITE), -1)
                            }

                            else -> _issues.error(
                                _locationMap?.get(ms) ?: locationOfRuleName(trr),
                                "Type '${typeDecl.qualifiedName.value}' has no property '${propName.value}' (set in the transformation rule for '${trr.grammarRuleName.value}'), use '#${OPTION_CREATE_TYPES}' to create it"
                            )
                        }
                    }
                    trr.resolveTypeAs(typeDecl.type())
                }
            }
        }
    }

    /**
     * without '#create-missing-types' every property set by 'T() { p := e }' must be a property of 'T' (AT-Q5),
     * as for a modify rule (see [resolveModifyRules])
     */
    private fun checkCreatedObjectProperties(trs: AsmTransformRuleSet) {
        if (trs.options.createTypes) return
        for (trr in trs.rules.values) {
            val expr = trr.expression
            if (expr !is CreateObjectExpression || trr.isResolved.not()) continue
            val typeDecl = trr.resolvedType.resolvedDefinitionOrNull as? StructuredType ?: continue // type not found: error given in typeFor
            expr.propertyAssignments.forEach { ass ->
                val propName = PropertyName(ass.variable.name)
                if (null == typeDecl.findAllPropertyOrNull(propName)) {
                    _issues.error(
                        _locationMap?.get(ass) ?: locationOfRuleName(trr),
                        "Type '${typeDecl.qualifiedName.value}' has no property '${propName.value}' (set in the transformation rule for '${trr.grammarRuleName.value}'), use '#${OPTION_CREATE_TYPES}' to create it"
                    )
                }
            }
        }
    }

    private fun typeFor(trs: AsmTransformRuleSet, trr: AsmTransformationRule): TypeInstance {
        // A TrRule needs to know the target type because this type is used
        // to define the type for a grammar Rule
        // thus only certain expressions are valid.
        val expr = trr.expression
        val gtns = findOrCreateGrammarTypeNamespace(trs.qualifiedName)
        val exprTypeResolver = ExpressionTypeResolver(_typesDomain, gtns, _issues)
        val exprType = when {
            trr.isResolved -> trr.resolvedType
            expr is CreateObjectExpression -> {
                val t = exprTypeResolver.typeFor(expr, StdLibDefault.AnyType)
                when {
                    StdLibDefault.NothingType != t -> t
                    // an existing type from the transform generated from the grammar
                    null != transformFromGrammar.value.typesDomain?.findFirstDefinitionByPossiblyQualifiedNameOrNull(expr.possiblyQualifiedTypeName) ->
                        findOrCloneFromDefaultTypeForTransformRule(expr.possiblyQualifiedTypeName, trs, trr).type()

                    else -> {
                        _issues.error(
                            locationOfRuleName(trr),
                            "Type '${expr.possiblyQualifiedTypeName.value}' for rule '${trr.grammarRuleName.value}' not found, define it or use '#${OPTION_CREATE_TYPES}'"
                        )
                        StdLibDefault.NothingType
                    }
                }
            }

            // self is the parse node of the rule (as for the property assignments of a created object), so 'children', 'child', '\$alternative' can be used
            expr is CreateTupleExpression -> exprTypeResolver.typeFor(expr, AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE)
            expr is CastExpression -> exprTypeResolver.typeFor(expr, AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE)
            else -> unsupportedRuleExpression(trr)
        }
        return exprType
    }

    private fun unsupportedRuleExpression(trr: AsmTransformationRule): TypeInstance {
        _issues.error(
            locationOfRuleName(trr),
            "The type of rule '${trr.grammarRuleName.value}' cannot be determined from '${trr.expression}', use 'Type() { ... }', 'tuple { ... }' or '<expression> as Type'"
        )
        return StdLibDefault.NothingType
    }

    /**
     * find type for 'typePqn' in default-transform-from-grammar
     * if it exists, clone it into type-domain for this transform
     * else create or find grammar-type-namespace and create new datatype for 'typePqn'
     */
    private fun findOrCloneFromDefaultTypeForTransformRule(typePqn: PossiblyQualifiedName, trs: AsmTransformRuleSet, trr: AsmTransformationRule): TypeDefinition {
        val grmDecl = transformFromGrammar.value.typesDomain?.findFirstDefinitionByPossiblyQualifiedNameOrNull(typePqn)
        grmDecl?.let { cloneSupertypesFromDefault(it) }
        val provided = grmDecl?.let { providedTypeOfOtherKind(it) }
        if (null != provided) {
            // the provided type is used as the type of the rule (David U20 A)
            (provided.namespace as? GrammarTypesNamespace)?.setTypeForGrammarRule(trr.grammarRuleName, provided.type())
            return provided
        }
        val clonedTypeDeclOrCreated = grmDecl?.findInOrCloneTo(_typesDomain) ?: let {
            // need a grammarTypeNamespace
            val gtns = findOrCreateGrammarTypeNamespace(trs.qualifiedName)
            // a provided type of another kind (e.g. an interface) is used as it is (David U20 A),
            // an error is given only if an object of it must be created
            val t = gtns.findOwnedTypeNamed(typePqn.simpleName)?.takeIf { it !is DataType }
                ?: gtns.findOwnedOrCreateDataTypeNamed(typePqn.simpleName)
            // no need to setTypeForGrammarRule, it is done later
            // gtns.setTypeForGrammarRule(trr.grammarRuleName, t.type())
            t
        }
        // if type was cloned (in findInOrCloneTo), it will not have had the tyoe for grammar rule set
        when (clonedTypeDeclOrCreated.namespace) {
            is GrammarTypesNamespace -> {
                //TODO: should really use clone of type from original!
                (clonedTypeDeclOrCreated.namespace as GrammarTypesNamespace).setTypeForGrammarRule(trr.grammarRuleName, clonedTypeDeclOrCreated.type())
            }
        }

        return clonedTypeDeclOrCreated
    }

    /**
     * a type with the qualified name of the default type 'grmDecl' is already in the types (e.g. provided in a types string),
     * but it is a different kind of type (e.g. an interface where the default type is a data type).
     * The provided type is then used for the rule (David U20 A), an error is given only if an object of it must be created.
     */
    private fun providedTypeOfOtherKind(grmDecl: TypeDefinition): TypeDefinition? {
        val existing = _typesDomain.findNamespaceOrNull(grmDecl.namespace.qualifiedName)?.findOwnedTypeNamed(grmDecl.name)
        return when {
            null == existing -> null
            grmDecl is DataType && existing !is DataType -> existing
            else -> null
        }
    }

    /**
     * the supertypes of a type from the default transform must also exist,
     * even if the rule that created them was overridden (e.g. a choice rule whose type is rewritten)
     */
    private fun cloneSupertypesFromDefault(grmDecl: TypeDefinition) {
        grmDecl.supertypes.forEach { st ->
            st.resolvedDefinitionOrNull?.let { stDecl ->
                cloneSupertypesFromDefault(stDecl)
                when (providedTypeOfOtherKind(stDecl)) {
                    null -> stDecl.findInOrCloneTo(_typesDomain)
                    else -> Unit // the provided type is used (David U20 A)
                }
            }
        }
    }

    private fun overrideDefaultNamespace(ns: AsmTransformNamespace) {
        when {
            ns.options.overrideDefault -> {
                val grmNs = transformFromGrammar.value.findNamespaceOrNull(ns.qualifiedName)
                when (grmNs) {
                    null -> _issues.error(_locationMap?.get(ns), "No default namespace with name '${ns.qualifiedName}' found, can't override it")
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

    private fun findOrCreateGrammarTypeNamespace(qn: QualifiedName): GrammarTypesNamespace =
        _typesDomain.findNamespaceOrNull(qn) as GrammarTypesNamespace?
            ?: GrammarTypesNamespaceSimple.findOrCreateGrammarNamespace(_typesDomain, qn).also { tns ->
                transformFromGrammar.value.typesDomain?.findNamespaceOrNull(tns.qualifiedName)?.let { gtns ->
                    gtns.import.forEach { tns.addImport(it) }
                }
                _typesDomain.addNamespace(tns)
            }

    private fun createMissingTypeNamespaces(trs: AsmTransformRuleSet) {
        when {
            trs.options.createTypes -> findOrCreateGrammarTypeNamespace(trs.qualifiedName)
        }
    }

    private fun overrideDefaultRuleSet(trs: AsmTransformRuleSet) {
        when {
            trs.options.overrideDefault -> {
                val grmRs = transformFromGrammar.value.findNamespaceOrNull(trs.namespace.qualifiedName)?.findDefinitionOrNull(trs.name)
                when (grmRs) {
                    null -> when (_grammarDomain.findDefinitionByQualifiedNameOrNull(trs.qualifiedName)) {
                        null -> Unit // no grammar for the rule set, error given in checkRulesNameGrammarRules
                        else -> _issues.error(_locationMap?.get(trs), "No default ruleset with name '${trs.qualifiedName}' found")
                    }
                    else -> {
                        for (grmRl in grmRs.rules.values) {
                            when {
                                trs.rules.containsKey(grmRl.grammarRuleName) -> Unit  // overridden, do not copy default rule
                                else -> {
                                    // rule not overridden so use default rule
                                    trs.setRule(grmRl)
                                    // and its type, which is in the types of the default transform
                                    findOrCloneFromDefaultTypeForTransformRule(grmRl.resolvedType.qualifiedTypeName, trs, grmRl)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** a type with the name of the created object exists in the namespace, but it is not a data type (e.g. an interface), so no object of it can be created */
    private fun isNotDataType(ns: GrammarTypesNamespace, expr: CreateObjectExpression): Boolean {
        val existing = ns.findOwnedTypeNamed(expr.possiblyQualifiedTypeName.simpleName)
        return null != existing && existing !is DataType
    }

    /** qualified name if the type resolves, else the name as written plus a note (qualifiedTypeName throws for a type that does not resolve) */
    private fun displayName(ti: TypeInstance): String = when (ti.resolvedDefinitionOrNull) {
        null -> "${ti.typeName.value} (not resolved)"
        else -> ti.qualifiedTypeName.value
    } + if (ti.isNullable) "?" else ""

    private fun createMissingTypes(trs: AsmTransformRuleSet) {
        when {
            trs.options.createTypes -> {
                for (trr in trs.rules.values) {
                    if (trr in _rulesForUnknownGrammarRule) continue
                    val expr = trr.expression
                    val exprPqn = when {
                        trr.isResolved -> trr.resolvedType.qualifiedTypeName
                        expr is CreateObjectExpression && isNotDataType(findOrCreateGrammarTypeNamespace(trs.qualifiedName), expr) -> {
                            val existing = findOrCreateGrammarTypeNamespace(trs.qualifiedName).findOwnedTypeNamed(expr.possiblyQualifiedTypeName.simpleName)
                            _issues.error(
                                locationOfRuleName(trr),
                                "Cannot create an object of '${expr.possiblyQualifiedTypeName.value}' (in the transformation rule for '${trr.grammarRuleName.value}'): ${if (existing is InterfaceType) "it is an interface" else "it is not a data type"}"
                            )
                            null
                        }

                        expr is CreateObjectExpression -> {
                            val gtns = findOrCreateGrammarTypeNamespace(trs.qualifiedName)
                            val exprTypeResolver = ExpressionTypeResolver(_typesDomain, gtns, _issues)
                            val t = gtns.findOwnedOrCreateDataTypeNamed(expr.possiblyQualifiedTypeName.simpleName)

                            val params = expr.constructorArguments.map { ass ->
                                val pName = TmParameterName(ass.variable.name)
                                var pType = exprTypeResolver.typeFor(ass.rhs, AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE)
                                if (pType == StdLibDefault.NothingType) {
                                    pType = StdLibDefault.AnyType
                                }
                                ParameterDefinitionSimple(pName, pType, null)
                            }
                            t.addConstructor(params)

                            expr.propertyAssignments.forEach { ass ->
                                val propName = PropertyName(ass.variable.name)
                                var propType = exprTypeResolver.typeFor(ass.rhs, AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE)
                                if (propType == StdLibDefault.NothingType) {
                                    propType = StdLibDefault.AnyType
                                }
                                val existingProp = t.findOwnedPropertyOrNull(propName)
                                when {
                                    null == existingProp -> {
                                        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
                                        t.appendPropertyStored(propName, propType, characteristics, -1)
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
                                            _locationMap?.get(ass) ?: locationOfRuleName(trr),
                                            "Trying to create missing property '${propName}: ${displayName(propType)}', '${t.qualifiedName}' already contains incompatible property '${existingProp.name}: ${displayName(existingProp.typeInstance)}'."
                                        )
                                    }
                                }
                            }
                            expr.possiblyQualifiedTypeName
                        }

                        expr is CreateTupleExpression -> StdLibDefault.TupleType.qualifiedName
                        expr is CastExpression -> expr.targetType.possiblyQualifiedName
                        else -> null // error issue given in typeFor
                    }
                    exprPqn?.let { findOrCloneFromDefaultTypeForTransformRule(exprPqn, trs, trr) }
                }
            }
        }
    }
}