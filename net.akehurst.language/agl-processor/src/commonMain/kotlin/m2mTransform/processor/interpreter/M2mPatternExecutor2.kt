package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.kotlinx.collections.transitveClosure
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.CreateObjectExpressionDefault
import net.akehurst.language.expressions.asm.FunctionCallDefault
import net.akehurst.language.expressions.asm.InfixExpressionDefault
import net.akehurst.language.expressions.asm.LiteralExpressionDefault
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.PropertyCallDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.asm.StatementBlockExpressionDefault
import net.akehurst.language.expressions.asm.TernaryConditionExpressionDefault
import net.akehurst.language.expressions.asm.VariableAssignmentStatementDefault
import net.akehurst.language.expressions.asm.VariableDefinitionDefault
import net.akehurst.language.expressions.asm.WithExpressionDefault
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.ValueType
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.orEmpty
import kotlin.collections.plus

class M2mPatternExecution2(
    val description: String,
    val inputs: List<String>,
    val outputs: List<String>,
    val execution: (evc: EvaluationContext) -> Expression
) {
    var index = -1 //unset

    /** do 'self' execution before all of these */
    val doMeBefore = mutableSetOf<M2mPatternExecution2>()

    val doMeBeforeAll get() = this.doMeBefore.transitveClosure { it.doMeBefore }

    fun mustComeBefore(other: M2mPatternExecution2): Boolean = when {
        this.doMeBeforeAll.contains(other) -> true
        else -> false
    }

    override fun toString(): String = "[$index] ${description} | ${inputs} -> ${outputs} ^ [${doMeBefore.joinToString { it.index.toString() }}]"
}

class ExecutionStep(
    val description: String,
    val inputs: List<String>,
    val outputs: List<String>,
    val expression: Expression
) {
    var index = -1 //unset
    fun execute(evaluator: ExpressionsInterpreterOverTypedObject, evc: EvaluationContext) {
        val result = evaluator.evaluateExpression(evc, this.expression)
        val outputValues = evaluator.objectGraph.untyped(result) as Map<String, Any>
        outputValues.forEach { (k, v) ->
            val tv = evaluator.objectGraph.toTypedObject(v, StdLibDefault.AnyType)// TODO: why untype then retype this
            evc.setNamedValue(k, tv)
        }
    }
}


class M2mPatternExecutor2(
    val issues: IssueHolder,
    val accessorMutator: ObjectGraphAccessorMutator,
    initialExes: List<ExecutionStep>
) {

    companion object {
        const val RESULT = $$"$result"
    }

    internal var _nextTempVarNum = 0
    internal val _executions = initialExes.mapIndexed { index, execution -> execution.also { it.index = index } }.toMutableList()
    val executionPlan get() = _executions.topologicalSort(::compareExecutions)
    val executionExpression
        get() = executionPlan.joinToString(separator = "\n") {
            val expr = it.expression
            "// ${it.description}\n${expr.asString()}"
        }

    fun addExecution(value: ExecutionStep) {
        value.index = _executions.size
        _executions.add(value)
    }

    fun buildAndExecute(tgtName: String, template: PropertyTemplateRhs, lhsType: TypeInstance, evc: EvaluationContext) {
        build(evc, tgtName, template, lhsType)
        execute(evc, tgtName)
    }

    /**
     * initial vars are those passed in (from the source domains)
     * and those discovered from when clause matches.
     * The known variables and the template are used to discover more variables
     * properties with rhs expresion either provide a variable or not
     * properties with object or collection template either match the property
     * (and can as such recursively discover more variables)
     * or if the property is null, then requires object construction and thus all nested variables must be inputs not outputs
     */
    fun build(initialVariables: EvaluationContext, tgtName: String, template: PropertyTemplateRhs, tgtType: TypeInstance) {
        val lhs = initialVariables.getOrInParent(tgtName) ?: accessorMutator.nothing()
        val preComputedInputs = discoverKnownVariables(lhs, initialVariables, template)
        constructExecutions(tgtName, preComputedInputs, template, tgtType)
    }

    fun execute(evc: EvaluationContext, tgtName: String): TypedObject {
        val eval = ExpressionsInterpreterOverTypedObject(accessorMutator)
        val sorted = executionPlan
        val evcExecution = evc.child()
        for (count in 0 until sorted.size) {
            val pe = sorted[count]
            pe.execute(eval, evcExecution)
        }
        val value = evcExecution.getOrInParent(tgtName) ?: accessorMutator.nothing()
        return value
    }

    private fun createTempVariable() = "temp${_nextTempVarNum++}"

    /**
     * returns a simplified template where all property assignments are from variables
     * any non-identified template is given an artificial id.
     */
    private fun constructExecutions(
        tgtName: String,
        preComputedInputs:Map<PropertyTemplateRhs, List<String>>,
        template: PropertyTemplateRhs,
        lhsType: TypeInstance
    ): ExecutionStep {
        return when (template) {
            is PropertyTemplateExpression -> constructExecutionsFromPropertyTemplateExpression(tgtName, preComputedInputs, template, lhsType)
            is ObjectTemplate -> constructExecutionsFromObjectTemplate(tgtName, preComputedInputs, template, lhsType)
            is CollectionTemplate -> constructExecutionsFromCollectionTemplate(tgtName, preComputedInputs, template, lhsType)
            else -> error("Unknown rhs type ${template::class}")
        }
    }

    /*
     {
       outputValueName := <outputExpression>
       ...
       Set(Pair(<outputName>, <outputValueName>>),...).toMap()
     }
     */
    private fun createOutputs(outputExpressions: Map<String, Expression>): Expression {
        val assignments = outputExpressions.map { (n, e) ->
            VariableAssignmentStatementDefault(VariableDefinitionDefault(n, null), null, e)
        }
        val pairList = outputExpressions.map { (n, e) ->
            val pairContent = listOf(LiteralExpressionDefault(StdLibDefault.String.qualifiedTypeName, n), RootExpressionDefault(n))
            FunctionCallDefault("Pair".asPossiblyQualifiedName, pairContent)
        }
        val outputValuesSet = FunctionCallDefault("Set".asPossiblyQualifiedName, pairList)
        val mapExpr = NavigationExpressionDefault(outputValuesSet, listOf(PropertyCallDefault("asMap")))
        val block = StatementBlockExpressionDefault(assignments, mapExpr)
        return block
    }

    /**
     * returns a Map from PropertyTemplateRhs to list of output variable name if any
     */
    fun discoverKnownVariables(lhs: TypedObject, variables: EvaluationContext, template: PropertyTemplateRhs): Map<PropertyTemplateRhs, List<String>> {
        val map = mutableMapOf<PropertyTemplateRhs, List<String>>()
        // find variables in this template
        template.identifier?.let { v ->
            // if the variable has no value
            variables.getOrInParent(v.value)?.let {
                // let variable have value of property
                variables.setNamedValue(v.value, lhs)
                map[template] = listOf(v.value)
            }
        }
        when {
            accessorMutator.isNothing(lhs) -> Unit
            else -> when (template) {
                is PropertyTemplateExpression -> Unit
                is ObjectTemplate -> {
                    // find variables in sub properties
                    template.propertyTemplate.entries.map { (pn, pt) ->
                        val propVal = lhs.getProperty(pn.value)
                        map.putAll(discoverKnownVariables(propVal, variables, pt.rhs))
                    }
                }

                is CollectionTemplate -> {
                    //lhs must be a collection
                    val propColl = lhs.self as Collection<TypedObject>
                    propColl.forEach { el ->
                        template.elements.map { pt ->
                            map.putAll(discoverKnownVariables(el, variables, pt))
                        }
                    }
                }

                else -> error("Unknown template type ${template::class}")
            }
        }
        return map
    }

    /*
     <tgtName> == <var>? : <expression>
     */
    private fun constructExecutionsFromPropertyTemplateExpression(
        tgtName: String,
        preComputedInputs:Map<PropertyTemplateRhs, List<String>>,
        template: PropertyTemplateExpression,
        lhsType: TypeInstance
    ): ExecutionStep {
        val templateVarName = template.identifier?.value ?: createTempVariable()
        val rhsName = when {
            template.expression is RootExpression -> (template.expression as RootExpression).name
            else -> null
        }
        val rhsInput = rhsName?.let { listOf(it) } ?: emptyList()
        val inputs = rhsInput + preComputedInputs[template].orEmpty()
        val outputs = preComputedInputs[template]?.let { emptyList<String>() } ?: listOf(templateVarName)

        val description = "check or set: $templateVarName == $rhsName"
        val expr = when {
            null == template.identifier -> template.expression
            else -> setToExpressionOrVar(templateVarName, template.expression)
        }
        val outputExpr = createOutputs(mapOf(templateVarName to expr))
        val step = ExecutionStep(description, inputs, outputs, outputExpr)
        addExecution(step)
        return step
    }

    /*

     */
    private fun constructExecutionsFromObjectTemplate(
        tgtName: String,
        preComputedInputs:Map<PropertyTemplateRhs, List<String>>,
        template: ObjectTemplate,
        lhsType: TypeInstance
    ): ExecutionStep {
        val templateVarName = template.identifier?.value ?: createTempVariable()
        val typeName = template.type.qualifiedTypeName
        val inputs = preComputedInputs[template].orEmpty()
        val outputs = listOf(tgtName)
        val description = "Find '$templateVarName' or construct ${typeName.value}(...)"
        val expr = findOrEnforceObjectTemplate(templateVarName, template)
        val outputExpr = createOutputs(mapOf(tgtName to expr))
        val step = ExecutionStep(description, inputs, outputs, outputExpr)
        addExecution(step)
        return step
    }

    private fun constructExecutionsFromCollectionTemplate(
        tgtName: String,
        preComputedInputs:Map<PropertyTemplateRhs, List<String>>,
        template: CollectionTemplate,
        lhsType: TypeInstance
    ): ExecutionStep {
        val templateVarName = template.identifier?.value
        TODO()
    }

    /**
     * +1 if e1 comes after e2
     * -1 if e2 comes after e1
     * exe1 comes after exe2 when:
     *  exe1.input contains any of exe2.outputs
     *  exe2.doAfterMe contains exe1
     */
    private fun compareExecutions(exe1: ExecutionStep, exe2: ExecutionStep): Int {
        return when {
            //    exe2.doMeBefore.contains(exe1) -> 1 // do exe1 after exe2
            //    exe1.doMeBefore.contains(exe2) -> -1 // do exe2 after exe1
            //exe2.mustComeBefore(exe1) -> 1 // do parent after child
            //exe1.mustComeBefore(exe2) -> -1 // do parent after child
            exe2.outputs.any { exe1.inputs.contains(it) } -> 1 // do outputs before inputs
            exe1.outputs.any { exe2.inputs.contains(it) } -> -1 // do outputs before inputs
            else -> 0
        }
    }

    private fun createFromPropertyTemplateExpression(evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateExpression): Map<String, TypedObject> {
        val id = rhs.identifier?.value
        val existing = evc.namedValues[id]
        return when (existing) {
            null -> {
                val o = createFromExpression(evc, lhsType, rhs.expression)
                val mv = rhs.identifier?.let { mapOf(it.value to o) } ?: emptyMap()
                mv + Pair(RESULT, o)
            }

            else -> emptyMap()
        }

    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    private fun createFromExpression(evc: EvaluationContext, lhsType: TypeInstance, expression: Expression): TypedObject {
        val exprInterp = ExpressionsInterpreterOverTypedObject(accessorMutator)
        return when (expression) {
            is CreateObjectExpression -> {
                exprInterp.constructObject(evc, expression)
            }

            else -> {
                val value = exprInterp.evaluateExpression(evc, expression)
                value
            }
        }
    }

    private fun findInEnvOrCreateFromObjectTemplate(evc: EvaluationContext, lhsType: TypeInstance, template: ObjectTemplate, conArgNames: Map<String, String>): Map<String, TypedObject> {
        val id = template.identifier?.value
        val existing = id?.let { evc.getOrInParent(it) }
        return when (existing) {
            null -> {
                val decl = template.type.resolvedDefinition
                when (decl) {
                    is DataType, is ValueType -> {
                        val matchedVars = mutableMapOf<String, TypedObject>()
                        val constructors = when (decl) {
                            is DataType -> decl.constructors
                            is ValueType -> decl.constructors
                            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
                        }
                        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
                        val conArgs = mutableMapOf<String, TypedObject>()
                        template.propertyTemplate.forEach { (k, v) ->
                            if (possibleConArgNames.contains(k.value)) {
                                val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
                                val arg = evc.getOrInParent(conArgNames[k.value]!!)
                                arg?.let { conArgs[k.value] = arg }
                            }
                        }
                        //.resolveType(tgtObjectGraph.typesDomain)
                        val o = accessorMutator.createStructureValue(template.type.qualifiedTypeName, conArgs)
                        val mv = template.identifier?.let { matchedVars + Pair(it.value, o) } ?: matchedVars
                        mv + Pair(RESULT, o)
                    }

                    else -> error("Cannot construct object of type ${decl.qualifiedName.value}")
                }
            }

            else -> mapOf(RESULT to existing)
        }
    }

    private fun createFromCollectionTemplate(evc: EvaluationContext, lhsType: TypeInstance, collectionTemplate: CollectionTemplate, elementIds: List<String>): Map<String, TypedObject> {
        //collection may already have been created, (via when/where/etc) and be a captured variable
        val existing = collectionTemplate.identifier?.let { evc.namedValues[it.value] }
        return when {
            null == existing || accessorMutator.isNothing(existing) -> {
                // create new collection from template elements
                val matchedVars = mutableMapOf<String, TypedObject>()
                val els = elementIds.mapNotNull { evc.getOrInParent(it) }
                val col = accessorMutator.createCollection(lhsType, els)
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                mv + Pair(RESULT, col)
            }

            else -> {
                // try to match template elements against existing collection elements, if not matched then create them.
                val els = elementIds.mapNotNull { evc.getOrInParent(it) }
                val newEls = accessorMutator.createCollection(existing.type, els)
                val newColl = existing.accessor.collectionUnion(existing, newEls)
                val matchedVars = mutableMapOf<String, TypedObject>()
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, newColl) } ?: matchedVars
                mv + Pair(RESULT, newColl)
            }
        }
    }

    /**
     * sets the property value from the template (which should be a simple variable reference)
     * returns any new variable matches - I think none
     */
    private fun setPropertyIfNothing(obj: TypedObject, pn: String, pv: TypedObject) {
        val possiblePv = obj.getProperty(pn)
        when {
            accessorMutator.isNothing(possiblePv) -> obj.setProperty(pn, pv)
            else -> Unit
        }
    }


    private fun findOrEnforceRhsExpr(tgtName: String, rhs: PropertyTemplateRhs): Expression = when (rhs) {
        is PropertyTemplateExpression -> findOrEnforcePropertyTemplateExpression(tgtName, rhs)
        is ObjectTemplate -> findOrEnforceObjectTemplate(tgtName, rhs)
        is CollectionTemplate -> TODO()
        else -> error("Unknown PropertyTemplateRhs type: ${rhs::class}")
    }

    private fun enforceRhsExpr(rhs: PropertyTemplateRhs): Expression = when (rhs) {
        is PropertyTemplateExpression -> rhs.expression
        is ObjectTemplate -> enforceObjectTemplate(rhs)
        is CollectionTemplate -> TODO()
        else -> error("Unknown PropertyTemplateRhs type: ${rhs::class}")
    }

    private fun findOrEnforcePropertyTemplateExpression(tgtName: String, template: PropertyTemplateExpression): Expression {
        return setToExpressionOrVar(tgtName, template.expression)
    }

    private fun findOrEnforceObjectTemplate(tgtName: String, template: ObjectTemplate): Expression {
        val construction = findOrEnforceConstructObjectTemplate(tgtName, template)
        val setProperties = findOrEnforcePropertiesObjectTemplate(template)
        return if (setProperties.isEmpty()) {
            construction
        } else {
            val objAssignment = VariableAssignmentStatementDefault(VariableDefinitionDefault(tgtName, null), null, construction)
            val propAssignments = setProperties.map { (k, v) -> VariableAssignmentStatementDefault(VariableDefinitionDefault(k, null), null, v) }
            val withBlock = StatementBlockExpressionDefault(propAssignments, RootExpressionDefault.SELF)
            val propAssignmentsWithObj = WithExpressionDefault(RootExpressionDefault(tgtName), withBlock)
            val assignments = listOf(objAssignment)
            val block = StatementBlockExpressionDefault(assignments, propAssignmentsWithObj)
            block
        }
    }

    private fun findOrEnforceConstructObjectTemplate(tgtName: String, template: ObjectTemplate): Expression {
        val args = setConstructorArgs(template)
        return findOrConstructExpression(tgtName, template.type.qualifiedTypeName, args)
    }

    private fun enforceObjectTemplate(template: ObjectTemplate): Expression {
        val args = setConstructorArgs(template)
        return constructObjectExpression(template.type.qualifiedTypeName, args)
    }

    private fun findOrEnforceConstructorArgs(template: ObjectTemplate): Map<String, Expression> {
        val decl = template.type.resolvedDefinition
        val constructors = when (decl) {
            is DataType -> decl.constructors
            is ValueType -> decl.constructors
            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
        }
        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
        val constructorArgExpressions = template.propertyTemplate.entries.mapNotNull { (k, v) ->
            if (possibleConArgNames.contains(k.value)) {
                val expr = findOrEnforceRhsExpr(k.value, v.rhs)
                Pair(k.value, expr)
            } else {
                null
            }
        }.associate { it }
        return constructorArgExpressions
    }

    private fun setConstructorArgs(template: ObjectTemplate): Map<String, Expression> {
        val decl = template.type.resolvedDefinition
        val constructors = when (decl) {
            is DataType -> decl.constructors
            is ValueType -> decl.constructors
            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
        }
        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
        val constructorArgExpressions = template.propertyTemplate.entries.mapNotNull { (k, v) ->
            if (possibleConArgNames.contains(k.value)) {
                val expr = enforceRhsExpr(v.rhs)
                Pair(k.value, expr)
            } else {
                null
            }
        }.associate { it }
        return constructorArgExpressions
    }

    private fun findOrEnforcePropertiesObjectTemplate(template: ObjectTemplate): Map<String, Expression> {
        val decl = template.type.resolvedDefinition
        val constructors = when (decl) {
            is DataType -> decl.constructors
            is ValueType -> decl.constructors
            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
        }
        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
        val setPropExpressions = template.propertyTemplate.entries.mapNotNull { (k, v) ->
            if (possibleConArgNames.contains(k.value)) {
                null
            } else {
                val templateVarName = v.rhs.identifier?.value ?: createTempVariable()
                val expr = findOrEnforceRhsExpr(templateVarName, v.rhs)
                Pair(k.value, expr)
            }
        }.associate { it }
        return setPropExpressions
    }

    /*
     when {
       $nothing == <id> -> {
           <set-args>
           construct <Type>(args)
       }
       else -> <id>
     }
     */
    private fun findOrConstructExpression(tgtName: String, tgtType: PossiblyQualifiedName, constructArgs: Map<String, Expression>): Expression {
        val ifIsNothing = InfixExpressionDefault(listOf(RootExpressionDefault.NOTHING, RootExpressionDefault(tgtName)), listOf("=="))
        val constructExpr = constructObjectExpression(tgtType, constructArgs)
//        val constructOption = WhenOptionDefault(ifIsNothing, constructExpr)
//        val findOption = WhenOptionElseDefault(RootExpressionDefault(tgtName))
//        val choice = WhenExpressionDefault(listOf(constructOption), findOption)
//        return choice

        val cond = InfixExpressionDefault(listOf(RootExpressionDefault.NOTHING, RootExpressionDefault(tgtName)), listOf("=="))
        val tc = TernaryConditionExpressionDefault(cond, constructExpr, RootExpressionDefault(tgtName))
        return tc
    }

    /*
       <tgt> := {
         <set-args>
         construct <Type>(args)
        }
     */
    private fun constructObjectExpression(tgtType: PossiblyQualifiedName, constructorArgExpressions: Map<String, Expression>): Expression {
        val conArgs = constructorArgExpressions.map { (k, rhs) ->
            val varDef = VariableDefinitionDefault(k, null)
            VariableAssignmentStatementDefault(varDef, null, rhs)
        }
        val constructExpr = CreateObjectExpressionDefault(tgtType, conArgs)
        return constructExpr
    }

    /*
     with(<lhsExpr>) <findOrSetExpression(...)>
     */
//    private fun findOrSetExpressionWith(withObject: Expression, propName: String, rhs: Expression): Expression {
//        val expr = findOrSetExpression(propName, rhs)
//        val withExpr = WithExpressionDefault(withObject, expr)
//        return withExpr
//    }

    /*
      <propName> == <var>: <expression>

      In enforce mode, the existing value if the property is ignored.

      $nothing == <var> ? bind(<var>,<expression>) : assert(<var>,<expression>)
     */
    private fun setToExpressionOrVar(varName: String, rhs: Expression): Expression {
        val cond = InfixExpressionDefault(listOf(RootExpressionDefault.NOTHING, RootExpressionDefault(varName)), listOf("=="))
        val trueExpr = FunctionCallDefault("bind".asPossiblyQualifiedName, listOf(RootExpressionDefault(varName), rhs))
        val falseExpr = FunctionCallDefault("assert".asPossiblyQualifiedName, listOf(RootExpressionDefault(varName), rhs))
        val tc = TernaryConditionExpressionDefault(cond, trueExpr, falseExpr)
        return tc
    }

}