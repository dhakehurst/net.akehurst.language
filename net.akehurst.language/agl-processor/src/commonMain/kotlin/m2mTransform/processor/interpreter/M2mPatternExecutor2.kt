package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.kotlinx.collections.transitveClosure
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.CreateObjectExpressionDefault
import net.akehurst.language.expressions.asm.InfixExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.asm.StatementBlockExpressionDefault
import net.akehurst.language.expressions.asm.VariableAssignmentStatementDefault
import net.akehurst.language.expressions.asm.VariableDefinitionDefault
import net.akehurst.language.expressions.asm.WhenExpressionDefault
import net.akehurst.language.expressions.asm.WhenOptionDefault
import net.akehurst.language.expressions.asm.WhenOptionElseDefault
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

class M2mPatternExecutor2(
    val issues: IssueHolder,
    val accessorMutator: ObjectGraphAccessorMutator,
    initialExes: List<M2mPatternExecution2>
) {

    companion object {
        const val RESULT = $$"$result"
    }

    internal var _nextTempVarNum = 0
    internal val _executions = initialExes.mapIndexed { index, execution -> execution.also { it.index = index } }.toMutableList()
    val executionPlan get() = _executions.topologicalSort(::compareExecutions)
    val executionExpression
        get() = executionPlan.joinToString(separator = "\n") {
            val expr = it.execution.invoke(EvaluationContext.of(emptyMap()))
            "// ${it.description}\n${expr.asString()}"
        }

    fun addExecution(value: M2mPatternExecution2) {
        value.index = _executions.size
        _executions.add(value)
    }

    fun buildAndExecute(tgtName: String, template: PropertyTemplateRhs, lhsType: TypeInstance, evc1: EvaluationContext) {
        build(tgtName, template, lhsType)
        execute(evc1, tgtName)
    }

    fun build(tgtName: String, template: PropertyTemplateRhs, lhsType: TypeInstance) {
        constructExecutions(tgtName, emptySet(), emptySet(), template, lhsType)
    }

    fun execute(evc1: EvaluationContext, tgtName: String): TypedObject {
        val eval = ExpressionsInterpreterOverTypedObject(accessorMutator)
        val sorted = executionPlan
        val evc = evc1.child()
        var value: TypedObject? = null
        for (count in 0 until sorted.size) {
            val pe = sorted[count]
            val expr = pe.execution.invoke(evc)
            value = eval.evaluateExpression(evc, expr)
        }
        //return evc.getOrInParent(tgtName) ?: accessorMutator.nothing()
        return value ?: accessorMutator.nothing()
    }

    private fun createTempVariable() = "temp${_nextTempVarNum++}"

    /**
     * returns a simplified template where all property assignments are from variables
     * any non-identified template is given an artificial id.
     */
    private fun constructExecutions(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution2>,
        doAfterMe: Set<M2mPatternExecution2>,
        template: PropertyTemplateRhs,
        lhsType: TypeInstance
    ): M2mPatternExecution2 {
        return when (template) {
            is PropertyTemplateExpression -> constructExecutionsFromPropertyTemplateExpression(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            is ObjectTemplate -> constructExecutionsFromObjectTemplate(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            is CollectionTemplate -> constructExecutionsFromCollectionTemplate(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            else -> error("Unknown rhs type ${template::class}")
        }
    }

    private fun constructExecutionsFromPropertyTemplateExpression(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution2>,
        doAfterMe: Set<M2mPatternExecution2>,
        template: PropertyTemplateExpression,
        lhsType: TypeInstance
    ): M2mPatternExecution2 {
        val templateVarName = template.identifier?.value
        val rhsName = when {
            template.expression is RootExpression -> (template.expression as RootExpression).name
            else -> null
        }
        val inputs = rhsName?.let { listOf(it) } ?: emptyList()
        val outputs = listOf(tgtName)
        val description = "check or set: $tgtName == $rhsName"
        val exe = M2mPatternExecution2(description, inputs, outputs) { evc ->
            findOrSetExpression(tgtName, template.expression)
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }
        addExecution(exe)
        return exe
    }

    /*

     */
    private fun constructExecutionsFromObjectTemplate(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution2>,
        doAfterMe: Set<M2mPatternExecution2>,
        template: ObjectTemplate,
        lhsType: TypeInstance
    ): M2mPatternExecution2 {
        val typeName = template.type.qualifiedTypeName
        val inputs = listOf<String>()
        val outputs = listOf(tgtName)
        val exe = M2mPatternExecution2("Find '$tgtName' or construct ${typeName.value}(...)", inputs, outputs) {
            findOrEnforceObjectTemplate(tgtName, template)
        }
        addExecution(exe)
        return exe
    }

    private fun constructExecutionsFromCollectionTemplate(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution2>,
        doAfterMe: Set<M2mPatternExecution2>,
        template: CollectionTemplate,
        lhsType: TypeInstance
    ): M2mPatternExecution2 {
        TODO()
    }

    /**
     * +1 if e1 comes after e2
     * -1 if e2 comes after e1
     * exe1 comes after exe2 when:
     *  exe1.input contains any of exe2.outputs
     *  exe2.doAfterMe contains exe1
     */
    private fun compareExecutions(exe1: M2mPatternExecution2, exe2: M2mPatternExecution2): Int {
        return when {
            exe2.doMeBefore.contains(exe1) -> 1 // do exe1 after exe2
            exe1.doMeBefore.contains(exe2) -> -1 // do exe2 after exe1
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
        return findOrSetExpression(tgtName, template.expression)
    }

    private fun findOrEnforceObjectTemplate(tgtName: String, template: ObjectTemplate): Expression {
        val construction = findOrEnforceConstructObjectTemplate(tgtName, template)
        val setProperties = checkOrEnforcePropertiesObjectTemplate(template)
        return if (setProperties.isEmpty()) {
            construction
        } else {
            val objAssignment = VariableAssignmentStatementDefault(VariableDefinitionDefault(tgtName, null), null, construction)
            val propAssignments = setProperties.map { (k, v) -> VariableAssignmentStatementDefault(VariableDefinitionDefault(k, null), null, v) }
            val withBlock = StatementBlockExpressionDefault(propAssignments, RootExpressionDefault(tgtName))
            val propAssignmentsWithObj = WithExpressionDefault(RootExpressionDefault(tgtName), withBlock)
            val assignments = listOf(objAssignment)
            val block = StatementBlockExpressionDefault(assignments, propAssignmentsWithObj)
            block
        }
    }

    private fun findOrEnforceConstructObjectTemplate(tgtName: String, template: ObjectTemplate): Expression {
        val args = findOrEnforceConstructorArgs(template)
        return findOrConstructExpression(tgtName, template.type.qualifiedTypeName, args)
    }

    private fun enforceObjectTemplate(template: ObjectTemplate): Expression {
        val args = findOrEnforceConstructorArgs(template)
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

    private fun checkOrEnforcePropertiesObjectTemplate(template: ObjectTemplate): Map<String, Expression> {
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
                val expr = findOrEnforceRhsExpr(k.value, v.rhs)
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
        val constructOption = WhenOptionDefault(ifIsNothing, constructExpr)
        val findOption = WhenOptionElseDefault(RootExpressionDefault(tgtName))
        val choice = WhenExpressionDefault(listOf(constructOption), findOption)
        return choice
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
    private fun findOrSetExpressionWith(withObject: Expression, propName: String, rhs: Expression): Expression {
        val expr = findOrSetExpression(propName, rhs)
        val withExpr = WithExpressionDefault(withObject, expr)
        return withExpr
    }

    /*
     when {
       $nothing == <propName> -> rhs
       else -> <propName>
     }
     */
    private fun findOrSetExpression(propName: String, rhs: Expression): Expression {
        val ifIsNothing = InfixExpressionDefault(listOf(RootExpressionDefault.NOTHING, RootExpressionDefault(propName)), listOf("=="))
        val setOption = WhenOptionDefault(ifIsNothing, rhs)
        val findOption = WhenOptionElseDefault(RootExpressionDefault(propName))
        val whenExpr = WhenExpressionDefault(listOf(setOption), findOption)
        return whenExpr
    }
}