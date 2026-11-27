/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.m2mTransform.api.M2mTransformRuleSet
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.m2mTransform.api.M2mTransformPatternRule
import net.akehurst.language.m2mTransform.api.RuleCall
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.DataType
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.m2mTransform.api.M2mTransformRule
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.m2mTransform.api.M2MTransformMapping
import net.akehurst.language.m2mTransform.api.PropertyTemplateRhs
import net.akehurst.language.m2mTransform.api.ObjectTemplate
import net.akehurst.language.m2mTransform.api.CollectionTemplate
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.*
import net.akehurst.language.m2mTransform.api.PropertyTemplateExpression
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypeParameterReference

class M2mTransformSemanticAnalyser : SemanticAnalyser<M2mTransformDomain, SentenceContext> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

    override fun clear() {
        _issues.clear()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: M2mTransformDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        _locationMap = locationMap
        _asm = asm
        val sentenceContext = options.sentenceContext
        when {
            null == sentenceContext -> _issues.warn(null, "No sentence context provided, either provide one or switch off Semantic Analysis.")
            else -> {
                // transform extends: resolve first, the rules of a transform include the inherited ones (allRule)
                asm.allTransformRuleSet.forEach { def -> resolveTransformExtends(asm, def) }
                asm.allTransformRuleSet.forEach { def -> checkTransformExtends(def) }
                asm.allTransformRuleSet.forEach { def ->
                    def.domainParameters.entries.forEach { (k, v) ->
                        val td = (sentenceContext.findItemsNamedConformingTo(v.value) { true }).firstOrNull()?.item
                        when (td) {
                            null -> _issues.error(locationMap?.get(def), "TypesDomain '${v.value}' for parameter '${k.value}' of transform '${def.name.value}' not found in the sentence context, add it to the context or correct the name.")
                            !is TypesDomain -> _issues.error(locationMap?.get(def), "'${v.value}' for parameter '${k.value}' of transform '${def.name.value}' is not a TypesDomain in the sentence context, rather a '${td::class.simpleName}'")
                            else -> def.resolveDomainParameter(k, td)
                        }
                    }
                    def.rule.forEach { (k, v) ->
                        // resolve extends references
                        v.extends.forEach { e ->
                            when (val r = def.allRule[e.nameOrQName.simpleName]) { //TODO: support qnames !
                                null -> _issues.error(locationMap?.get(def), "Rule '${e.nameOrQName.value}' extended by rule '${k.value}' in transform '${def.name.value}' not found.")
                                else -> e.resolveAs(r)
                            }
                        }
                        v.domainSignature.forEach { (dr, dv) ->
                            val typesDomain = def.domainParameterResolved[dr]
                            when {
                                // an unresolved domain parameter is reported once, for the transform
                                null == typesDomain && def.domainParameters.containsKey(dr) -> Unit
                                null == typesDomain -> _issues.error(ruleLocation(def, k), "Domain '${dr.value}' used in rule '${k.value}' is not a parameter of transform '${def.name.value}', expected one of ${def.domainParameters.keys.joinToString { "'${it.value}'" }}")
                                else -> {
                                    val typeIssues = dv.variable.resolveType(typesDomain)
                                    _issues.addAll(withRuleLocation(typeIssues, def, v))
                                }
                            }
                        }
                        // resolve where and when rule refs
                        when (v) {
                            is M2mTransformPatternRule -> {
                                v.domainTemplate.forEach { (dr,rhs) ->
                                    val typesDomain = def.domainParameterResolved[dr]
                                    if (null == typesDomain) {
                                        // already reported for the domain signature of the rule
                                    } else {
                                         // TODO: check that a template value conforms to its property type, e.g. an object template
                                         //  'key == Key {..}' for a collection property 'key: Set<Key>' is not reported
                                         val typeIssues = rhs.resolveTypes(typesDomain)
                                         _issues.addAll(withRuleLocation(typeIssues, def, v))
                                         checkTemplateProperties(def, k, rhs)
                                    }
                                }
                                if (v is M2MTransformMapping) {
                                    v.expression.forEach { (dr, expr) -> if (null != expr) checkCreateObject(def, k, dr, v, expr) }
                                }

                                when (val wn = v.when_) {
                                    is RuleCall -> resolveRuleCall(def, k, "when", wn, locationMap)
                                }
                                v.where.forEach { w ->
                                    resolveRuleCall(def, k, "where", w, locationMap)
                                }
                                checkVariablesBound(def, v)
                                if (v is M2MTransformMapping) checkAssignedTypes(def, v)
                            }
                        }
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(emptyList(), _issues)
    }

    /**
     * resolves the rule called in a 'when' or 'where' clause of rule 'callerName',
     * and checks that the called rule has the domains and parameters named in the call
     */
    private var _locationMap: LocationMap? = null

    /** issues made by the asm (no location map there) get the location of the rule they are in */
    private fun withRuleLocation(issues: List<LanguageIssue>, def: M2mTransformRuleSet, rule: M2mTransformRule): List<LanguageIssue> {
        val loc = ruleLocation(def, rule.name) ?: return issues
        return issues.map { if (null == it.location) it.copy(location = loc) else it }
    }

    /**
     * a property named in an object template must be a property of the template's type (David, issue #20 ER3).
     * Only checked for a data type without subtypes, as the property may be on a subtype otherwise.
     */
    private fun checkTemplateProperties(def: M2mTransformRuleSet, ruleName: SimpleName, rhs: PropertyTemplateRhs) {
        when (rhs) {
            is ObjectTemplate -> {
                val td = rhs.type?.resolvedDefinition
                if (td is DataType && td.subtypes.isEmpty()) {
                    val props = td.type().allResolvedProperty
                    rhs.propertyTemplate.forEach { (pn, pt) ->
                        if (null == props[PropertyName(pn.value)]) {
                            _issues.error(
                                ruleLocation(def, ruleName),
                                "In rule '${ruleName.value}' of transform '${def.name.value}', type '${td.name.value}' has no property '${pn.value}' (used in the template '${pn.value} == ...'), expected one of ${props.keys.joinToString { "'${it.value}'" }}"
                            )
                        }
                    }
                }
                rhs.propertyTemplate.values.forEach { checkTemplateProperties(def, ruleName, it.rhs) }
            }

            is CollectionTemplate -> rhs.elements.forEach { checkTemplateProperties(def, ruleName, it) }
            else -> Unit
        }
    }

    /**
     * for 'domain d x:T := T() { p := ... }' the created type must be found (in the domains of the transform)
     * and, for a data type without subtypes, each assigned property must be a property of it
     */
    private fun checkCreateObject(def: M2mTransformRuleSet, ruleName: SimpleName, dr: DomainReference, rule: M2mTransformPatternRule, expr: Expression) {
        if (expr !is CreateObjectExpression) return
        val preferred = def.domainParameterResolved[dr] ?: return // the unresolved domain is already reported
        val domains = (listOf(preferred) + def.domainParameterResolved.values).distinct()
        val td = domains.firstNotNullOfOrNull { it.findFirstDefinitionByPossiblyQualifiedNameOrNull(expr.possiblyQualifiedTypeName) }
        when {
            null == td -> _issues.error(
                ruleLocation(def, ruleName),
                "In rule '${ruleName.value}' of transform '${def.name.value}', the type '${expr.possiblyQualifiedTypeName.value}' to create for domain '${dr.value}' is not found in the domains of the transform"
            )

            td is DataType && td.subtypes.isEmpty() -> {
                val props = td.type().allResolvedProperty
                expr.propertyAssignments.forEach { pa ->
                    if (null == props[PropertyName(pa.variable.name)]) {
                        _issues.error(
                            ruleLocation(def, ruleName),
                            "In rule '${ruleName.value}' of transform '${def.name.value}', type '${td.name.value}' has no property '${pa.variable.name}' (assigned in '${td.name.value}() { ${pa.variable.name} := ... }'), expected one of ${props.keys.joinToString { "'${it.value}'" }}"
                        )
                    }
                }
            }

            else -> Unit
        }
    }

    /**
     * Each variable used in an expression of a pattern rule must be bound (issue #20, ER8): by the rule's domain signatures,
     * parameters or pivots, by a template ('name == n', 'p:Point { }', a gap 'rest:...', a member that is only a variable),
     * or as an argument of a 'when'/'where' call that is only a variable (it may be the target the call binds).
     * Bindings of the rules this rule extends count too. Names inside 'with', in lambdas and block assignments are local,
     * and a name in the '{ p := ... }' of a constructor may be a property of the created object, so those are not reported.
     */
    private fun checkVariablesBound(def: M2mTransformRuleSet, rule: M2mTransformPatternRule) {
        val bound = mutableSetOf<String>()
        val visited = mutableSetOf<M2mTransformRule>()
        fun bindAll(r: M2mTransformRule) {
            if (!visited.add(r)) return
            r.domainSignature.values.forEach { bound.add(it.variable.name.value) }
            r.parameters.forEach { bound.add(it.name.value) }
            if (r is M2mTransformPatternRule) {
                r.pivot.keys.forEach { bound.add(it.value) }
                r.domainTemplate.values.forEach { bindTemplate(it, bound) }
                val calls = listOfNotNull(r.when_ as? RuleCall) + r.where
                calls.forEach { c -> c.domainArguments.values.forEach { a -> if (a is RootExpression) bound.add(a.name) } }
            }
            r.extends.forEach { e -> e.resolved?.let { bindAll(it) } }
        }
        bindAll(rule)

        val used = mutableListOf<String>()
        rule.domainTemplate.values.forEach { usesInTemplate(it, used) }
        if (rule is M2MTransformMapping) rule.expression.values.forEach { e -> if (null != e) usesIn(e, emptySet(), used) }
        when (val wn = rule.when_) {
            null -> Unit
            is RuleCall -> callUses(wn, used)
            else -> usesIn(wn, emptySet(), used)
        }
        rule.where.forEach { callUses(it, used) }

        used.filter { it !in bound }.distinct().forEach { n ->
            _issues.error(
                ruleLocation(def, rule.name),
                "In rule '${rule.name.value}' of transform '${def.name.value}', the variable '$n' is used but never bound; bind it in a template (e.g. 'name == $n'), as a rule parameter, or with a 'when'/'where' call"
            )
        }
    }

    /**
     * In a mapping, a value assigned with ':=' must conform to the type it is assigned to (issue #20, ER8):
     * the property in 'T() { p := v }', or the target variable in 'domain d x:T := v'.
     * Only checked when the type of the value is certain: a variable whose type is given by the domain signature,
     * a pivot or parameter, or by the template property it is bound to, a literal, and property navigations from them.
     * Collections, 'Any' and nullability are not checked.
     */
    private fun checkAssignedTypes(def: M2mTransformRuleSet, rule: M2MTransformMapping) {
        val env = mutableMapOf<String, TypeInstance?>() // null: bound more than once with different types
        fun bind(name: String, t: TypeInstance?) {
            if (null == t) return
            env[name] = if (env.containsKey(name) && env[name] != t) null else t
        }
        rule.domainSignature.values.forEach { bind(it.variable.name.value, it.variable.type) }
        rule.pivot.values.forEach { bind(it.name.value, it.type) }
        rule.parameters.forEach { bind(it.name.value, it.type) }
        fun bindTemplate(rhs: PropertyTemplateRhs, type: TypeInstance?) {
            when (rhs) {
                is ObjectTemplate -> {
                    val t = rhs.type ?: type
                    rhs.identifier?.let { bind(it.value, t) }
                    val props = t?.takeIf { null != it.resolvedDefinitionOrNull }?.allResolvedProperty ?: emptyMap()
                    rhs.propertyTemplate.forEach { (pn, pt) -> bindTemplate(pt.rhs, props[PropertyName(pn.value)]?.typeInstance) }
                }

                is PropertyTemplateExpression -> (rhs.expression as? RootExpression)?.let { if (!it.isSelf && !it.isNothing) bind(it.name, type) }
                else -> Unit
            }
        }
        rule.domainTemplate.forEach { (dr, rhs) -> bindTemplate(rhs, rule.domainSignature[dr]?.variable?.type) }

        fun literalType(qn: QualifiedName): TypeInstance? =
            def.domainParameterResolved.values.firstNotNullOfOrNull { it.findByQualifiedNameOrNull(qn) }?.type()
                ?: StdLibDefault.findOwnedTypeNamed(qn.last)?.type()

        fun typeOf(e: Expression): TypeInstance? = when (e) {
            is RootExpression -> if (e.isSelf || e.isNothing) null else env[e.name]
            is LiteralExpression -> literalType(e.qualifiedTypeName)
            is GroupExpression -> typeOf(e.expression)
            is NavigationExpression -> e.parts.fold(typeOf(e.start)) { t, part ->
                when {
                    null == t || null == t.resolvedDefinitionOrNull -> null
                    part is PropertyCall -> t.allResolvedProperty[PropertyName(part.propertyName)]?.typeInstance
                    else -> null
                }
            }

            else -> null
        }

        fun checkable(t: TypeInstance?): TypeInstance? = t?.takeIf {
            val td = it.resolvedDefinitionOrNull
            null != td && it !is TypeParameterReference && !it.isCollection
                && td != StdLibDefault.AnyType.resolvedDefinition && td != StdLibDefault.NothingType.resolvedDefinition
        }?.notNullable()

        fun check(valueExpr: Expression, target: TypeInstance?, what: String) {
            val tt = checkable(target) ?: return
            val vt = checkable(typeOf(valueExpr)) ?: return
            if (!vt.conformsTo(tt)) {
                _issues.error(
                    ruleLocation(def, rule.name),
                    "In rule '${rule.name.value}' of transform '${def.name.value}', the value '${valueExpr.asString()}' has type '${vt.typeName.value}', which does not conform to the type '${tt.typeName.value}' of $what"
                )
            }
        }

        rule.expression.forEach { (dr, expr) ->
            if (null == expr) return@forEach
            val targetVar = rule.domainSignature[dr]?.variable
            when (expr) {
                is CreateObjectExpression -> {
                    val domains = (listOfNotNull(def.domainParameterResolved[dr]) + def.domainParameterResolved.values).distinct()
                    val td = domains.firstNotNullOfOrNull { it.findFirstDefinitionByPossiblyQualifiedNameOrNull(expr.possiblyQualifiedTypeName) }
                    if (td is DataType) {
                        val props = td.type().allResolvedProperty
                        expr.propertyAssignments.forEach { pa ->
                            props[PropertyName(pa.variable.name)]?.let { pd -> check(pa.rhs, pd.typeInstance, "property '${pa.variable.name}' of '${td.name.value}'") }
                        }
                    }
                }

                else -> targetVar?.let { check(expr, it.type, "'${it.name.value}'") }
            }
        }
    }

    private fun bindTemplate(rhs: PropertyTemplateRhs, bound: MutableSet<String>) {
        rhs.identifier?.let { bound.add(it.value) }
        when (rhs) {
            is ObjectTemplate -> rhs.propertyTemplate.values.forEach { bindTemplate(it.rhs, bound) }
            is CollectionTemplate -> {
                rhs.gaps.forEach { g -> g?.name?.let { bound.add(it.value) } }
                rhs.elements.forEach { bindTemplate(it, bound) }
            }
            is PropertyTemplateExpression -> (rhs.expression as? RootExpression)?.let { if (!it.isSelf && !it.isNothing) bound.add(it.name) }
            else -> Unit
        }
    }

    private fun usesInTemplate(rhs: PropertyTemplateRhs, used: MutableList<String>) {
        when (rhs) {
            is ObjectTemplate -> rhs.propertyTemplate.values.forEach { usesInTemplate(it.rhs, used) }
            is CollectionTemplate -> rhs.elements.forEach { usesInTemplate(it, used) }
            // a template that is only a variable binds it
            is PropertyTemplateExpression -> if (rhs.expression !is RootExpression) usesIn(rhs.expression, emptySet(), used)
            else -> Unit
        }
    }

    private fun callUses(call: RuleCall, used: MutableList<String>) {
        call.ruleArguments.values.forEach { usesIn(it, emptySet(), used) }
        call.domainArguments.values.forEach { usesIn(it, emptySet(), used) }
    }

    /** the root names used in [expr] that are not [local] */
    private fun usesIn(expr: Expression, local: Set<String>, used: MutableList<String>) {
        fun u(e: Expression) = usesIn(e, local, used)
        when (expr) {
            is RootExpression -> if (!expr.isSelf && !expr.isNothing && !expr.name.startsWith("$") && expr.name !in local) used.add(expr.name)
            is LiteralExpression -> Unit
            is NavigationExpression -> {
                u(expr.start)
                expr.parts.forEach { p ->
                    when (p) {
                        is MethodCall -> p.arguments.forEach { u(it) }
                        is IndexOperation -> p.indices.forEach { u(it) }
                        else -> Unit
                    }
                }
            }
            is CreateObjectExpression -> {
                expr.constructorArguments.forEach { u(it.rhs) }
                // a name in '{ p := ... }' may be a property of the created object, it is not checked
            }
            is CreateTupleExpression -> expr.propertyAssignments.forEach { u(it.rhs) }
            is OnExpression -> u(expr.expression) // its '{ p := ... }' as for a constructor
            is FunctionCall -> expr.arguments.forEach { u(it) }
            is LambdaExpression -> usesIn(expr.expression, local + expr.variables + "it", used)
            is StatementBlockExpression -> {
                val names = local + expr.assignment.map { it.variable.name }
                expr.assignment.forEach { usesIn(it.rhs, names, used) }
                usesIn(expr.expression, names, used)
            }
            is WithExpression -> u(expr.withContext) // names in its expression may be properties of the context
            is WhenExpression -> {
                expr.options.forEach { u(it.condition); u(it.expression) }
                u(expr.elseOption.expression)
            }
            is TernaryConditionExpression -> { u(expr.condition); u(expr.trueExpression); u(expr.falseExpression) }
            is InfixExpression -> expr.expressions.forEach { u(it) }
            is CastExpression -> u(expr.expression)
            is TypeTestExpression -> u(expr.expression)
            is GroupExpression -> u(expr.expression)
            is RuleCall -> callUses(expr, used)
            else -> Unit
        }
    }

    private var _asm: M2mTransformDomain? = null

    /**
     * rules are data classes (equal when name and 'top' are equal), so the location of a rule is only used
     * when no other transform has an equal rule, otherwise the location of the transform is used
     */
    private fun ruleLocation(def: M2mTransformRuleSet, ruleName: SimpleName): InputLocation? {
        val rule = def.rule[ruleName]
        val unique = null != rule && (_asm?.allTransformRuleSet?.count { rs -> rs.rule[ruleName] == rule } ?: 0) <= 1
        return when {
            unique -> _locationMap?.get(rule) ?: _locationMap?.get(def)
            else -> _locationMap?.get(def)
        }
    }

    private fun resolveRuleCall(def: M2mTransformRuleSet, callerName: SimpleName, clause: String, call: RuleCall, locationMap: LocationMap?) {
        val location = locationMap?.get(def)
        val called = def.allRule[call.ruleName]
        when (called) {
            null -> _issues.error(location, "In '$clause' clause of rule '${callerName.value}' in transform '${def.name.value}', the called rule '${call.ruleName.value}' is not found.")
            else -> {
                call.resolveAs(called)
                call.domainArguments.keys.filter { it !in called.domainSignature.keys }.forEach { dr ->
                    _issues.error(location, "In '$clause' clause of rule '${callerName.value}' in transform '${def.name.value}', the called rule '${call.ruleName.value}' has no domain '${dr.value}'.")
                }
                val paramNames = called.parameters.map { it.name }
                call.ruleArguments.keys.filter { it !in paramNames }.forEach { an ->
                    _issues.error(location, "In '$clause' clause of rule '${callerName.value}' in transform '${def.name.value}', the called rule '${call.ruleName.value}' has no parameter '${an.value}'.")
                }
            }
        }
    }

    // TODO: transforms (for 'extends') are only looked up in this text; look them up also in the namespaces given by 'import ns' (MT20, docs m2m-transform.md)
    /** a transform named in 'extends' is looked up by qualified name, or by simple name in the same namespace first */
    private fun resolveTransformExtends(asm: M2mTransformDomain, def: M2mTransformRuleSet) {
        def.extends.forEach { ref ->
            val name = ref.nameOrQName
            val all = asm.allTransformRuleSet
            val found = when (name) {
                is QualifiedName -> all.firstOrNull { it.qualifiedName == name }
                else -> all.firstOrNull { it.namespace === def.namespace && it.name.value == name.value }
                    ?: all.firstOrNull { it.name.value == name.value }
            }
            when (found) {
                null -> _issues.error(null, "Transform '${name.value}' extended by transform '${def.name.value}' not found.")
                else -> ref.resolveAs(found)
            }
        }
    }

    private fun checkTransformExtends(def: M2mTransformRuleSet) {
        val supers = def.extends.mapNotNull { it.resolved }
        if (extendsTransitively(def, def, mutableListOf())) {
            _issues.error(null, "Transform '${def.name.value}' extends itself (directly or through other transforms).")
            return
        }
        supers.forEach { sup ->
            if (sup.domainParameters != def.domainParameters) {
                val fmt = { m: Map<DomainReference, SimpleName> -> m.entries.joinToString { "${it.key.value}:${it.value.value}" } }
                _issues.error(null, "Transform '${def.name.value}' extends '${sup.name.value}', but their domains differ: (${fmt(def.domainParameters)}) and (${fmt(sup.domainParameters)}).")
            }
        }
        val inherited = supers.flatMap { it.allRule.keys }.toSet()
        def.rule.values.forEach { r ->
            when {
                r.isOverride && r.name !in inherited ->
                    _issues.error(null, "Rule '${r.name.value}' in transform '${def.name.value}' is marked 'override', but no rule named '${r.name.value}' is inherited.")

                r.isOverride.not() && r.name in inherited ->
                    _issues.error(null, "Rule '${r.name.value}' in transform '${def.name.value}' replaces an inherited rule with the same name, it must be marked 'override'.")
            }
        }
    }

    private fun extendsTransitively(def: M2mTransformRuleSet, target: M2mTransformRuleSet, visited: MutableList<M2mTransformRuleSet>): Boolean =
        def.extends.mapNotNull { it.resolved }.any { sup ->
            when {
                sup === target -> true
                visited.any { it === sup } -> false
                else -> {
                    visited.add(sup)
                    extendsTransitively(sup, target, visited)
                }
            }
        }
}