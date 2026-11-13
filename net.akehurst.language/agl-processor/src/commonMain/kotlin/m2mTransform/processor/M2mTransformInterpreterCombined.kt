/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.asm.simple.AnyExt.asString
import net.akehurst.language.agl.m2mTransform.processor.interpreter.ExecutionStepCombined
import net.akehurst.language.agl.m2mTransform.processor.interpreter.M2mPatternExecutorCombined
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.InfixExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.api.VariableAssignmentStatement
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObjectCombined
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter.Companion.RESULT
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter.Companion.cartesianProduct
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter.Companion.domainArgumentNames
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter.Companion.ruleArgumentNames
import net.akehurst.language.m2mTransform.processor.TemplateMatchResult.Companion.merge
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.ValueType
import net.akehurst.language.types.asm.StdLibDefault

/**
 * One m2m interpreter with both a non-suspending and a suspending API (transform / transformSuspend).
 * Behaviour is that of M2mTransformInterpreter.
 *
 * Same approach as ObjectGraphAccessorMutatorByReflection and ExpressionsInterpreterOverTypedObjectCombined:
 * each function body is written once, as a 'private inline fun xxxInternal', called by a non-suspending
 * and a suspending wrapper. The calls that differ (other rule functions, expression evaluation, object graph access)
 * are passed in as lambdas. Inline functions cannot be recursive, so every call to another rule function
 * goes through a lambda that calls the (non-inline) wrapper of the same mode.
 *
 * Relation rules are executed by M2mPatternExecutorCombined, whose steps have a non-suspending and a suspending body.
 */
class M2mTransformInterpreterCombined(
    val m2m: M2mTransformDomain,
    val domainAccessorMutatorByDomainName: Map<SimpleName, ObjectGraphAccessorMutator>,
    val issues: IssueHolder = IssueHolder(LanguageProcessorPhase.INTERPRET)
) {

    companion object {
        /**
         * Same as M2mTransformInterpreter.findCoveringSubsets2, but inline so that 'matches' may suspend.
         */
        inline fun <T, B, R> findCoveringSubsets2(
            cover: Collection<T>,
            bySubsetsOf: Collection<B>,
            matches: (c: T, b: B) -> Pair<Boolean, R>
        ): List<List<R>> {
            val tToCoverMap: Map<T, List<R>> = cover.associateWith { tElement ->
                bySubsetsOf.mapNotNull { cElement ->
                    val (isMatch, rValue) = matches(tElement, cElement)
                    if (isMatch) rValue else null
                }
            }
            val cp = tToCoverMap.values.cartesianProduct()
            return cp
        }
    }

    // ===================================================================================
    // --- Non-suspending API ---
    // ===================================================================================

    /**
     * @param targetDomainRef reference to the domain that is the target of the transformation
     * @param domainGraphs the objects for each source domain, keyed by the domain reference
     */
    fun transform(targetTransform: M2mTransformRuleSet, targetDomainRef: DomainReference, ruleArguments: Map<String, TypedObject>, domainGraphs: Map<DomainReference, List<TypedObject>>): M2MTransformResult =
        transformInternal(targetTransform, targetDomainRef, ruleArguments, domainGraphs) { x, r, s, a -> executeRule(x, r, s, a) }

    private fun executeRule(m2mExecution: M2mTransformExecution, rule: M2mTransformRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>): Unit = when (rule) {
        is M2mTransformAbstractRule -> executeAbstract(m2mExecution, rule, source, ruleArguments)
        is M2MTransformRelation -> executeRelation(m2mExecution, rule, source, ruleArguments)
        is M2MTransformMapping -> executeMapping(m2mExecution, rule, source, ruleArguments)
        is M2MTransformTable -> executeTable(m2mExecution, rule, source, ruleArguments)
        else -> error("Unknown rule type ${rule::class}")
    }

    private fun executeAbstract(m2mExecution: M2mTransformExecution, rule: M2mTransformAbstractRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>): Unit =
        executeAbstractInternal(m2mExecution, rule, source, ruleArguments) { x, r, s, a -> executeRule(x, r, s, a) }

    private fun executeMapping(m2mExecution: M2mTransformExecution, rule: M2MTransformMapping, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) =
        executeMappingInternal(
            m2mExecution, rule, source, ruleArguments,
            { x, r, s -> matchSourceVariables(x, r, s) },
            { x, r, w, e -> executeWhen(x, r, w, e) },
            { x, e, t, ex -> createFromExpression(x, e, t, ex) },
            { x, r, w, e -> executeWhere(x, r, w, e) }
        ) { x, o, e, ex -> setPropertiesFromExpression(x, o, e, ex) }

    private fun executeRelation(m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) =
        executeRelationInternal(
            m2mExecution, rule, source, ruleArguments,
            { x, r, s -> matchSourceVariables(x, r, s) },
            { x, r, w, e -> executeWhen(x, r, w, e) }
        ) { x, r, alt, e, steps -> executeRelationMainBody(x, r, alt, e, steps) }

    private fun executeRelationMainBody(m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, srcMatch: Map<DomainReference, TemplateMatchResult>, varsAfterWhen: EvaluationContext, whereSteps: List<ExecutionStepCombined>) {
        val (executor, tgtName) = buildRelationExecutor(m2mExecution, rule, srcMatch, varsAfterWhen, whereSteps)
        executor.execute(varsAfterWhen, tgtName)
    }

    private fun executeTable(m2mExecution: M2mTransformExecution, rule: M2MTransformTable, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) =
        executeTableInternal(m2mExecution, rule, source, ruleArguments) { og, e, ex -> evaluate(og, e, ex) }

    private fun matchSourceVariables(m2mExecution: M2mTransformExecution, rule: M2mTransformPatternRule, source: Map<DomainReference, List<TypedObject>>): Map<DomainReference, TemplateMatchAlternatives> =
        matchSourceVariablesInternal(m2mExecution, rule, source) { x, e, og, s, rhs -> matchVariablesFromRhs(x, e, og, s, rhs) }

    fun matchVariablesFromRhs(m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject, rhs: PropertyTemplateRhs): TemplateMatchAlternatives =
        matchVariablesFromRhsInternal(
            m2mExecution, evc, accessorMutator, src, rhs,
            { x, e, og, l, r -> matchVariablesFromPropertyTemplateExpression(x, e, og, l, r) },
            { x, e, t, og, s -> matchVariablesFromObjectTemplate(x, e, t, og, s) }
        ) { x, e, t, og, s -> matchVariablesFromCollectionTemplate(x, e, t, og, s) }

    /**
     * always returns isMatch==true
     */
    fun matchVariablesFromPropertyTemplateExpression(m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, lhs: TypedObject, rhs: PropertyTemplateExpression): TemplateMatchResult =
        matchVariablesFromPropertyTemplateExpressionInternal(m2mExecution, evc, accessorMutator, lhs, rhs) { og, e, ex -> evaluate(og, e, ex) }

    fun matchVariablesFromObjectTemplate(m2mExecution: M2mTransformExecution, evc: EvaluationContext, objectTemplate: ObjectTemplate, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject): TemplateMatchAlternatives =
        matchVariablesFromObjectTemplateInternal(m2mExecution, evc, objectTemplate, accessorMutator, src, { o, n -> o.getProperty(n) }) { x, e, og, s, rhs -> matchVariablesFromRhs(x, e, og, s, rhs) }

    fun matchVariablesFromCollectionTemplate(m2mExecution: M2mTransformExecution, evc: EvaluationContext, collectionTemplate: CollectionTemplate, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject): TemplateMatchAlternatives =
        matchVariablesFromCollectionTemplateInternal(m2mExecution, evc, collectionTemplate, accessorMutator, src) { x, e, t, og, els -> matchVariablesFromCollectionTemplateToElementsIn(x, e, t, og, els) }

    // get the different alternative combinations of objects from elements that (cover) match the defined templates
    // Set(  List(element-match per template)  )
    fun matchVariablesFromCollectionTemplateToElementsIn(m2mExecution: M2mTransformExecution, evc: EvaluationContext, collectionTemplate: CollectionTemplate, accessorMutator: ObjectGraphAccessorMutator, elements: List<TypedObject>): List<TemplateMatchResult> =
        matchVariablesFromCollectionTemplateToElementsInInternal(m2mExecution, evc, collectionTemplate, accessorMutator, elements) { x, e, og, s, rhs -> matchVariablesFromRhs(x, e, og, s, rhs) }

    fun executeWhen(m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, when_: Expression, evc: EvaluationContext): Pair<Boolean, EvaluationContext> =
        executeWhenInternal(m2mExecution, owningRule, when_, evc, { x, r, a, e -> getSource(x, r, a, e) }, { x, r, s, a -> executeRule(x, r, s, a) }) { og, e, ex -> evaluate(og, e, ex) }

    fun checkRelation(m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, domainArguments: Map<DomainReference, Expression>, callerEvc: EvaluationContext): Pair<Boolean, EvaluationContext> =
        checkRelationInternal(
            m2mExecution, rule, domainArguments, callerEvc,
            { og, e, ex -> evaluate(og, e, ex) },
            { x, e, og, s, rhs -> matchVariablesFromRhs(x, e, og, s, rhs) },
            { x, t, n, e -> lookupTargetViaBoundNeighbors(x, t, n, e) },
            { x, r, w, e -> executeWhen(x, r, w, e) }
        ) { x, r, a, e -> checkRelation(x, r, a, e) }

    private fun lookupTargetViaBoundNeighbors(m2mExecution: M2mTransformExecution, template: PropertyTemplateRhs, tgtVarName: String, evc: EvaluationContext): TypedObject? =
        lookupTargetViaBoundNeighborsInternal(m2mExecution, template, tgtVarName, evc, { o, n -> o.getProperty(n) }) { x, t, n, e -> lookupTargetViaBoundNeighbors(x, t, n, e) }

    /*
     * returns matchedVariables + variables set by executing the where
     */
    fun executeWhere(m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, where: RuleWhere, evc: EvaluationContext): EvaluationContext =
        executeWhereInternal(
            m2mExecution, owningRule, where, evc,
            { og, e, ex -> evaluate(og, e, ex) },
            { x, a, e -> evaluateArguments(x, a, e) }
        ) { x, o, r, a, s -> executeRuleWhere(x, o, r, a, s) }

    /*
     * returns null if the where-rule matches nothing
     */
    fun executeRuleWhere(m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, rule: M2mTransformRule, ruleArguments: Map<String, TypedObject>, source: Map<DomainReference, List<TypedObject>>): TypedObject? =
        executeRuleWhereInternal(m2mExecution, owningRule, rule, ruleArguments, source) { x, r, s, a -> executeRule(x, r, s, a) }

    fun evaluateArguments(m2mExecution: M2mTransformExecution, arguments: Map<DomainReference, Expression>, evc: EvaluationContext): Map<DomainReference, List<TypedObject>> =
        evaluateArgumentsInternal(m2mExecution, arguments, evc) { og, e, ex -> evaluate(og, e, ex) }

    fun getSource(m2mExecution: M2mTransformExecution, rule: M2mTransformRule, arguments: Map<DomainReference, Expression>, evc: EvaluationContext): Map<DomainReference, List<TypedObject>> =
        getSourceInternal(m2mExecution, rule, arguments, evc) { og, e, ex -> evaluate(og, e, ex) }

    fun evaluateArgs(m2mExecution: M2mTransformExecution, rule: M2mTransformRule, arguments: List<Expression>, matchedVariables: Map<String, TypedObject>): Map<DomainReference, List<TypedObject>> =
        evaluateArgsInternal(m2mExecution, rule, arguments, matchedVariables) { og, e, ex -> evaluate(og, e, ex) }

    fun createFromRhs(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateRhs): Pair<TypedObject, Map<String, TypedObject>> =
        createFromRhsInternal(
            m2mExecution, evc, lhsType, rhs,
            { x, e, t, r -> createFromPropertyTemplateExpression(x, e, t, r) },
            { x, e, t, r -> createFromObjectTemplate(x, e, t, r) }
        ) { x, e, t, r -> createFromCollectionTemplate(x, e, t, r) }

    fun createFromPropertyTemplateExpression(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateExpression): Pair<TypedObject, Map<String, TypedObject>> =
        createFromPropertyTemplateExpressionInternal(m2mExecution, evc, lhsType, rhs) { x, e, t, ex -> createFromExpression(x, e, t, ex) }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    fun createFromExpression(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, expression: Expression): TypedObject =
        createFromExpressionInternal(
            m2mExecution, evc, lhsType, expression,
            { og, e, ex -> interpreter(og).constructObject(e, ex) }
        ) { og, e, ex -> evaluate(og, e, ex) }

    fun createFromObjectTemplate(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, objectTemplate: ObjectTemplate): Pair<TypedObject, Map<String, TypedObject>> =
        createFromObjectTemplateInternal(
            m2mExecution, evc, lhsType, objectTemplate,
            { x, e, t, r -> createFromRhs(x, e, t, r) }
        ) { og, tn, args -> og.createStructureValue(tn, args) }

    fun createFromCollectionTemplate(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, collectionTemplate: CollectionTemplate): Pair<TypedObject, Map<String, TypedObject>> =
        createFromCollectionTemplateInternal(
            m2mExecution, evc, lhsType, collectionTemplate,
            { x, e, t, r -> createFromRhs(x, e, t, r) }
        ) { x, e, t, og, els -> matchVariablesFromCollectionTemplateToElementsIn(x, e, t, og, els) }

    fun setPropertiesFromRhs(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, rhs: PropertyTemplateRhs): Unit =
        setPropertiesFromRhsInternal(
            m2mExecution, obj, evc, rhs,
            { x, o, e, ex -> setPropertiesFromExpression(x, o, e, ex) },
            { x, o, e, t -> setPropertiesFromObjectTemplate(x, o, e, t) }
        ) { x, o, e, t -> setPropertiesFromCollectionTemplate(x, o, e, t) }

    fun setPropertiesFromExpression(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, expression: Expression) =
        setPropertiesFromExpressionInternal(m2mExecution, obj, evc, expression) { og, e, o, pa -> interpreter(og).propertyAssignmentBlock(e, o, pa) }

    fun setPropertiesFromObjectTemplate(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, objectTemplate: ObjectTemplate): Unit =
        setPropertiesFromObjectTemplateInternal(
            m2mExecution, obj, evc, objectTemplate,
            { o, n -> o.getProperty(n) },
            { x, e, t, r -> createFromRhs(x, e, t, r) }
        ) { x, o, e, r -> setPropertiesFromRhs(x, o, e, r) }

    fun setPropertiesFromCollectionTemplate(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, collectionTemplate: CollectionTemplate): Unit =
        setPropertiesFromCollectionTemplateInternal(m2mExecution, obj, evc, collectionTemplate) { x, o, e, r -> setPropertiesFromRhs(x, o, e, r) }

    private fun evaluate(og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression): TypedObject =
        interpreter(og).evaluateExpression(evc, expression)

    // ===================================================================================
    // --- Suspending API ---
    // ===================================================================================

    /**
     * @param targetDomainRef reference to the domain that is the target of the transformation
     * @param domainGraphs the objects for each source domain, keyed by the domain reference
     */
    suspend fun transformSuspend(targetTransform: M2mTransformRuleSet, targetDomainRef: DomainReference, ruleArguments: Map<String, TypedObject>, domainGraphs: Map<DomainReference, List<TypedObject>>): M2MTransformResult =
        transformInternal(targetTransform, targetDomainRef, ruleArguments, domainGraphs) { x, r, s, a -> executeRuleSuspend(x, r, s, a) }

    private suspend fun executeRuleSuspend(m2mExecution: M2mTransformExecution, rule: M2mTransformRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>): Unit = when (rule) {
        is M2mTransformAbstractRule -> executeAbstractSuspend(m2mExecution, rule, source, ruleArguments)
        is M2MTransformRelation -> executeRelationSuspend(m2mExecution, rule, source, ruleArguments)
        is M2MTransformMapping -> executeMappingSuspend(m2mExecution, rule, source, ruleArguments)
        is M2MTransformTable -> executeTableSuspend(m2mExecution, rule, source, ruleArguments)
        else -> error("Unknown rule type ${rule::class}")
    }

    private suspend fun executeAbstractSuspend(m2mExecution: M2mTransformExecution, rule: M2mTransformAbstractRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>): Unit =
        executeAbstractInternal(m2mExecution, rule, source, ruleArguments) { x, r, s, a -> executeRuleSuspend(x, r, s, a) }

    private suspend fun executeMappingSuspend(m2mExecution: M2mTransformExecution, rule: M2MTransformMapping, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) =
        executeMappingInternal(
            m2mExecution, rule, source, ruleArguments,
            { x, r, s -> matchSourceVariablesSuspend(x, r, s) },
            { x, r, w, e -> executeWhenSuspend(x, r, w, e) },
            { x, e, t, ex -> createFromExpressionSuspend(x, e, t, ex) },
            { x, r, w, e -> executeWhereSuspend(x, r, w, e) }
        ) { x, o, e, ex -> setPropertiesFromExpressionSuspend(x, o, e, ex) }

    private suspend fun executeRelationSuspend(m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) =
        executeRelationInternal(
            m2mExecution, rule, source, ruleArguments,
            { x, r, s -> matchSourceVariablesSuspend(x, r, s) },
            { x, r, w, e -> executeWhenSuspend(x, r, w, e) }
        ) { x, r, alt, e, steps -> executeRelationMainBodySuspend(x, r, alt, e, steps) }

    private suspend fun executeRelationMainBodySuspend(m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, srcMatch: Map<DomainReference, TemplateMatchResult>, varsAfterWhen: EvaluationContext, whereSteps: List<ExecutionStepCombined>) {
        val (executor, tgtName) = buildRelationExecutor(m2mExecution, rule, srcMatch, varsAfterWhen, whereSteps)
        executor.executeSuspend(varsAfterWhen, tgtName)
    }

    private suspend fun executeTableSuspend(m2mExecution: M2mTransformExecution, rule: M2MTransformTable, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) =
        executeTableInternal(m2mExecution, rule, source, ruleArguments) { og, e, ex -> evaluateSuspend(og, e, ex) }

    private suspend fun matchSourceVariablesSuspend(m2mExecution: M2mTransformExecution, rule: M2mTransformPatternRule, source: Map<DomainReference, List<TypedObject>>): Map<DomainReference, TemplateMatchAlternatives> =
        matchSourceVariablesInternal(m2mExecution, rule, source) { x, e, og, s, rhs -> matchVariablesFromRhsSuspend(x, e, og, s, rhs) }

    suspend fun matchVariablesFromRhsSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject, rhs: PropertyTemplateRhs): TemplateMatchAlternatives =
        matchVariablesFromRhsInternal(
            m2mExecution, evc, accessorMutator, src, rhs,
            { x, e, og, l, r -> matchVariablesFromPropertyTemplateExpressionSuspend(x, e, og, l, r) },
            { x, e, t, og, s -> matchVariablesFromObjectTemplateSuspend(x, e, t, og, s) }
        ) { x, e, t, og, s -> matchVariablesFromCollectionTemplateSuspend(x, e, t, og, s) }

    /**
     * always returns isMatch==true
     */
    suspend fun matchVariablesFromPropertyTemplateExpressionSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, lhs: TypedObject, rhs: PropertyTemplateExpression): TemplateMatchResult =
        matchVariablesFromPropertyTemplateExpressionInternal(m2mExecution, evc, accessorMutator, lhs, rhs) { og, e, ex -> evaluateSuspend(og, e, ex) }

    suspend fun matchVariablesFromObjectTemplateSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, objectTemplate: ObjectTemplate, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject): TemplateMatchAlternatives =
        matchVariablesFromObjectTemplateInternal(m2mExecution, evc, objectTemplate, accessorMutator, src, { o, n -> o.getPropertySuspend(n) }) { x, e, og, s, rhs -> matchVariablesFromRhsSuspend(x, e, og, s, rhs) }

    suspend fun matchVariablesFromCollectionTemplateSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, collectionTemplate: CollectionTemplate, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject): TemplateMatchAlternatives =
        matchVariablesFromCollectionTemplateInternal(m2mExecution, evc, collectionTemplate, accessorMutator, src) { x, e, t, og, els -> matchVariablesFromCollectionTemplateToElementsInSuspend(x, e, t, og, els) }

    suspend fun matchVariablesFromCollectionTemplateToElementsInSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, collectionTemplate: CollectionTemplate, accessorMutator: ObjectGraphAccessorMutator, elements: List<TypedObject>): List<TemplateMatchResult> =
        matchVariablesFromCollectionTemplateToElementsInInternal(m2mExecution, evc, collectionTemplate, accessorMutator, elements) { x, e, og, s, rhs -> matchVariablesFromRhsSuspend(x, e, og, s, rhs) }

    suspend fun executeWhenSuspend(m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, when_: Expression, evc: EvaluationContext): Pair<Boolean, EvaluationContext> =
        executeWhenInternal(m2mExecution, owningRule, when_, evc, { x, r, a, e -> getSourceSuspend(x, r, a, e) }, { x, r, s, a -> executeRuleSuspend(x, r, s, a) }) { og, e, ex -> evaluateSuspend(og, e, ex) }

    suspend fun checkRelationSuspend(m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, domainArguments: Map<DomainReference, Expression>, callerEvc: EvaluationContext): Pair<Boolean, EvaluationContext> =
        checkRelationInternal(
            m2mExecution, rule, domainArguments, callerEvc,
            { og, e, ex -> evaluateSuspend(og, e, ex) },
            { x, e, og, s, rhs -> matchVariablesFromRhsSuspend(x, e, og, s, rhs) },
            { x, t, n, e -> lookupTargetViaBoundNeighborsSuspend(x, t, n, e) },
            { x, r, w, e -> executeWhenSuspend(x, r, w, e) }
        ) { x, r, a, e -> checkRelationSuspend(x, r, a, e) }

    private suspend fun lookupTargetViaBoundNeighborsSuspend(m2mExecution: M2mTransformExecution, template: PropertyTemplateRhs, tgtVarName: String, evc: EvaluationContext): TypedObject? =
        lookupTargetViaBoundNeighborsInternal(m2mExecution, template, tgtVarName, evc, { o, n -> o.getPropertySuspend(n) }) { x, t, n, e -> lookupTargetViaBoundNeighborsSuspend(x, t, n, e) }

    /*
     * returns matchedVariables + variables set by executing the where
     */
    suspend fun executeWhereSuspend(m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, where: RuleWhere, evc: EvaluationContext): EvaluationContext =
        executeWhereInternal(
            m2mExecution, owningRule, where, evc,
            { og, e, ex -> evaluateSuspend(og, e, ex) },
            { x, a, e -> evaluateArgumentsSuspend(x, a, e) }
        ) { x, o, r, a, s -> executeRuleWhereSuspend(x, o, r, a, s) }

    /*
     * returns null if the where-rule matches nothing
     */
    suspend fun executeRuleWhereSuspend(m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, rule: M2mTransformRule, ruleArguments: Map<String, TypedObject>, source: Map<DomainReference, List<TypedObject>>): TypedObject? =
        executeRuleWhereInternal(m2mExecution, owningRule, rule, ruleArguments, source) { x, r, s, a -> executeRuleSuspend(x, r, s, a) }

    suspend fun evaluateArgumentsSuspend(m2mExecution: M2mTransformExecution, arguments: Map<DomainReference, Expression>, evc: EvaluationContext): Map<DomainReference, List<TypedObject>> =
        evaluateArgumentsInternal(m2mExecution, arguments, evc) { og, e, ex -> evaluateSuspend(og, e, ex) }

    suspend fun getSourceSuspend(m2mExecution: M2mTransformExecution, rule: M2mTransformRule, arguments: Map<DomainReference, Expression>, evc: EvaluationContext): Map<DomainReference, List<TypedObject>> =
        getSourceInternal(m2mExecution, rule, arguments, evc) { og, e, ex -> evaluateSuspend(og, e, ex) }

    suspend fun evaluateArgsSuspend(m2mExecution: M2mTransformExecution, rule: M2mTransformRule, arguments: List<Expression>, matchedVariables: Map<String, TypedObject>): Map<DomainReference, List<TypedObject>> =
        evaluateArgsInternal(m2mExecution, rule, arguments, matchedVariables) { og, e, ex -> evaluateSuspend(og, e, ex) }

    suspend fun createFromRhsSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateRhs): Pair<TypedObject, Map<String, TypedObject>> =
        createFromRhsInternal(
            m2mExecution, evc, lhsType, rhs,
            { x, e, t, r -> createFromPropertyTemplateExpressionSuspend(x, e, t, r) },
            { x, e, t, r -> createFromObjectTemplateSuspend(x, e, t, r) }
        ) { x, e, t, r -> createFromCollectionTemplateSuspend(x, e, t, r) }

    suspend fun createFromPropertyTemplateExpressionSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateExpression): Pair<TypedObject, Map<String, TypedObject>> =
        createFromPropertyTemplateExpressionInternal(m2mExecution, evc, lhsType, rhs) { x, e, t, ex -> createFromExpressionSuspend(x, e, t, ex) }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    suspend fun createFromExpressionSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, expression: Expression): TypedObject =
        createFromExpressionInternal(
            m2mExecution, evc, lhsType, expression,
            { og, e, ex -> interpreter(og).constructObjectSuspend(e, ex) }
        ) { og, e, ex -> evaluateSuspend(og, e, ex) }

    suspend fun createFromObjectTemplateSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, objectTemplate: ObjectTemplate): Pair<TypedObject, Map<String, TypedObject>> =
        createFromObjectTemplateInternal(
            m2mExecution, evc, lhsType, objectTemplate,
            { x, e, t, r -> createFromRhsSuspend(x, e, t, r) }
        ) { og, tn, args -> og.createStructureValueSuspend(tn, args) }

    suspend fun createFromCollectionTemplateSuspend(m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, collectionTemplate: CollectionTemplate): Pair<TypedObject, Map<String, TypedObject>> =
        createFromCollectionTemplateInternal(
            m2mExecution, evc, lhsType, collectionTemplate,
            { x, e, t, r -> createFromRhsSuspend(x, e, t, r) }
        ) { x, e, t, og, els -> matchVariablesFromCollectionTemplateToElementsInSuspend(x, e, t, og, els) }

    suspend fun setPropertiesFromRhsSuspend(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, rhs: PropertyTemplateRhs): Unit =
        setPropertiesFromRhsInternal(
            m2mExecution, obj, evc, rhs,
            { x, o, e, ex -> setPropertiesFromExpressionSuspend(x, o, e, ex) },
            { x, o, e, t -> setPropertiesFromObjectTemplateSuspend(x, o, e, t) }
        ) { x, o, e, t -> setPropertiesFromCollectionTemplateSuspend(x, o, e, t) }

    suspend fun setPropertiesFromExpressionSuspend(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, expression: Expression) =
        setPropertiesFromExpressionInternal(m2mExecution, obj, evc, expression) { og, e, o, pa -> interpreter(og).propertyAssignmentBlockSuspend(e, o, pa) }

    suspend fun setPropertiesFromObjectTemplateSuspend(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, objectTemplate: ObjectTemplate): Unit =
        setPropertiesFromObjectTemplateInternal(
            m2mExecution, obj, evc, objectTemplate,
            { o, n -> o.getPropertySuspend(n) },
            { x, e, t, r -> createFromRhsSuspend(x, e, t, r) }
        ) { x, o, e, r -> setPropertiesFromRhsSuspend(x, o, e, r) }

    suspend fun setPropertiesFromCollectionTemplateSuspend(m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, collectionTemplate: CollectionTemplate): Unit =
        setPropertiesFromCollectionTemplateInternal(m2mExecution, obj, evc, collectionTemplate) { x, o, e, r -> setPropertiesFromRhsSuspend(x, o, e, r) }

    private suspend fun evaluateSuspend(og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression): TypedObject =
        interpreter(og).evaluateExpressionSuspend(evc, expression)

    // ===================================================================================
    // --- Shared, not dependent on suspend ---
    // ===================================================================================

    /** types of all domains of the transform can be referred to in expressions (e.g. 'is', 'as'), those of 'og' first */
    private fun interpreter(og: ObjectGraphAccessorMutator) = ExpressionsInterpreterOverTypedObjectCombined(
        og, null,
        domainAccessorMutatorByDomainName.values.filter { it !== og }.map { it.typesDomain }
    )

    private fun compileRelationWhereClause(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformRelation,
        varsAfterWhen: EvaluationContext
    ): List<ExecutionStepCombined> {
        val steps = mutableListOf<ExecutionStepCombined>()

        // Assuming your where AST contains a list of relation calls (e.g. `mapped StringConvert`)
        for (call in rule.where) { // <-- Replace with your AST property
            val whereRule = call.resolved!!
            val whereTargetArgName = (call.domainArguments[m2mExecution.targetDomainRef] as? RootExpression)?.name
            // Determine direction strictly by checking if the variable is already bound in our environment context
            val isWhereTargetAlreadyBound = whereTargetArgName != null && varsAfterWhen.getOrInParent(whereTargetArgName) != null
            val whereOutputs = if (isWhereTargetAlreadyBound || whereTargetArgName == null) emptyList() else listOf(whereTargetArgName)
            val whereInputs = call.ruleArgumentNames + call.domainArgumentNames - whereOutputs
            val description = call.asString() // <-- Replace with your AST property

            val step = ExecutionStepCombined(description, whereInputs, whereOutputs, { evc ->
                executeWhere(m2mExecution, rule, call, evc)
            }, { evc ->
                executeWhereSuspend(m2mExecution, rule, call, evc)
            })

            steps.add(step)
        }

        return steps
    }

    /**
     * returns the executor and the name of the target variable
     */
    private fun buildRelationExecutor(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformRelation,
        srcMatch: Map<DomainReference, TemplateMatchResult>,
        varsAfterWhen: EvaluationContext,
        whereSteps: List<ExecutionStepCombined>
    ): Pair<M2mPatternExecutorCombined, String> {
        val template = rule.domainTemplate[m2mExecution.targetDomainRef] ?: error("...")
        val lhsType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type ?: error("...")
        val tgtName = template.identifier?.value ?: RESULT

        val srcs = srcMatch.entries.associate { (srcDomainRef, v) ->
            val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("...")
            val srcId = srcObjPat.identifier?.value ?: error("...")
            Pair(srcDomainRef, v.getValueNamed(srcId) ?: error("..."))
        }

        val mappedVars = rule.domainTemplate.map { (k, v) -> v.identifier?.value ?: error("...") }

        val recordMapping = ExecutionStepCombined("Record Mapping", mappedVars + tgtName, emptyList(), { evc ->
            val tgtValue = evc.getOrInParent(tgtName) ?: error("Target value '$tgtName' not found in evaluation context.")
            val mapping = srcs + Pair(m2mExecution.targetDomainRef, tgtValue)
            m2mExecution.addRecord(rule, mapping)
        })
        val initVars = varsAfterWhen.namedValues.keys + whereSteps.flatMap { it.outputs }.toSet()
        val initExes = listOf(recordMapping) + whereSteps
        val executor = M2mPatternExecutorCombined(
            m2mExecution.issues, m2mExecution.targetAccessorMutator, initVars, initExes,
            domainAccessorMutatorByDomainName.values.filter { it !== m2mExecution.targetAccessorMutator }.map { it.typesDomain }
        )
        executor.build(tgtName, template, lhsType)
        return Pair(executor, tgtName)
    }

    private fun extractWhenAndWhereTargets(rule: M2MTransformRelation, tgt: DomainReference): List<String> {
        val fromWhen = rule.when_?.let { extractWhenTarget(it, tgt) } ?: emptyList()

        val fromWhere = rule.where.mapNotNull {
            val exp = it.domainArguments[tgt]
            when (exp) {
                is RootExpression -> exp.name
                else -> null
            }
        }
        return fromWhen + fromWhere
    }

    private fun extractWhenTarget(exp: Expression, tgt: DomainReference): List<String> {
        return when (exp) {
            is RuleWhen -> {
                val exp = exp.domainArguments[tgt]
                when (exp) {
                    is RootExpression -> listOf(exp.name)
                    else -> emptyList()
                }
            }

            is InfixExpression -> exp.expressions.flatMap { extractWhenTarget(it, tgt) }
            //TODO: do we need any other cases here ?
            else -> emptyList()
        }
    }

    /**
     * the record of 'rule' whose source values are the given ones, i.e. 'rule' was applied for them (as QVT 'when'),
     * null if 'rule' was not applied for them
     */
    private fun findAppliedRecord(m2mExecution: M2mTransformExecution, rule: M2mTransformRule, source: Map<DomainReference, List<TypedObject>>): Map<DomainReference, TypedObject>? {
        val rec = m2mExecution.records[rule] ?: return null
        return rec.alternatives.firstOrNull { mapping ->
            mapping.entries.all { (dr, v) ->
                when (dr) {
                    m2mExecution.targetDomainRef -> true // do not match it
                    else -> source[dr]?.any { //FIXME: this maybe not correct if multiple alternatives !
                        m2mExecution.domainAccessorMutator[dr]!!.equalTo(v, it)
                    } ?: false
                }
            }
        }
    }

    /**
     * binds the target-domain argument of a 'when' rule call to the given value
     */
    private fun bindWhenTarget(m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, when_: RuleWhen, tgtValue: TypedObject?, evc: EvaluationContext): EvaluationContext {
        val targetArg = when_.domainArguments[m2mExecution.targetDomainRef]
        return when (targetArg) {
            null -> {
                m2mExecution.errorIssue(
                    "Argument for target domain '${m2mExecution.targetDomainRef.value}' not found in rule call '${when_.ruleName.value}' in 'when' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                )
                evc
            }

            is RootExpression -> evc.child(mapOf(targetArg.name to (tgtValue ?: m2mExecution.targetAccessorMutator.nothing())))

            else -> {
                m2mExecution.warnIssue(
                    "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${when_.ruleName.value}' in 'when' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                )
                evc
            }
        }
    }

    /**
     * true if 'rule' was already applied for the variables bound in 'matched' (as QVT, a rule is applied at most once for the same binding)
     */
    private fun isAlreadyApplied(m2mExecution: M2mTransformExecution, rule: M2mTransformPatternRule, matched: TemplateMatchResult): Boolean =
        m2mExecution.appliedBindings[rule]?.any { b -> sameBinding(b, matched.matchedVariables) } ?: false

    private fun markApplied(m2mExecution: M2mTransformExecution, rule: M2mTransformPatternRule, matched: TemplateMatchResult) {
        m2mExecution.appliedBindings.getOrPut(rule) { mutableListOf() }.add(matched.matchedVariables)
    }

    private fun sameBinding(a: Map<String, TypedObject>, b: Map<String, TypedObject>): Boolean =
        a.keys == b.keys && a.all { (k, v) -> v.accessor.equalTo(v, b[k]!!) }

    /**
     * for 'related all' / 'mapped all': element i of each collection argument (as for 'map all' in a 'where' clause),
     * null (and an error issue) if an argument is not a collection
     */
    private fun sourceElementsForAll(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        when_: RuleWhen,
        source: Map<DomainReference, List<TypedObject>>
    ): List<Map<DomainReference, List<TypedObject>>>? {
        if (source.values.any { vs -> vs.any { it.type.isCollection.not() } }) {
            m2mExecution.errorIssue(
                "In 'when' clause of rule '${owningRule.name.value}' in '${m2mExecution.targetTransform.name.value}', the all call to rule '${when_.ruleName.value}' is expecting a collection."
            )
            return null
        }
        val sourceElements = mutableListOf<MutableMap<DomainReference, MutableList<TypedObject>>>()
        source.forEach { (dr, colls) ->
            val og = m2mExecution.domainAccessorMutator[dr] ?: error("ObjectGraph not found for domain '${dr.value}'")
            colls.forEach { coll ->
                og.forEachIndexed(coll) { idx, el ->
                    while (idx >= sourceElements.size) sourceElements.add(mutableMapOf())
                    sourceElements[idx].getOrPut(dr) { mutableListOf() }.add(el)
                }
            }
        }
        return sourceElements
    }

    /**
     * 'related all R {..}' / 'mapped all R {..}' holds if 'rule' was applied for every element, the target variable is bound to the list of results
     */
    private fun whenForAllResult(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        when_: RuleWhen,
        rule: M2mTransformRule,
        found: List<Map<DomainReference, TypedObject>?>,
        evc: EvaluationContext
    ): Pair<Boolean, EvaluationContext> = when {
        found.any { null == it } -> Pair(false, evc)
        else -> {
            val tgtElemType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type ?: StdLibDefault.AnyType
            val tgtType = StdLibDefault.List.type(listOf(tgtElemType.asTypeArgument))
            val tgtList = found.mapNotNull { it!![m2mExecution.targetDomainRef] }
            val tgtValue = m2mExecution.targetAccessorMutator.createCollection(tgtType, tgtList)
            Pair(true, bindWhenTarget(m2mExecution, owningRule, when_, tgtValue, evc))
        }
    }

    fun checkMapping(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformMapping,
        domainArguments: Map<DomainReference, Expression>,
        callerEvc: EvaluationContext
    ): Pair<Boolean, EvaluationContext> {
        val source = getSource(m2mExecution, rule, domainArguments, callerEvc)
        val found = findAppliedRecord(m2mExecution, rule, source) ?: return Pair(false, callerEvc)
        val targetArg = domainArguments[m2mExecution.targetDomainRef]
        val tgtValue = found[m2mExecution.targetDomainRef]
        return when {
            targetArg is RootExpression && null != tgtValue -> Pair(true, callerEvc.child(mapOf(targetArg.name to tgtValue)))
            else -> Pair(true, callerEvc)
        }
    }

    fun findMatchInMappingRecord(m2mExecution: M2mTransformExecution, record: MappingRecord, source: Map<DomainReference, List<TypedObject>>): TypedObject? {
        val alts = record.alternatives.filter { rec ->
            source.all { (k, v) ->
                val alt = rec[k]!!
                v.any { alt.accessor.equalTo(alt, it) }
            }
        }
        return when (alts.size) {
            0 -> null
            1 -> alts.first()[m2mExecution.targetDomainRef]
            else -> TODO("handle multiple matches from where")
        }
    }

    // ===================================================================================
    // --- Internal, shared by both APIs ---
    // ===================================================================================

    private inline fun transformInternal(
        targetTransform: M2mTransformRuleSet,
        targetDomainRef: DomainReference,
        ruleArguments: Map<String, TypedObject>,
        domainGraphs: Map<DomainReference, List<TypedObject>>,
        executeRule: (m2mExecution: M2mTransformExecution, rule: M2mTransformRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) -> Unit
    ): M2MTransformResult {
        val domainAccessorMutator = targetTransform.domainParameters.entries.associate { (k, v) ->
            Pair(k, domainAccessorMutatorByDomainName[v] ?: error("Domain ObjectGraph not found for domain $k"))
        }
        // issues raised before this call (earlier transforms on the same interpreter or accessor-mutators) are not part of this result
        val issuesBefore = issues.all.toSet()
        val accessorIssuesBefore = domainAccessorMutatorByDomainName.mapValues { (_, am) -> am.issues.all.toSet() }
        val m2mExecution = M2mTransformExecution(targetTransform, targetDomainRef, domainAccessorMutator, issues)
        m2mExecution.evaluationStep("Executing transform rule set '${targetTransform.qualifiedName}' with target domain '${targetDomainRef.value}'.")
        when {
            targetTransform.topRule.isEmpty() -> m2mExecution.errorIssue("No conforming top rule found for target domain '${targetDomainRef.value}'")
            else -> targetTransform.topRule.forEach { topRule ->
                executeRule(m2mExecution, topRule, domainGraphs, ruleArguments)
            }
        }
        val allIssues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        allIssues.addAll(issues.all.filter { it !in issuesBefore })
        domainAccessorMutatorByDomainName.forEach { (dn, am) ->
            val before = accessorIssuesBefore[dn] ?: emptySet()
            allIssues.addAll(am.issues.all.filter { it !in before })
        }
        return M2MTransformResult(allIssues, m2mExecution.records, targetDomainRef)
    }

    private inline fun executeAbstractInternal(
        m2mExecution: M2mTransformExecution,
        rule: M2mTransformAbstractRule,
        source: Map<DomainReference, List<TypedObject>>,
        ruleArguments: Map<String, TypedObject>,
        executeRule: (m2mExecution: M2mTransformExecution, rule: M2mTransformRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) -> Unit
    ) {
        val subrules = m2mExecution.targetTransform.rule.values.filter { r -> r !is M2mTransformAbstractRule && r.conformsTo(rule) }
        subrules.forEach { sr ->
            executeRule(m2mExecution, sr, source, ruleArguments)
            val rec = m2mExecution.records[sr]
            rec?.bySource(source)?.let {
                m2mExecution.addRecord(rule, it)
            }
        }
    }

    private inline fun executeMappingInternal(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformMapping,
        source: Map<DomainReference, List<TypedObject>>,
        ruleArguments: Map<String, TypedObject>,
        matchSourceVariables: (m2mExecution: M2mTransformExecution, rule: M2mTransformPatternRule, source: Map<DomainReference, List<TypedObject>>) -> Map<DomainReference, TemplateMatchAlternatives>,
        executeWhen: (m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, when_: Expression, evc: EvaluationContext) -> Pair<Boolean, EvaluationContext>,
        createFromExpression: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, expression: Expression) -> TypedObject,
        executeWhere: (m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, where: RuleWhere, evc: EvaluationContext) -> EvaluationContext,
        setPropertiesFromExpression: (m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, expression: Expression) -> Unit
    ) {
        m2mExecution.evaluationStep("Executing mapping rule '${rule.name.value}'.")
        // for each domain reference get the match alternatives for each source object
        // Map of DomainReference -> List< TemplateMatchAlternatives per object >
        val domToListOfAlts = matchSourceVariables(m2mExecution, rule, source)
        when {
            domToListOfAlts.isEmpty() -> m2mExecution.warnIssue("No matches found in source domains for rule '${rule.name}'.")
            else -> {
                val altSources = domToListOfAlts
                    .map { (k, v) -> Pair(k, v.alternatives) }
                    .toMap()
                    .cartesianProduct()
                when {
                    altSources.isEmpty() -> emptyList<Unit>() // no match for this rule - a valid situation
                    else -> altSources.map { alt ->
                        val expression = rule.expression[m2mExecution.targetDomainRef]
                        when (expression) {
                            null -> m2mExecution.errorIssue("No expression found for target domain ref '$${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                            else -> {
                                val allVars = alt.values.merge()
                                val evcForWhen = EvaluationContext.of(ruleArguments + allVars.matchedVariables)
                                if (isAlreadyApplied(m2mExecution, rule, allVars)) return@map Unit
                                val whenResult = rule.when_?.let {
                                    executeWhen(m2mExecution, rule, it, evcForWhen)
                                } ?: Pair(true, evcForWhen)
                                if (whenResult.first) {
                                    markApplied(m2mExecution, rule, allVars)
                                    val varsAfterWhen = whenResult.second// + allVars.matchedVariables
                                    // construct
                                    val tgtExpr = rule.expression[m2mExecution.targetDomainRef]
                                        ?: error("No expression found for target domain '${m2mExecution.targetDomainRef.value}'") // should never happen
                                    val lhsType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type
                                        ?: error("No domainSignature found for target domain '${m2mExecution.targetDomainRef.value}'") // should never happen
                                    val tgtObj = createFromExpression(m2mExecution, varsAfterWhen, lhsType, tgtExpr)
                                    val mapping = rule.domainSignature.entries.associate { (dr, ds) ->
                                        val v = when (dr) {
                                            m2mExecution.targetDomainRef -> tgtObj
                                            else -> varsAfterWhen.getOrInParent(ds.variable.name.value)
                                                ?: error("No variable found for '${ds.variable.name.value}' of domain '$dr'.") // should never be error!
                                        }
                                        Pair(dr, v)
                                    }
                                    m2mExecution.addRecord(rule, mapping)

                                    // where
                                    val varsAfterWhere = when {
                                        rule.where.isEmpty() -> varsAfterWhen
                                        else -> rule.where.fold(varsAfterWhen) { acc, it ->
                                            executeWhere(m2mExecution, rule, it, acc)
                                        }
                                    }

                                    // setProperties
                                    setPropertiesFromExpression(m2mExecution, tgtObj, varsAfterWhere, tgtExpr)

                                } else {
                                    m2mExecution.infoIssue("when clause evaluated to false for target domain ref '${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private inline fun executeRelationInternal(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformRelation,
        source: Map<DomainReference, List<TypedObject>>,
        ruleArguments: Map<String, TypedObject>,
        matchSourceVariables: (m2mExecution: M2mTransformExecution, rule: M2mTransformPatternRule, source: Map<DomainReference, List<TypedObject>>) -> Map<DomainReference, TemplateMatchAlternatives>,
        executeWhen: (m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, when_: Expression, evc: EvaluationContext) -> Pair<Boolean, EvaluationContext>,
        executeRelationMainBody: (m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, srcMatch: Map<DomainReference, TemplateMatchResult>, varsAfterWhen: EvaluationContext, whereSteps: List<ExecutionStepCombined>) -> Unit
    ) {
        m2mExecution.evaluationStep("Executing relation rule '${rule.name.value}'.")
        val domToListOfAlts = matchSourceVariables(m2mExecution, rule, source)
        when {
            domToListOfAlts.isEmpty() -> m2mExecution.warnIssue("No matches found in source domains for rule '${rule.name}'.")
            else -> {
                val altSources = domToListOfAlts
                    .map { (k, v) -> Pair(k, v.alternatives) }
                    .toMap()
                    .cartesianProduct()
                when {
                    altSources.isEmpty() -> emptyList<Unit>() //no match for this rule - a valid situation
                    else -> altSources.map { alt ->
                        val allVars = alt.values.merge()
                        if (isAlreadyApplied(m2mExecution, rule, allVars)) return@map Unit
                        val evcForWhen = EvaluationContext.of(ruleArguments + allVars.matchedVariables)
                        val whenResult = rule.when_?.let { executeWhen(m2mExecution, rule, it, evcForWhen) }
                            ?: Pair(true, evcForWhen)

                        if (whenResult.first) {
                            markApplied(m2mExecution, rule, allVars)
                            val whereClauseSteps = compileRelationWhereClause(m2mExecution, rule, whenResult.second)
                            executeRelationMainBody(m2mExecution, rule, alt, whenResult.second, whereClauseSteps)
                        } else {
                            m2mExecution.evaluationTrace("when clause evaluated to false for target domain ref '${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                        }

                    }
                }
            }
        }
    }

    private inline fun executeTableInternal(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformTable,
        source: Map<DomainReference, List<TypedObject>>,
        ruleArguments: Map<String, TypedObject>,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject
    ) {
        m2mExecution.evaluationStep("Executing table rule '${rule.name.value}'.")
        source.cartesianProduct().mapNotNull { srcAlt ->
            val mapping = rule.values.firstNotNullOfOrNull { vs ->
                val srcValues = srcAlt.mapNotNull { (srcDomainRef, v) ->
                    val srcObjectGraph = m2mExecution.domainAccessorMutator[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
                    val evc = EvaluationContext.of(emptyMap<String, TypedObject>()) //TODO: are there any variables !
                    val drExp = vs[srcDomainRef] ?: error("")
                    val value = evaluate(srcObjectGraph, evc, drExp)
                    val match = srcObjectGraph.equalTo(v, value)
                    if (match) {
                        Pair(srcDomainRef, value)
                    } else {
                        null
                    }
                }
                // if found a match, add the target value
                if (srcValues.isEmpty()) {
                    null
                } else {
                    val evc = EvaluationContext.of(emptyMap<String, TypedObject>()) //TODO: are there any variables !
                    val drExp = vs[m2mExecution.targetDomainRef] ?: error("")
                    val value = evaluate(m2mExecution.targetAccessorMutator, evc, drExp)
                    srcValues + Pair(m2mExecution.targetDomainRef, value)
                }
            }?.toMap()
            mapping?.let {
                m2mExecution.addRecord(rule, mapping)
            }
        }
    }

    private inline fun matchSourceVariablesInternal(
        m2mExecution: M2mTransformExecution, //FIXME: only needed for 'issues' - just pass issues!
        rule: M2mTransformPatternRule,
        source: Map<DomainReference, List<TypedObject>>,
        matchVariablesFromRhs: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject, rhs: PropertyTemplateRhs) -> TemplateMatchAlternatives
    ): Map<DomainReference, TemplateMatchAlternatives> {
        val srcDomainRefs = rule.domainSignature.filterKeys { k -> k != m2mExecution.targetDomainRef }
        val results = srcDomainRefs.map { (srcDomainRef, srcDomainItem) ->
            val srcDomainType = rule.domainSignature[srcDomainRef]?.variable?.type ?: error("Source domain type not found for domain '$srcDomainRef'")
            val srcOg = m2mExecution.domainAccessorMutator[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
            val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
            // match variables from source domain
            val srcList = source[srcDomainRef] ?: error("No source object found for domain '$srcDomainRef'")
            //filter list to only those objects whoes types match this rule
            val filtered = srcList.filter { it.type.conformsTo(srcDomainType) }
            val alts = filtered.map { src ->
                matchVariablesFromRhs(m2mExecution, EvaluationContext.of(emptyMap()), srcOg, src, srcObjPat)
            }.filter { it.isMatch }
            Pair(srcDomainRef, TemplateMatchAlternatives(alts.flatMap { it.alternatives }))
        }
        // results contains, for each source domain, a list of alternative MatchResults
        return results.toMap()
    }

    private inline fun matchVariablesFromRhsInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        accessorMutator: ObjectGraphAccessorMutator,
        src: TypedObject,
        rhs: PropertyTemplateRhs,
        matchVariablesFromPropertyTemplateExpression: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, lhs: TypedObject, rhs: PropertyTemplateExpression) -> TemplateMatchResult,
        matchVariablesFromObjectTemplate: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, objectTemplate: ObjectTemplate, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject) -> TemplateMatchAlternatives,
        matchVariablesFromCollectionTemplate: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, collectionTemplate: CollectionTemplate, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject) -> TemplateMatchAlternatives
    ): TemplateMatchAlternatives =
        when (rhs) {
            is PropertyTemplateExpression -> {
                val mr = matchVariablesFromPropertyTemplateExpression(m2mExecution, evc, accessorMutator, src, rhs)
                // only keep the result if it is a match
                mr.takeIf { it.isMatch }?.let { TemplateMatchAlternatives(listOf(it)) } ?: TemplateMatchAlternatives(emptyList())
            }

            is ObjectTemplate -> matchVariablesFromObjectTemplate(m2mExecution, evc, rhs, accessorMutator, src)
            is CollectionTemplate -> matchVariablesFromCollectionTemplate(m2mExecution, evc, rhs, accessorMutator, src)
            else -> error("Unknown rhs type ${rhs::class}")
        }.let { tma ->
            rhs.identifier?.let { id ->
                tma.withVariable(id.value, src)
            } ?: tma
        }

    private inline fun matchVariablesFromPropertyTemplateExpressionInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        accessorMutator: ObjectGraphAccessorMutator,
        lhs: TypedObject,
        rhs: PropertyTemplateExpression,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TemplateMatchResult {
        // either :
        // 1) rhs is a free-variable with no value set, in which case set it to the lhs
        // 2) rhs is an expression with a value that matches the lhs
        val expr = rhs.expression
        return when {
            expr is RootExpression && null == evc.getOrInParent(expr.name) -> {
                TemplateMatchResult(true, mapOf(expr.name to lhs))
            }

            else -> {
                val value = evaluate(accessorMutator, evc, expr)
                val isMatch = accessorMutator.equalTo(lhs, value)
                TemplateMatchResult(isMatch, emptyMap())
            }
        }
    }

    private inline fun matchVariablesFromObjectTemplateInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        objectTemplate: ObjectTemplate,
        accessorMutator: ObjectGraphAccessorMutator,
        src: TypedObject,
        getProperty: (obj: TypedObject, name: String) -> TypedObject,
        matchVariablesFromRhs: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject, rhs: PropertyTemplateRhs) -> TemplateMatchAlternatives
    ): TemplateMatchAlternatives {
        // as QVT Relations: an object template matches only an object (not nothing) that conforms to the template's type
        val objType = objectTemplate.type
        return when {
            src.isNothing -> TemplateMatchAlternatives(emptyList())
            null != objType && src.type.conformsTo(objType).not() -> TemplateMatchAlternatives(emptyList())
            objectTemplate.propertyTemplate.isEmpty() -> TemplateMatchAlternatives(listOf(TemplateMatchResult.EMPTY()))
            else -> {
                val propTemplateMatches = objectTemplate.propertyTemplate.map { (k, v) ->
                    val rhsPat = v.rhs
                    val lhs = getProperty(src, k.value)
                    matchVariablesFromRhs(m2mExecution, evc, accessorMutator, lhs, rhsPat)
                } //TODO: determine no match before cartesianProduct
                val result = propTemplateMatches.map { it.alternatives }.cartesianProduct()
                val alts = result.map { l -> l.merge() }.filter { it.isMatch }
                TemplateMatchAlternatives(alts)
            }
        }
    }

    private inline fun matchVariablesFromCollectionTemplateInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        collectionTemplate: CollectionTemplate,
        accessorMutator: ObjectGraphAccessorMutator,
        src: TypedObject,
        matchVariablesFromCollectionTemplateToElementsIn: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, collectionTemplate: CollectionTemplate, accessorMutator: ObjectGraphAccessorMutator, elements: List<TypedObject>) -> List<TemplateMatchResult>
    ): TemplateMatchAlternatives {
        val result = when {
            src.type.isCollection -> {
                // TODO: maybe a faster way to do it if NOT isSubset!
                val elements = mutableListOf<TypedObject>()
                accessorMutator.forEachIndexed(src) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                if (collectionTemplate.isSubset.not() && elements.size != collectionTemplate.elements.size) {
                    // TODO: should return a no match not error!
                    error("Collection size does not match template size: ${elements.size} != ${collectionTemplate.elements.size} ")
                }
                matchVariablesFromCollectionTemplateToElementsIn(m2mExecution, evc, collectionTemplate, accessorMutator, elements)
            }

            else -> error("src is not a collection")
        }
        return TemplateMatchAlternatives(result)
    }

    private inline fun matchVariablesFromCollectionTemplateToElementsInInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        collectionTemplate: CollectionTemplate,
        accessorMutator: ObjectGraphAccessorMutator,
        elements: List<TypedObject>,
        matchVariablesFromRhs: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject, rhs: PropertyTemplateRhs) -> TemplateMatchAlternatives
    ): List<TemplateMatchResult> {
        val options = findCoveringSubsets2(
            cover = collectionTemplate.elements,
            bySubsetsOf = elements
        ) { tp, el ->
            val mr = matchVariablesFromRhs(m2mExecution, evc, accessorMutator, el, tp)
            Pair(mr.alternatives.isNotEmpty(), mr)
        }
        val res = options.map { opt ->
            opt.flatMap { it.alternatives }.merge()
        }
        return res
    }

    private inline fun executeWhenInternal(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        when_: Expression,
        evc: EvaluationContext,
        getSource: (m2mExecution: M2mTransformExecution, rule: M2mTransformRule, arguments: Map<DomainReference, Expression>, evc: EvaluationContext) -> Map<DomainReference, List<TypedObject>>,
        executeRule: (m2mExecution: M2mTransformExecution, rule: M2mTransformRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) -> Unit,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject
    ): Pair<Boolean, EvaluationContext> {
        // TODO: use an extended Expression evaluator that handles the RuleWhen options, so we can have compound when-expressions
        return when (when_) {
            is RuleWhenRelationHolds, is RuleWhenMappingHolds -> when_.resolved?.let { rule ->
                // as QVT: a rule in 'when' is a precondition, it holds if the rule was applied with these source values
                val source = getSource(m2mExecution, rule, when_.domainArguments, evc)
                val found = findAppliedRecord(m2mExecution, rule, source) ?: run {
                    // as QVT, a rule in 'when' that was not yet applied for these values is applied first
                    if (m2mExecution.rulesAppliedForWhen.add(rule)) {
                        val ruleArguments = when_.ruleArguments.entries.associate { Pair(it.key.value, evaluate(m2mExecution.targetAccessorMutator, evc, it.value)) }
                        executeRule(m2mExecution, rule, source, ruleArguments)
                        m2mExecution.rulesAppliedForWhen.remove(rule)
                        findAppliedRecord(m2mExecution, rule, source)
                    } else null
                }
                found?.let { Pair(true, bindWhenTarget(m2mExecution, owningRule, when_, found[m2mExecution.targetDomainRef], evc)) } ?: Pair(false, evc)
            } ?: run {
                m2mExecution.errorIssue("In 'when' clause of rule '${owningRule.name.value}', rule '${when_.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                Pair(false, evc)
            }

            is RuleWhenRelationHoldsForAll, is RuleWhenMappingHoldsForAll -> when_.resolved?.let { rule ->
                val source = getSource(m2mExecution, rule, when_.domainArguments, evc)
                when (val elements = sourceElementsForAll(m2mExecution, owningRule, when_, source)) {
                    null -> Pair(false, evc)
                    else -> {
                        val found = elements.map { el ->
                            findAppliedRecord(m2mExecution, rule, el) ?: run {
                                // as QVT, a rule in 'when' that was not yet applied for these values is applied first
                                if (m2mExecution.rulesAppliedForWhen.add(rule)) {
                                    val ruleArguments = when_.ruleArguments.entries.associate { Pair(it.key.value, evaluate(m2mExecution.targetAccessorMutator, evc, it.value)) }
                                    executeRule(m2mExecution, rule, el, ruleArguments)
                                    m2mExecution.rulesAppliedForWhen.remove(rule)
                                    findAppliedRecord(m2mExecution, rule, el)
                                } else null
                            }
                        }
                        whenForAllResult(m2mExecution, owningRule, when_, rule, found, evc)
                    }
                }
            } ?: run {
                m2mExecution.errorIssue("In 'when' clause of rule '${owningRule.name.value}', rule '${when_.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                Pair(false, evc)
            }
            else -> {
                val srcOg = m2mExecution.domainAccessorMutator.entries.filterNot { it.key == m2mExecution.targetDomainRef }.first().value //should never be null!
                //FIXME: which og to use? all might be needed!
                val res = evaluate(srcOg, evc, when_)
                val v: Boolean = when {
                    res.type.conformsTo(StdLibDefault.Boolean) -> {
                        srcOg.valueOf(res) as Boolean
                    }

                    else -> TODO()
                }
                Pair(v, evc)
            }
        }
    }

    private inline fun checkRelationInternal(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformRelation,
        domainArguments: Map<DomainReference, Expression>,
        callerEvc: EvaluationContext,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject,
        matchVariablesFromRhs: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, accessorMutator: ObjectGraphAccessorMutator, src: TypedObject, rhs: PropertyTemplateRhs) -> TemplateMatchAlternatives,
        lookupTargetViaBoundNeighbors: (m2mExecution: M2mTransformExecution, template: PropertyTemplateRhs, tgtVarName: String, evc: EvaluationContext) -> TypedObject?,
        executeWhen: (m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, when_: Expression, evc: EvaluationContext) -> Pair<Boolean, EvaluationContext>,
        checkRelation: (m2mExecution: M2mTransformExecution, rule: M2MTransformRelation, domainArguments: Map<DomainReference, Expression>, callerEvc: EvaluationContext) -> Pair<Boolean, EvaluationContext>
    ): Pair<Boolean, EvaluationContext> {

        // --- STEP 1: MAP INCOMING ARGUMENTS TO LOCAL PARAMETERS ---
        val localBindings = mutableMapOf<String, TypedObject>()
        rule.domainSignature.forEach { (domainRef, signature) ->
            val argExpr = domainArguments[domainRef]
            if (argExpr != null) {
                val og = m2mExecution.domainAccessorMutator[domainRef] ?: error("ObjectGraph not found for domain $domainRef")
                val resolvedArg = evaluate(og, callerEvc, argExpr)

                val localParamName = signature.variable.name.value
                localBindings[localParamName] = resolvedArg
            }
        }
        // Create an isolated child context for the relation's internal matching execution
        val childInitialEvc = EvaluationContext.of(localBindings)

        // --- STEP 2: INTERNAL DOMAIN PATTERN MATCHING ---
        // Gather all local variations that satisfy the internal patterns
        var currentAlternatives = listOf(childInitialEvc)

        rule.domainTemplate.forEach { (domainRef, objectTemplate) ->
            val og = m2mExecution.domainAccessorMutator[domainRef] ?: error("ObjectGraph not found for domain $domainRef")
            val nextAlternatives = mutableListOf<EvaluationContext>()

            for (altEvc in currentAlternatives) {
                val localParamName = rule.domainSignature[domainRef]?.variable?.name?.value ?: error("Domain signature name not found")
                val alreadyBoundObj = altEvc.getOrInParent(localParamName)

                if (alreadyBoundObj != null && !og.isNothing(alreadyBoundObj)) {
                    // Scenario A: The parameter was passed in by the caller. Validate it.
                    val matchResult = matchVariablesFromRhs(m2mExecution, altEvc, og, alreadyBoundObj, objectTemplate)
                    if (matchResult.alternatives.isNotEmpty()) {
                        matchResult.alternatives.forEach { nextAlternatives.add(altEvc.child(it.matchedVariables)) }
                    }
                } else {
                    // Scenario B: Parameter wasn't passed. Look it up using neighbors or trace records.
                    val candidateObjects = mutableListOf<TypedObject>()

                    // Fallback check 1: Trace records
                    val record = m2mExecution.records[rule]
                    record?.alternatives?.mapNotNull { it[domainRef] }?.let { candidateObjects.addAll(it) }

                    // Fallback check 2: Neighbor template tracking (like finding 'sm' from bound 'pd')
                    val targetVarName = rule.domainSignature[domainRef]!!.variable.name.value
                    val neighborCandidate = lookupTargetViaBoundNeighbors(m2mExecution, objectTemplate, targetVarName, altEvc)
                    if (neighborCandidate != null) candidateObjects.add(neighborCandidate)

                    for (candidate in candidateObjects) {
                        val matchResult = matchVariablesFromRhs(m2mExecution, altEvc, og, candidate, objectTemplate)
                        if (matchResult.alternatives.isNotEmpty()) {
                            matchResult.alternatives.forEach {
                                val newFrame = altEvc.child(it.matchedVariables)
                                newFrame.setNamedValue(localParamName, candidate)
                                nextAlternatives.add(newFrame)
                            }
                        }
                    }
                }
            }
            currentAlternatives = nextAlternatives
            if (currentAlternatives.isEmpty()) return Pair(false, callerEvc) // Early exit if any domain match falls through
        }

        // --- STEP 3: RECURSIVE CLAUSE VALIDATION ---
        // Find the first alternative context that satisfies internal pre/post conditions
        val winningAlternative = currentAlternatives.firstOrNull { altEvc ->
            // 1. Evaluate internal 'when' clause
            val whenPassed = rule.when_?.let {
                executeWhen(m2mExecution, rule, it, altEvc).first
            } ?: true

            if (!whenPassed) return@firstOrNull false

            // 2. Evaluate internal 'where' clauses
            val wherePassed = rule.where.all { rw ->
                val childWhereRule = rw.resolved!! as M2MTransformRelation
                checkRelation(m2mExecution, childWhereRule, rw.domainArguments, altEvc).first
            }

            wherePassed
        }

        // --- STEP 4: PREPARE PAIR RETURN FOR CALLER ---
        return if (winningAlternative != null) {
            // Create an isolated return frame on top of the caller context to receive values
            val outputFrame = callerEvc.child()

            rule.domainSignature.forEach { (domainRef, signature) ->
                val argExpr = domainArguments[domainRef]
                if (argExpr is RootExpression) { // E.g. mapping internal 'sm' back to caller's 'myStateMachineVar'
                    val localParamName = signature.variable.name.value
                    val resolvedValue = winningAlternative.getOrInParent(localParamName)
                    if (resolvedValue != null) {
                        outputFrame.setNamedValue(argExpr.name, resolvedValue)
                    }
                }
            }
            Pair(true, outputFrame)
        } else {
            Pair(false, callerEvc)
        }
    }

    private inline fun lookupTargetViaBoundNeighborsInternal(
        m2mExecution: M2mTransformExecution,
        template: PropertyTemplateRhs,
        tgtVarName: String,
        evc: EvaluationContext,
        getProperty: (obj: TypedObject, name: String) -> TypedObject,
        lookupTargetViaBoundNeighbors: (m2mExecution: M2mTransformExecution, template: PropertyTemplateRhs, tgtVarName: String, evc: EvaluationContext) -> TypedObject?
    ): TypedObject? {
        if (template !is ObjectTemplate) return null

        // Scan properties of this template to find nested elements that are already bound
        template.propertyTemplate.forEach { (propName, propTemplate) ->
            val rhs = propTemplate.rhs
            if (rhs is ObjectTemplate) {
                val nestedId = rhs.identifier?.value
                val boundNeighbor = nestedId?.let { evc.getOrInParent(it) }

                if (boundNeighbor != null && !m2mExecution.targetAccessorMutator.isNothing(boundNeighbor)) {
                    // We found a bound neighbor (like 'pd' inside the StateMachine template)!
                    // Look at the nested template properties to see if it points back to our target variable name
                    rhs.propertyTemplate.forEach { (neighborPropName, neighborPropTemplate) ->
                        val neighborRhs = neighborPropTemplate.rhs
                        if (neighborRhs is PropertyTemplateExpression && neighborRhs.expression is RootExpression) {
                            if ((neighborRhs.expression as RootExpression).name == tgtVarName) {
                                // Jackpot! The neighbor's property (e.g., pd.observableStateMachine) points to our target variable (sm)
                                val candidate = getProperty(boundNeighbor, neighborPropName.value)
                                if (!m2mExecution.targetAccessorMutator.isNothing(candidate)) {
                                    return candidate
                                }
                            }
                        }
                    }
                }
                // Recurse deeper into nested object templates if needed
                val deepMatch = lookupTargetViaBoundNeighbors(m2mExecution, rhs, tgtVarName, evc)
                if (deepMatch != null) return deepMatch
            }
        }
        return null
    }

    private inline fun executeWhereInternal(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        where: RuleWhere,
        evc: EvaluationContext,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject,
        evaluateArguments: (m2mExecution: M2mTransformExecution, arguments: Map<DomainReference, Expression>, evc: EvaluationContext) -> Map<DomainReference, List<TypedObject>>,
        executeRuleWhere: (m2mExecution: M2mTransformExecution, owningRule: M2mTransformRule, rule: M2mTransformRule, ruleArguments: Map<String, TypedObject>, source: Map<DomainReference, List<TypedObject>>) -> TypedObject?
    ): EvaluationContext {
        val ruleArguments = where.ruleArguments.entries.associate {
            val value = evaluate(m2mExecution.targetAccessorMutator, evc, it.value)
            Pair(it.key.value, value)
        }
        return when (where) { //TODO: support more complex expressions - override the expression interpreter to intercept function calls as rule-calls
            is RuleWhereCallRelation, is RuleWhereCallMapping -> where.resolved?.let { rule ->
                val boundArgs = evaluateArguments(m2mExecution, where.domainArguments, evc)
                val tgtValue = executeRuleWhere(m2mExecution, owningRule, rule, ruleArguments, boundArgs)
                if (null == tgtValue) m2mExecution.warnIssue("In rule '${owningRule.name.value}' the 'where' clause matched nothing.")
                val targetArg = where.domainArguments[m2mExecution.targetDomainRef]
                when (targetArg) {
                    null -> {
                        m2mExecution.errorIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' not found in rule call '${where.ruleName.value}' in 'where' clause of TransformRuleSet '${m2mExecution.targetTransform.name.value}'."
                        )
                        evc
                    }

                    is RootExpression -> when (tgtValue) {
                        null -> evc.setNamedValue(targetArg.name, m2mExecution.targetAccessorMutator.nothing())
                        else -> evc.setNamedValue(targetArg.name, tgtValue)
                    }

                    else -> {
                        m2mExecution.warnIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${where.ruleName.value}' in 'where' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                        )
                        evc
                    }
                }
            } ?: run {
                m2mExecution.errorIssue("In 'where' clause of rule '${owningRule.name.value}', rule '${where.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                evc
            }

            is RuleWhereCallRelationForAll, is RuleWhereCallMappingForAll -> where.resolved?.let { rule ->
                val tgtElemType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type ?: StdLibDefault.AnyType
                val tgtType = StdLibDefault.List.type(listOf(tgtElemType.asTypeArgument)) //TODO: get this from tgt variable
                val boundArgs = evaluateArguments(m2mExecution, where.domainArguments, evc)
                val tgtValue = when {
                    boundArgs.all { (k, v) -> v.all { it.type.isCollection } } -> {
                        val sourceElements = mutableListOf<Map<DomainReference, List<TypedObject>>>()
                        val sourceCollType = mutableListOf<TypeInstance>()
                        boundArgs.entries.forEach { (dr, alts) ->
                            alts.forEach { to ->
                                sourceCollType.add(to.type)
                                val og = m2mExecution.domainAccessorMutator[dr]!!
                                og.forEachIndexed(to) { idx, el ->
                                    if (idx >= sourceElements.size) {
                                        sourceElements.add(mutableMapOf())
                                    }
                                    if (null == sourceElements[idx][dr]) {
                                        (sourceElements[idx] as MutableMap)[dr] = mutableListOf()
                                    }
                                    (sourceElements[idx][dr] as MutableList).add(el)
                                }
                            }
                        }
                        val tgtList = sourceElements.mapNotNull { src ->
                            executeRuleWhere(m2mExecution, owningRule, rule, ruleArguments, src)
                        }
                        // warn only when no element matched
                        if (sourceElements.isNotEmpty() && tgtList.isEmpty()) m2mExecution.warnIssue("In rule '${owningRule.name.value}' the 'where' clause matched nothing.")
                        //if(sourceCollType.all { it. }) //TODO: check all colls are type of tgt
                        when {
                            sourceCollType.isEmpty() -> m2mExecution.targetAccessorMutator.nothing()
                            else -> m2mExecution.targetAccessorMutator.createCollection(tgtType, tgtList)
                        }
                    }

                    else -> {
                        m2mExecution.errorIssue(
                            "In 'where' clause of rule '${owningRule.name.value}' in '${m2mExecution.targetTransform.name.value}', the all call to rule '${where.ruleName.value}' is expecting a collection."
                        )
                        null
                    }
                }
                val targetArg = where.domainArguments[m2mExecution.targetDomainRef]
                when (targetArg) {
                    null -> {
                        m2mExecution.errorIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' not found in rule call '${where.ruleName.value}' in 'where' clause of TransformRuleSet '${m2mExecution.targetTransform.name.value}'."
                        )
                        evc
                    }

                    is RootExpression -> when (tgtValue) {
                        null -> evc.setNamedValue(targetArg.name, m2mExecution.targetAccessorMutator.nothing())
                        else -> evc.setNamedValue(targetArg.name, tgtValue)
                    }

                    else -> {
                        m2mExecution.warnIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${where.ruleName.value}' in 'where' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                        )
                        evc
                    }
                }
            } ?: run {
                m2mExecution.errorIssue("In 'where' clause of rule '${owningRule.name.value}', rule '${where.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                evc
            }

            else -> {
                m2mExecution.errorIssue("Cannot execute the 'where' clause.")
                evc
            }
        }
    }

    private inline fun executeRuleWhereInternal(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        rule: M2mTransformRule,
        ruleArguments: Map<String, TypedObject>,
        source: Map<DomainReference, List<TypedObject>>,
        executeRule: (m2mExecution: M2mTransformExecution, rule: M2mTransformRule, source: Map<DomainReference, List<TypedObject>>, ruleArguments: Map<String, TypedObject>) -> Unit
    ): TypedObject? {
        var mappingRecord = m2mExecution.records[rule]
        val match = when {
            null == mappingRecord -> {
                executeRule(m2mExecution, rule, source, ruleArguments)
                mappingRecord = m2mExecution.records[rule]
                mappingRecord?.let { findMatchInMappingRecord(m2mExecution, mappingRecord, source) }
            }

            else -> {
                val matchFromExisting = findMatchInMappingRecord(m2mExecution, mappingRecord, source)
                when {
                    null == matchFromExisting -> {
                        executeRule(m2mExecution, rule, source, ruleArguments)
                        mappingRecord = m2mExecution.records[rule]
                        mappingRecord?.let { findMatchInMappingRecord(m2mExecution, mappingRecord, source) }
                    }

                    else -> matchFromExisting
                }
            }
        }

        return match // the caller warns if nothing matched
    }

    private inline fun evaluateArgumentsInternal(
        m2mExecution: M2mTransformExecution,
        arguments: Map<DomainReference, Expression>,
        evc: EvaluationContext,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject
    ): Map<DomainReference, List<TypedObject>> {
        return arguments.mapNotNull { (domainRef, expr) ->
            if (expr is RootExpression) { // shortcut, saves evaluating a simple var name
                val value = evc.getOrInParent(expr.name)
                // a source value of $nothing is a value (it matches only $nothing), it is not unbound
                val isUnbound = null == value || (domainRef == m2mExecution.targetDomainRef && m2mExecution.targetAccessorMutator.isNothing(value))
                if (isUnbound.not()) {
                    Pair(domainRef, listOf(value!!))
                } else {
                    null // Omit from the map if it's currently unbound
                }
            } else {
                val og = m2mExecution.domainAccessorMutator[domainRef] ?: error("Domain Reference '${domainRef.value}' not found.")
                val res = evaluate(og, evc, expr)
                Pair(domainRef, listOf(res))
            }
        }.toMap()
    }

    private inline fun getSourceInternal(
        m2mExecution: M2mTransformExecution,
        rule: M2mTransformRule,
        arguments: Map<DomainReference, Expression>,
        evc: EvaluationContext,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject
    ): Map<DomainReference, List<TypedObject>> {
        return (arguments - m2mExecution.targetDomainRef).mapValues { (k, v) ->
            val og = m2mExecution.domainAccessorMutator[k] ?: error("Cannot find ObjectGraph for domain reference '${k.value}'.")
            val res = evaluate(og, evc, v)
            listOf(res)
        }
    }

    //TODO: merge code with above efficiently
    private inline fun evaluateArgsInternal(
        m2mExecution: M2mTransformExecution,
        rule: M2mTransformRule,
        arguments: List<Expression>,
        matchedVariables: Map<String, TypedObject>,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject
    ): Map<DomainReference, List<TypedObject>> {
        val argsValues = arguments.mapIndexed { idx, argExpr ->
            val domainRef = rule.domainSignature.keys.elementAt(idx)
            Pair(domainRef, argExpr)
        }.associate { it }
        return argsValues.mapValues { (k, v) ->
            val og = m2mExecution.domainAccessorMutator[k] ?: error("Cannot find ObjectGraph for domain reference '${k.value}'.")
            val evc = EvaluationContext.of(matchedVariables)
            val res = evaluate(og, evc, v)
            listOf(res)
        }
    }

    private inline fun createFromRhsInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        rhs: PropertyTemplateRhs,
        createFromPropertyTemplateExpression: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateExpression) -> Pair<TypedObject, Map<String, TypedObject>>,
        createFromObjectTemplate: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, objectTemplate: ObjectTemplate) -> Pair<TypedObject, Map<String, TypedObject>>,
        createFromCollectionTemplate: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, collectionTemplate: CollectionTemplate) -> Pair<TypedObject, Map<String, TypedObject>>
    ): Pair<TypedObject, Map<String, TypedObject>> = when (rhs) {
        is PropertyTemplateExpression -> createFromPropertyTemplateExpression(m2mExecution, evc, lhsType, rhs)
        is ObjectTemplate -> createFromObjectTemplate(m2mExecution, evc, lhsType, rhs)
        is CollectionTemplate -> createFromCollectionTemplate(m2mExecution, evc, lhsType, rhs)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    private inline fun createFromPropertyTemplateExpressionInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        rhs: PropertyTemplateExpression,
        createFromExpression: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, expression: Expression) -> TypedObject
    ): Pair<TypedObject, Map<String, TypedObject>> {
        val o = createFromExpression(m2mExecution, evc, lhsType, rhs.expression)
        val mv = rhs.identifier?.let { mapOf(it.value to o) } ?: emptyMap()
        return Pair(o, mv)
    }

    private inline fun createFromExpressionInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        expression: Expression,
        constructObject: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: CreateObjectExpression) -> TypedObject,
        evaluate: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        return when (expression) {
            is CreateObjectExpression -> {
                constructObject(m2mExecution.targetAccessorMutator, evc, expression)
            }

            else -> {
                val value = evaluate(m2mExecution.targetAccessorMutator, evc, expression)
                value
            }
        }
    }

    private inline fun createFromObjectTemplateInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        objectTemplate: ObjectTemplate,
        createFromRhs: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateRhs) -> Pair<TypedObject, Map<String, TypedObject>>,
        createStructureValue: (og: ObjectGraphAccessorMutator, qualifiedTypeName: net.akehurst.language.base.api.QualifiedName, constructorArgs: Map<String, TypedObject>) -> TypedObject
    ): Pair<TypedObject, Map<String, TypedObject>> {
        val tplType = objectTemplate.type
        if (null == tplType) {
            error("Cannot construct object for ObjectTemplate type not resolved")
        } else {
            val decl = tplType.resolvedDefinition
            return when (decl) {
                is DataType, is ValueType -> {
                    val matchedVars = mutableMapOf<String, TypedObject>()
                    val constructors = when (decl) {
                        is DataType -> decl.constructors
                        is ValueType -> decl.constructors
                        else -> error("Type '${decl.qualifiedName.value}' has no constructors")
                    }
                    val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
                    val conArgs = mutableMapOf<String, TypedObject>()
                    objectTemplate.propertyTemplate.forEach { (k, v) ->
                        if (possibleConArgNames.contains(k.value)) {
                            val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
                            val (value, mv) = createFromRhs(m2mExecution, evc, propType, v.rhs)
                            matchedVars.putAll(mv)
                            conArgs[k.value] = value
                        }
                    }
                    val o = createStructureValue(m2mExecution.targetAccessorMutator, tplType.qualifiedTypeName, conArgs)
                    val mv = objectTemplate.identifier?.let { matchedVars + Pair(it.value, o) } ?: matchedVars
                    Pair(o, mv)
                }

                else -> error("Cannot construct object of type ${tplType.qualifiedTypeName.value}")
            }
        }
    }

    private inline fun createFromCollectionTemplateInternal(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        collectionTemplate: CollectionTemplate,
        createFromRhs: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateRhs) -> Pair<TypedObject, Map<String, TypedObject>>,
        matchVariablesFromCollectionTemplateToElementsIn: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, collectionTemplate: CollectionTemplate, accessorMutator: ObjectGraphAccessorMutator, elements: List<TypedObject>) -> List<TemplateMatchResult>
    ): Pair<TypedObject, Map<String, TypedObject>> {
        //collection may already have been created, (via when/where/etc) and be a captured variable
        val existing = collectionTemplate.identifier?.let { evc.getOrInParent(it.value) }
        return when {
            null == existing -> {
                // create new collection from template elements
                val matchedVars = mutableMapOf<String, TypedObject>()
                val elements = collectionTemplate.elements.map {
                    val (o, mv) = createFromRhs(m2mExecution, evc, lhsType.typeArguments[0].type, it)
                    matchedVars.putAll(mv)
                    o
                }
                val col = m2mExecution.targetAccessorMutator.createCollection(lhsType, elements)
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                Pair(col, mv)
            }

            else -> {
                // try to match template elements against existing collection elements, if not matched then create them.
                val elements = mutableListOf<TypedObject>()
                m2mExecution.targetAccessorMutator.forEachIndexed(existing) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                val matches = matchVariablesFromCollectionTemplateToElementsIn(m2mExecution, evc, collectionTemplate, m2mExecution.targetAccessorMutator, elements)

                when {
                    matches.isEmpty() -> {
                        // create new collection from template elements
                        val matchedVars = mutableMapOf<String, TypedObject>()
                        val elements = collectionTemplate.elements.map {
                            val (o, mv) = createFromRhs(m2mExecution, evc, lhsType.typeArguments[0].type, it)
                            matchedVars.putAll(mv)
                            o
                        }
                        val col = m2mExecution.targetAccessorMutator.createCollection(lhsType, elements)
                        val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                        Pair(col, mv)
                    }

                    1 == matches.size -> {
                        val matched = matches.first()
                        matched.matchedVariables
                        TODO()
                    }

                    else -> TODO("not sue what to do here yet")
                }
            }
        }
    }

    private inline fun setPropertiesFromRhsInternal(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        rhs: PropertyTemplateRhs,
        setPropertiesFromExpression: (m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, expression: Expression) -> Unit,
        setPropertiesFromObjectTemplate: (m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, objectTemplate: ObjectTemplate) -> Unit,
        setPropertiesFromCollectionTemplate: (m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, collectionTemplate: CollectionTemplate) -> Unit
    ) = when (rhs) {
        is PropertyTemplateExpression -> setPropertiesFromExpression(m2mExecution, obj, evc, rhs.expression)
        is ObjectTemplate -> setPropertiesFromObjectTemplate(m2mExecution, obj, evc, rhs)
        is CollectionTemplate -> setPropertiesFromCollectionTemplate(m2mExecution, obj, evc, rhs)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    private inline fun setPropertiesFromExpressionInternal(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        expression: Expression,
        propertyAssignmentBlock: (og: ObjectGraphAccessorMutator, evc: EvaluationContext, self: TypedObject, propertyAssignments: List<VariableAssignmentStatement>) -> Unit
    ) {
        when (expression) {
            is CreateObjectExpression -> {
                propertyAssignmentBlock(m2mExecution.targetAccessorMutator, evc, obj, expression.propertyAssignments)
            }

            else -> {
                // nothing to do, already done when createFromPropertyPatternExpression was called
            }
        }
    }

    private inline fun setPropertiesFromObjectTemplateInternal(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        objectTemplate: ObjectTemplate,
        getProperty: (obj: TypedObject, name: String) -> TypedObject,
        createFromRhs: (m2mExecution: M2mTransformExecution, evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateRhs) -> Pair<TypedObject, Map<String, TypedObject>>,
        setPropertiesFromRhs: (m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, rhs: PropertyTemplateRhs) -> Unit
    ) {
        val tplType = objectTemplate.type
        if (null == tplType) {
            error("Cannot setPropertiesFromObjectTemplate type not resolved")
        } else {
            val decl = tplType.resolvedDefinition
            return when (decl) {
                // only DataTypes have properties that can be set
                is DataType -> {
                    val propValues = mutableMapOf<String, TypedObject>()
                    objectTemplate.propertyTemplate.forEach { (k, v) ->
                        val propType = obj.type.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType

                        // TODO: if property is a constructor arg, it is already set, else it will have no value yet
                        // can we deduce this rather than getting all properties and just checking for nothing

                        val possiblePv = getProperty(obj, k.value)
                        val pv = when {
                            m2mExecution.targetAccessorMutator.isNothing(possiblePv) -> {
                                val (o, vars) = createFromRhs(m2mExecution, evc, propType, v.rhs)
                                setPropertiesFromRhs(m2mExecution, o, evc, v.rhs)
                                o
                            }

                            else -> {
                                setPropertiesFromRhs(m2mExecution, possiblePv, evc, v.rhs)
                                possiblePv
                            }
                        }
                        propValues[k.value] = pv
                    }
                }

                else -> Unit
            }
        }
    }

    private inline fun setPropertiesFromCollectionTemplateInternal(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        collectionTemplate: CollectionTemplate,
        setPropertiesFromRhs: (m2mExecution: M2mTransformExecution, obj: TypedObject, evc: EvaluationContext, rhs: PropertyTemplateRhs) -> Unit
    ) {
        val elements = mutableListOf<TypedObject>()
        m2mExecution.targetAccessorMutator.forEachIndexed(obj) { i, el ->
            elements.add(el)
        }
        collectionTemplate.elements.map { elTemplate ->
            when {
                // if elTemplate is named, then it should be a variable we can et it
                null != elTemplate.identifier -> {
                    val el = evc.getOrInParent(elTemplate.identifier!!.value)
                    el?.let {
                        check(elements.contains(el)) //maybe not needed
                        setPropertiesFromRhs(m2mExecution, el, evc, elTemplate)
                    }
                }

                else -> TODO("How to identify a collection template element?")
            }
        }
    }
}
