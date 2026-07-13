package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.kotlinx.collections.transitveClosure
import net.akehurst.language.agl.m2mTransform.processor.interpreter.ObjectTemplateObjectTemplateExt.constructorArgumentTemplates
import net.akehurst.language.agl.m2mTransform.processor.interpreter.ObjectTemplateObjectTemplateExt.propertyOnlyTemplates
import net.akehurst.language.asm.simple.AnyExt.asString
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.LiteralExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.FunctionCallDefault
import net.akehurst.language.expressions.asm.LiteralExpressionDefault
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.PropertyCallDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.asm.StatementBlockExpressionDefault
import net.akehurst.language.expressions.asm.VariableAssignmentStatementDefault
import net.akehurst.language.expressions.asm.VariableDefinitionDefault
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

class ExecutionStep(
    val description: String,
    val inputs: List<String>,
    val outputs: List<String>,
    val execution: (evc: EvaluationContext) -> Unit
) {
    var index = -1 //unset initially

    override fun toString(): String = description
}

object ObjectTemplateObjectTemplateExt {
    val ObjectTemplate.constructorArgumentNames: List<String>
        get() {
            val typeDef = type.resolvedDefinition
            val constructors = when (typeDef) {
                is DataType -> typeDef.constructors
                is ValueType -> typeDef.constructors
                else -> error("Type '${typeDef.qualifiedName.value}' has no constructors")
            }
            val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
            return possibleConArgNames
        }

    val ObjectTemplate.constructorArgumentTemplates: List<PropertyTemplate>
        get() =
            propertyTemplate.values.filter { constructorArgumentNames.contains(it.propertyName.value) }


    val ObjectTemplate.propertyOnlyTemplates: List<PropertyTemplate>
        get() =
            propertyTemplate.values.filterNot { constructorArgumentNames.contains(it.propertyName.value) }
}

class M2mPatternExecutor2(
    val issues: IssueHolder,
    val accessorMutator: ObjectGraphAccessorMutator,
    initialExes: List<ExecutionStep>
) {

    companion object {
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

    }

    internal var _nextTempVarNum = 0
    internal val _executions = initialExes.mapIndexed { index, execution -> execution.also { it.index = index } }.toMutableList()
    val executionPlan get() = _executions.topologicalSort(::compareExecutions)

    fun addExecution(value: ExecutionStep) {
        value.index = _executions.size
        _executions.add(value)
    }

    fun createStep(description: String, inputs: List<String>, outputs: List<String>, execution: (evc: EvaluationContext) -> Unit) {
        val step = ExecutionStep(description, inputs, outputs, execution)
        addExecution(step)
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
        traversePropertyTemplateRhs(false,null, StdLibDefault.NothingType, tgtName, tgtType, lhs, initialVariables, template)
    }

    fun execute(evc: EvaluationContext, tgtName: String): TypedObject {
        val sorted = executionPlan
        val evcExecution = evc.child()
        for (count in 0 until sorted.size) {
            val pe = sorted[count]
            pe.execution.invoke(evcExecution)
        }
        val value = evcExecution.getOrInParent(tgtName) ?: accessorMutator.nothing()
        return value
    }

    private fun createTempVariable() = "temp${_nextTempVarNum++}"

    /**
     * returns a simplified template where all property assignments are from variables
     * any non-identified template is given an artificial id.
     * Because we already found out all the inputs based on the initial variables and lhs,
     * we know that all other variables must be outputs, and constructed objects are inputs
     * to properties and nested templates
     */
    private fun constructExecutions(
        tgtName: String,
        preComputedInputs: Map<PropertyTemplateRhs, List<String>>,
        template: PropertyTemplateRhs,
        lhsType: TypeInstance
    ) {
//       for(step in _executions) {
//           constructExpression(step)
//       }
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
     * traverse the template.
     * create Execution steps
     * set inputs and outputs based on known variables and their properties
     */
    fun traversePropertyTemplateRhs(setLhs:Boolean, parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, lhs: TypedObject, variables: EvaluationContext, template: PropertyTemplateRhs) {
        when (template) {
            is PropertyTemplateExpression -> traversePropertyTemplateExpression(setLhs, parentName, parentType, lhsName, lhsType, lhs, variables, template)
            is ObjectTemplate -> traverseObjectTemplate(parentName, parentType, lhsName, lhsType, lhs, variables, template)
            is CollectionTemplate -> traverseCollectionTemplate(parentName, parentType, lhsName, lhsType, lhs, variables, template)
            else -> error("Unknown rhs type ${template::class}")
        }
    }

    /*
     <lhs> == <var>? : <expression>

     if this is called from a collection element template, then the parentName is null
     */
    fun traversePropertyTemplateExpression(setLhs:Boolean, parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, lhs: TypedObject, initialKnownVariables: EvaluationContext, template: PropertyTemplateExpression) {
        //TODO: check lhs against its type, maybe ?
        // in this case, because we are enforcing, the lhs object is irrelevant!
        val lhsFullName = parentName?.let { "$parentName$$lhsName" } ?: lhsName
        val templateVarName = template.identifier?.value
        val expressionVarName = when (template.expression) {
            is RootExpression -> (template.expression as RootExpression).name
            is LiteralExpression -> {
                val vn = "$lhsFullName\$rhs"
                createStep("$vn := ${template.expression.asString()}", emptyList(), listOf(vn)) { evc ->
                    val value = ExpressionsInterpreterOverTypedObject(accessorMutator).evaluateExpression(evc, template.expression) //TODO: reuse interpreter
                    evc.setNamedValue(vn, value)
                }
                vn
            }
            else -> {
                val vn = "$lhsFullName\$rhs"
                val freeVars = template.expression.freeVariableNames
                createStep("$vn := ${template.expression.asString()}", freeVars, listOf(vn)) { evc ->
                    val value = ExpressionsInterpreterOverTypedObject(accessorMutator).evaluateExpression(evc, template.expression) //TODO: reuse interpreter
                    evc.setNamedValue(vn, value)
                }
                vn
            }
        }
        when {
            null == templateVarName -> when {
                accessorMutator.isNothing(lhs) -> {
                    // expression var must be known
                    // expressionVarName is input
                    createStep("$lhsFullName := $expressionVarName", listOf(expressionVarName), listOf(lhsFullName)) { evc ->
                        val exprValue = evc.getOrInParent(expressionVarName) ?: error("$expressionVarName not found")
                        evc.setNamedValue(lhsFullName, exprValue)
                    }
                    if(setLhs) createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                }

                null == initialKnownVariables.getOrInParent(expressionVarName) -> {
                    // expression var is not known - match it from lhs
                    // expressionVarName is output - from lhs
                    check(null != parentName) { "$parentName must not be null here" }
                    createStep("$lhsFullName := $expressionVarName := ${parentName}.$lhsName", parentName?.let { listOf(it) } ?: emptyList(), listOf(lhsFullName, expressionVarName)) { evc ->
                        val parent = evc.getOrInParent(parentName) ?: error("$parentName not found")
                        val lhs = parent.getProperty(lhsName)
                        evc.setNamedValue(expressionVarName, lhs)
                        evc.setNamedValue(lhsFullName, lhs)
                    }
                    // lhs is read, no need to set it
                }

                else -> {
                    // expression var is known - enforce it
                    // expressionVarName is input
                    createStep("$lhsFullName := $$expressionVarName", listOf(expressionVarName), listOf(lhsFullName)) { evc ->
                        val exprValue = evc.getOrInParent(expressionVarName) ?: error("Expression $expressionVarName not found")
                        evc.setNamedValue(lhsFullName, exprValue)
                    }
                    if(setLhs) createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                }
            }

            else -> when {
                null == initialKnownVariables.getOrInParent(templateVarName) -> when {
                    null == initialKnownVariables.getOrInParent(expressionVarName) -> {
                        // expression var is not known
                        // expressionVarName is output - from lhs
                        //check(null != parentName) { "$parentName must not be null here" }
                        val lhs = parentName?.let { "$parentName$$lhsName" } ?: lhsName
                        createStep("$lhsFullName := $templateVarName := $expressionVarName := $lhs", emptyList(), listOf(lhsFullName, templateVarName, expressionVarName)) { evc ->
                            //val parent = evc.getOrInParent(parentName) ?: error("$parentName not found")
                            val lhs = parentName?.let {
                                val parent = evc.getOrInParent(parentName) ?: error("$parentName not found")
                                parent.getProperty(lhs)
                            } ?: evc.getOrInParent(lhs) ?: error("Both $parentName && $lhs not found")
                            evc.setNamedValue(expressionVarName, lhs)
                            evc.setNamedValue(templateVarName, lhs)
                            evc.setNamedValue(lhsFullName, lhs)
                        }
                        // lhs is read, no need to set it
                    }

                    else -> {
                        // expression var is known
                        // expressionVarName is input
                        createStep("$lhsFullName := $templateVarName := $expressionVarName", listOf(expressionVarName), listOf(lhsFullName, templateVarName)) { evc ->
                            val exprValue = evc.getOrInParent(expressionVarName) ?: error("$expressionVarName not found")
                            evc.setNamedValue(templateVarName, exprValue)
                            evc.setNamedValue(lhsFullName, exprValue)
                        }
                        if(setLhs) createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                    }
                }

                else -> {
                    // var is an input - ignore or assert expression
                    createStep("$lhsName := $templateVarName", listOf(templateVarName), listOf(lhsName)) { evc ->
                        val value = evc.getOrInParent(templateVarName) ?: error("$templateVarName not found")
                        evc.setNamedValue(lhsFullName, value)
                    }
                    if(setLhs) createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                }
            }
        }
    }

    fun createSetLhsStep(parentName: String?, parentType: TypeInstance, lhsName: String, lhsFullName: String) {
        if (null != parentName && parentType.isCollection.not()) {
            // set lhs value in parent
            createStep("$parentName.$lhsName := $lhsFullName", listOf(parentName, lhsFullName), emptyList()) { evc ->
                val value = evc.getOrInParent(lhsFullName) ?: error("$lhsFullName not found")
                val parent = evc.getOrInParent(parentName) ?: error("$parentName not found")
                parent.setProperty(lhsName, value)
            }
        }
    }

    /*
      <lhs> == <var>? : Type { <propertyTemplateList> }
     */
    fun traverseObjectTemplate(parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, lhs: TypedObject, initialKnownVariables: EvaluationContext, template: ObjectTemplate) {
        val lhsFullName = parentName?.let { "$parentName$$lhsName" } ?: lhsName
        val templateVarName = template.identifier?.value
        when {
            null == templateVarName -> when {
                accessorMutator.isNothing(lhs) -> {
                    // no template variable & lhs is nothing
                    // must construct object from template
                    // inputs are constructor arguments
                    // output is the object as lhsName
                    val conArgTemplates = template.constructorArgumentTemplates
                    val conArgNames = conArgTemplates.map { it.propertyName.value }.map { "$lhsFullName$$it" }
                    traverseObjectTemplateProperties(false, parentName, lhsName, lhsType, accessorMutator.nothing(), initialKnownVariables, conArgTemplates)
                    createStep("$lhsFullName := ${template.type.typeName.value}(${conArgNames.joinToString()}){}", conArgNames, listOf(lhsFullName)) { evc ->
                        val args = conArgTemplates.associate {
                            val argValName = "$lhsName$${it.propertyName.value}"
                            it.propertyName.value to (evc.getOrInParent("$lhsName$$argValName") ?: error("$argValName not found"))
                        }
                        val value = accessorMutator.createStructureValue(template.type.qualifiedTypeName, args)
                        evc.setNamedValue(lhsFullName, value)
                    }
                    createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                    traverseObjectTemplateProperties(true, parentName, lhsName, lhsType, lhs, initialKnownVariables, template.propertyOnlyTemplates)
                }

                else -> {
                    // no template variable & lhs has a value
                    // use lhs as the object (if its type matches - if not then fail)
                    // input is no input ?
                    // output is lhsName
                    check(null != parentName) { "$parentName must not be null here" }
                    createStep("$lhsFullName := ${parentName}.$lhsName", emptyList(), listOf(lhsFullName)) { evc ->
                        val parent = evc.getOrInParent(parentName) ?: error("$parentName not found")
                        val lhs = parent.getProperty(lhsName)
                        evc.setNamedValue(lhsFullName, lhs)
                    }
                    // lhs is read, no need to set it
                    traverseObjectTemplateProperties(true, parentName, lhsName, lhsType, lhs, initialKnownVariables, template.propertyOnlyTemplates)
                }
            }

            else -> when {
                null == initialKnownVariables.getOrInParent(templateVarName) -> when {
                    accessorMutator.isNothing(lhs) -> {
                        // have template variable with no value & lhs is nothing
                        // must construct object from template
                        // inputs are constructor arguments
                        // output is the lhsName & templateVarName which takes the value of the object
                        val conArgTemplates = template.constructorArgumentTemplates
                        val conArgNames = conArgTemplates.map { it.propertyName.value }.map { "$lhsFullName$$it" }
                        traverseObjectTemplateProperties(false, parentName, lhsName, lhsType, accessorMutator.nothing(), initialKnownVariables, conArgTemplates)
                        createStep("$lhsFullName := $templateVarName := ${template.type.typeName.value}(${conArgNames.joinToString()}){}", conArgNames, listOf(lhsFullName, templateVarName)) { evc ->
                            val args = conArgTemplates.associate {
                                val argValName = "$lhsFullName$${it.propertyName.value}"
                                it.propertyName.value to (evc.getOrInParent(argValName) ?: error("$argValName not found"))
                            }
                            val value = accessorMutator.createStructureValue(template.type.qualifiedTypeName, args)
                            evc.setNamedValue(templateVarName, value)
                            evc.setNamedValue(lhsFullName, value)
                        }
                        createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                        traverseObjectTemplateProperties(true, parentName, lhsName, lhsType, lhs, initialKnownVariables, template.propertyOnlyTemplates)
                    }

                    else -> {
                        // have template variable with no value & lhs has a value
                        // use lhs as the object (if its type matches - if not then fail)
                        // inputs are lhs
                        // output is lhsName &  templateVarName which takes the value of the object
                        check(null != parentName) { "$parentName must not be null here" }
                        createStep("$lhsFullName := $templateVarName := ${parentName}.$lhsName", emptyList(), listOf(lhsFullName, templateVarName)) { evc ->
                            val parent = evc.getOrInParent(parentName) ?: error("$parentName not found")
                            val value = parent.getProperty(lhsName)
                            evc.setNamedValue(templateVarName, value)
                            evc.setNamedValue(lhsFullName, value)
                        }
                        // lhs is read, no need to set it
                        traverseObjectTemplateProperties(true, parentName, lhsName, lhsType, lhs, initialKnownVariables, template.propertyTemplate.values)
                    }
                }

                else -> {
                    // have template variable with a value ==> ignore lhs
                    // variable is the value - check type
                    // inputs template variable
                    // output is lhsName which takes the value of the object
                    createStep("$lhsFullName := $templateVarName", listOf(templateVarName), listOf(lhsFullName)) { evc ->
                        val value = evc.getOrInParent(templateVarName) ?: error("$templateVarName not found")
                        evc.setNamedValue(lhsFullName, value)
                    }
                    createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                    val knownVar = initialKnownVariables.getOrInParent(templateVarName) ?: error("Must be not null at this point!")
                    traverseObjectTemplateProperties(true, parentName, lhsName, lhsType, knownVar, initialKnownVariables, template.propertyTemplate.values)
                }
            }
        }
    }

    fun traverseObjectTemplateProperties(setLhs:Boolean, parentName: String?, lhsName: String, lhsType: TypeInstance, lhs: TypedObject, initialKnownVariables: EvaluationContext, propertyTemplates: Collection<PropertyTemplate>) {
        val propParentName = parentName?.let { "$parentName$$lhsName" } ?: lhsName
        for (pt in propertyTemplates) {
            val propLhs = lhs.getProperty(pt.propertyName.value)
            val propLhsName = pt.propertyName.value
            val propType = lhsType.resolvedDefinition.findAllPropertyOrNull(PropertyName(propLhsName))?.typeInstance ?: StdLibDefault.AnyType
            traversePropertyTemplateRhs(setLhs, propParentName, lhsType, propLhsName, propType, propLhs, initialKnownVariables, pt.rhs)
        }
    }

    fun traverseCollectionTemplate(parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, lhs: TypedObject, initialKnownVariables: EvaluationContext, template: CollectionTemplate) {
        val lhsFullName = parentName?.let { "$parentName$$lhsName" } ?: lhsName
        val templateVarName = template.identifier?.value
        val newElementNames = template.elements.mapIndexed { i, et -> "$lhsFullName\$el$i" }
        when {
            null == templateVarName -> when {
                accessorMutator.isNothing(lhs) -> {
                    // no template variable & lhs is nothing
                    // must create collection from template
                    // inputs are constructor arguments
                    // output is the object as lhsName
                    traverseCollectionTemplateElement(parentName, lhsName, lhsType, lhs, initialKnownVariables, template)
                    createStep("$lhsFullName := ${lhsType.typeName.value}(${newElementNames.joinToString()})", newElementNames, listOf(lhsFullName)) { evc ->
                        val elements = newElementNames.map { evc.getOrInParent(it) ?: error("$it not found") }
                        val col = accessorMutator.createCollection(lhsType, elements)
                        evc.setNamedValue(lhsFullName, col)
                    }
                    createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                }

                else -> {
                    // no template variable & lhs has value
                    // use lhs as collection
                    // match content in collection if possible, else add to collection
                    traverseCollectionTemplateElement(parentName, lhsName, lhsType, lhs, initialKnownVariables, template)
                    createStep("$lhsFullName := ${parentName}.$lhsName + ${newElementNames.joinToString(" + ")}", newElementNames, listOf(lhsFullName)) { evc ->
                        val elements = newElementNames.map { evc.getOrInParent(it) ?: error("$it not found") }
                        val col = accessorMutator.createCollection(lhsType, elements)
                        evc.setNamedValue(lhsFullName, col)
                    }
                    // lhs is read, no need to set it
                }
            }

            else -> when {
                null == initialKnownVariables.getOrInParent(templateVarName) -> when {
                    accessorMutator.isNothing(lhs) -> {
                        // have template variable with no value & lhs is nothing
                        // must create collection from template
                        // inputs are elements - or do we just add them later ?
                        // output is the lhsName & templateVarName which takes the value of the object
                        traverseCollectionTemplateElement(parentName, lhsName, lhsType, lhs, initialKnownVariables, template)
                        createStep("$lhsFullName := $templateVarName := ${lhsType.typeName.value}(${newElementNames.joinToString()})", newElementNames, listOf(lhsFullName)) { evc ->
                            val elements = newElementNames.map { evc.getOrInParent(it) ?: error("$it not found") }
                            val col = accessorMutator.createCollection(lhsType, elements)
                            evc.setNamedValue(templateVarName, col)
                            evc.setNamedValue(lhsFullName, col)
                        }
                        createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                    }

                    else -> {
                        // have template variable with no value & lhs has a value
                        // use lhs as the collection (if its type matches - if not then fail)
                        // inputs are lhs
                        // output is lhsName &  templateVarName which takes the value of the collection
                        // for each template element, match it or create if no match
                        check(parentName != null) { "$parentName must not be null!" }
                        traverseCollectionTemplateElement(parentName, lhsName, lhsType, lhs, initialKnownVariables, template)
                        createStep("$lhsFullName := $templateVarName := ${parentName}.$lhsName + ${newElementNames.joinToString(separator = " + ")}", newElementNames + templateVarName, listOf(lhsFullName)) { evc ->
                            val parent = evc.getOrInParent(parentName) ?: error("$parentName not found")
                            val col = parent.getProperty(lhsName)
                            val elements = newElementNames.map { evc.getOrInParent(it) ?: error("$it not found") }
                            val newElementColl = accessorMutator.createCollection(lhsType, elements)
                            val newCol = accessorMutator.collectionUnion(col, newElementColl)
                            evc.setNamedValue(templateVarName, newCol)
                            evc.setNamedValue(lhsFullName, newCol)
                        }
                        createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
                    }
                }

                else -> {
                    // have template variable with a value ==> ignore lhs
                    // variable is the value - check type
                    // inputs template variable
                    // output is lhsName which takes the value of the collection
                    createStep("$lhsFullName := $templateVarName + ${newElementNames.joinToString(separator = " + ")}", newElementNames + templateVarName, listOf(lhsFullName)) { evc ->
                        val elements = newElementNames.map { evc.getOrInParent(it) ?: error("$it not found") }
                        val newElementColl = accessorMutator.createCollection(lhsType, elements)
                        val templateVarValue = evc.getOrInParent(templateVarName) ?: error("$templateVarName not found")
                        val newCol = accessorMutator.collectionUnion(templateVarValue, newElementColl)
                        evc.setNamedValue(lhsFullName, newCol)
                    }
                }
            }
        }
    }

    fun traverseCollectionTemplateElement(parentName: String?, lhsName: String, lhsType: TypeInstance, lhs: TypedObject, initialKnownVariables: EvaluationContext, template: CollectionTemplate) {
        val elType = lhsType.typeArguments.firstOrNull()?.type ?: StdLibDefault.AnyType
        val elParentName = parentName?.let { "$parentName$$lhsName" } ?: lhsName
        when {
            lhsType.isCollection -> when {
                accessorMutator.isNothing(lhs) -> {
                    // must simply create elements from template
                    template.elements.forEachIndexed { etIndex, et ->
                        //val elLhsName = "el$etIndex"
                        val elLhsName = "${elParentName}\$el$etIndex"
                        traversePropertyTemplateRhs(false, null, lhsType, elLhsName, elType, accessorMutator.nothing(), initialKnownVariables, et)
                    }
                }
                lhs.type.isCollection -> {
                    accessorMutator.forEachIndexed(lhs) { elIndex, elValue ->
                        template.elements.forEachIndexed { etIndex, et ->
                            // TODO: if elValue matches el template ?
                            val elLhsName = "${elParentName}\$el$elIndex"
                            traversePropertyTemplateRhs(false, null, lhsType, elLhsName, elType, elValue, initialKnownVariables, et)
                        }
                    }
                }
                else -> error("lhs is not a collection")
            }

            else -> error("lhsType is not a collection")
        }
    }

}