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
    initialKnownVariableNames: Set<String>,
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

        private fun expressionFreeVariableNames(expression: Expression): List<String> {
            return when (expression) {
                is RootExpression -> listOf(expression.name)
                // Add recursive variable extraction for other expression types if needed
                else -> emptyList() //TODO
            }
        }
    }

    internal val _executions = initialExes.mapIndexed { index, execution -> execution.also { it.index = index } }.toMutableList()
    internal val _knownVariables = initialKnownVariableNames.toMutableSet()
    // Keep your initial properties, then add this compilation-state tracker:
    internal val _locallyBoundVars = initialKnownVariableNames.toMutableSet()
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
        build(tgtName, template, lhsType)
        execute(evc, tgtName)
    }

    /**
     * Resets compiler state, runs the pre-scan to locate all harvestable variables,
     * and compiles the AST into executable steps.
     */
    fun build(tgtName: String, template: PropertyTemplateRhs, tgtType: TypeInstance) {
        harvestVariables(_knownVariables.contains(tgtName), template)
        // 3. Begin main traversal
        traversePropertyTemplateRhs(false, null, StdLibDefault.NothingType, tgtName, tgtType, template)
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

    fun harvestVariables(parentIsKnown: Boolean, template: PropertyTemplateRhs) {
        when (template) {
            is ObjectTemplate -> {
                val objVar = template.identifier?.value
                val isKnown = parentIsKnown || (objVar != null && _knownVariables.contains(objVar))
                if (isKnown && objVar != null) {
                    _knownVariables.add(objVar)
                }
                template.propertyTemplate.values.forEach { pt ->
                    harvestVariables(isKnown, pt.rhs)
                }
            }
            is PropertyTemplateExpression -> {
                // Unify variable targeting: look at both the identifier and the RHS expression
                val templateVarName = template.identifier?.value
                val isRootExpr = template.expression is RootExpression
                val rootExprName = (template.expression as? RootExpression)?.name
                val targetVarName = templateVarName ?: rootExprName

                if (parentIsKnown && targetVarName != null) {
                    _knownVariables.add(targetVarName)
                }
            }
            is CollectionTemplate -> {
                val colVar = template.identifier?.value
                val isKnown = parentIsKnown || (colVar != null && _knownVariables.contains(colVar))
                if (isKnown && colVar != null) {
                    _knownVariables.add(colVar)
                }
                template.elements.forEach { et ->
                    harvestVariables(isKnown, et)
                }
            }
        }
    }

    /**
     * traverse the template.
     * create Execution steps
     * set inputs and outputs based on known variables and their properties
     */
    fun traversePropertyTemplateRhs(setLhs: Boolean, parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, template: PropertyTemplateRhs) {
        when (template) {
            is PropertyTemplateExpression -> traversePropertyTemplateExpression(setLhs, parentName, parentType, lhsName, lhsType, template)
            // Now passing setLhs to both object and collection templates!
            is ObjectTemplate -> traverseObjectTemplate(setLhs, parentName, parentType, lhsName, lhsType, template)
            is CollectionTemplate -> traverseCollectionTemplate(setLhs, parentName, parentType, lhsName, lhsType, template)
            else -> error("Unknown rhs type ${template::class}")
        }
    }

    /*
     <lhs> == <var>? : <expression>

     if this is called from a collection element template, then the parentName is null
     */
    fun traversePropertyTemplateExpression(setLhs: Boolean, parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, template: PropertyTemplateExpression) {
        val lhsFullName = parentName?.let { "$parentName$$lhsName" } ?: lhsName

        val templateVarName = template.identifier?.value
        val isRootExpr = template.expression is RootExpression
        val rootExprName = (template.expression as? RootExpression)?.name
        val targetVarName = templateVarName ?: rootExprName
        // Root-only references ("p") consume an existing variable; they do not define it unless harvested.
        val bindTargetInEnforce = !(templateVarName == null && isRootExpr)

        var canBeHarvested = false

        if (targetVarName != null) {
            val isAlreadyBound = _locallyBoundVars.contains(targetVarName)

            // Determine if the RHS expression can be fully evaluated
            val expressionFreeVars = expressionFreeVariableNames(template.expression)
            val canEvaluateExpression = expressionFreeVars.all { _locallyBoundVars.contains(it) }

            // CRITICAL UNIFICATION RULE:
            // We can only harvest from the model if we CANNOT evaluate the expression!
            canBeHarvested = _knownVariables.contains(targetVarName) &&
                    !isAlreadyBound &&
                    !canEvaluateExpression &&
                    parentName != null &&
                    _locallyBoundVars.contains(parentName)

            val inputs = mutableListOf<String>()
            val outputs = mutableListOf(lhsFullName)

            val expressionVarName = if (!isRootExpr) {
                val vn = "$lhsFullName\$rhs"
                val freeVars = expressionFreeVars
                createStep("$vn := ${template.expression.asString()}", freeVars, listOf(vn)) { evc ->
                    val value = ExpressionsInterpreterOverTypedObject(accessorMutator).evaluateExpression(evc, template.expression)
                    evc.setNamedValue(vn, value)
                }
                inputs.add(vn)
                vn
            } else {
                rootExprName!!
            }

            if (canBeHarvested) {
                // Harvesting Path: Model is the source of truth
                outputs.add(targetVarName)
                _locallyBoundVars.add(targetVarName)

                // If there is a root expression variable (like p in q:p) that is not bound,
                // it also gets bound to this harvested value
                if (rootExprName != null && !_locallyBoundVars.contains(rootExprName)) {
                    outputs.add(rootExprName)
                    _locallyBoundVars.add(rootExprName)
                }

                inputs.add(parentName!!)
            } else {
                // Enforcing Path: Expression/variable is the source of truth
                if (!isAlreadyBound) {
                    if (bindTargetInEnforce) {
                        outputs.add(targetVarName)
                        _locallyBoundVars.add(targetVarName)
                    } else {
                        inputs.add(targetVarName)
                    }
                } else {
                    inputs.add(targetVarName)
                }

                expressionFreeVars.forEach { fv ->
                    if (!inputs.contains(fv)) {
                        inputs.add(fv)
                    }
                }
            }

            // Dynamically adjust step description based on the actual evaluation direction
            val description = if (canBeHarvested) {
                if (rootExprName != null && rootExprName != targetVarName) {
                    "$lhsFullName := $targetVarName := $rootExprName := $parentName.$lhsName"
                } else {
                    "$lhsFullName := $targetVarName := $parentName.$lhsName"
                }
            } else {
                if (targetVarName != expressionVarName) {
                    "$lhsFullName := $targetVarName := $expressionVarName"
                } else {
                    "$lhsFullName := $targetVarName"
                }
            }

            createStep(description, inputs, outputs) { evc ->
                if (canBeHarvested) {
                    val parent = evc.getOrInParent(parentName!!) ?: error("$parentName not found")
                    val modelValue = parent.getProperty(lhsName)
                    evc.setNamedValue(targetVarName, modelValue)
                    if (rootExprName != null) {
                        evc.setNamedValue(rootExprName, modelValue)
                    }
                    evc.setNamedValue(lhsFullName, modelValue)
                } else {
                    // FIX: Look up the value from the expression variable ("b"), NEVER the unbound target variable ("r")!
                    val sourceValue = evc.getOrInParent(expressionVarName) ?: error("$expressionVarName not found")

                    if (bindTargetInEnforce) {
                        val boundVal = evc.getOrInParent(targetVarName)
                        if (boundVal == null) {
                            evc.setNamedValue(targetVarName, sourceValue)
                        } else {
                            check(boundVal == sourceValue) { "Paradox! Variable $targetVarName ($boundVal) conflicts with expression ($sourceValue)" }
                        }
                    }

                    evc.setNamedValue(lhsFullName, sourceValue)
                }
            }
        } else {
            val expressionVarName = "$lhsFullName\$rhs"
            val freeVars = expressionFreeVariableNames(template.expression)
            createStep("$expressionVarName := ${template.expression.asString()}", freeVars, listOf(expressionVarName)) { evc ->
                val value = ExpressionsInterpreterOverTypedObject(accessorMutator).evaluateExpression(evc, template.expression)
                evc.setNamedValue(expressionVarName, value)
            }

            createStep("$lhsFullName := $expressionVarName", listOf(expressionVarName), listOf(lhsFullName)) { evc ->
                val exprValue = evc.getOrInParent(expressionVarName) ?: error("$expressionVarName not found")
                evc.setNamedValue(lhsFullName, exprValue)
            }
        }

        // CRITICAL: Skip writing back (setLhs) if we harvested the property from the model
        val shouldSetLhs = setLhs && !canBeHarvested
        if (shouldSetLhs && parentName != null) {
            createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
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
    fun traverseObjectTemplate(setLhs: Boolean, parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, template: ObjectTemplate) {
        val lhsFullName = parentName?.let { "$parentName$$lhsName" } ?: lhsName
        val templateVarName = template.identifier?.value

        // 1. Use the simple variable identity name
        val objVarName = templateVarName ?: "$lhsFullName\$obj"
        val isAlreadyBound = _locallyBoundVars.contains(objVarName)

        val conArgTemplates = template.constructorArgumentTemplates
        val conArgNames = conArgTemplates.map { it.propertyName.value }.map { "$objVarName$$it" }
        val hasBoundParent = parentName != null && _locallyBoundVars.contains(parentName)
        val requireParentInput = hasBoundParent && (parentType.isCollection.not() || isAlreadyBound)

        val stepInputs = mutableListOf<String>().apply {
            if (requireParentInput) add(parentName!!)
            if (isAlreadyBound) {
                add(objVarName) // If bound, only depend on its identity resolution
            } else {
                addAll(conArgNames) // CRITICAL: Only require conArgs if we must physically construct it!
            }
        }

        val stepOutputs = mutableListOf(lhsFullName).apply {
            if (!isAlreadyBound) add(objVarName)
        }

        // 2. Recursively resolve constructor arguments (always setLhs = false)
        // Note: we pass objVarName as the parent name!
        traverseObjectTemplateProperties(false, objVarName, lhsType, conArgTemplates)

        // 3. Emit the Object Resolution Step
        val description = when {
            null==templateVarName -> when {
                isAlreadyBound -> "$lhsFullName := $objVarName"
                else -> "$lhsFullName := ${template.type.typeName.value}(${conArgNames.joinToString()}){}"
            }
            else -> when {
                isAlreadyBound -> "$lhsFullName := $templateVarName"
                else -> "$lhsFullName := $templateVarName := ${template.type.typeName.value}(${conArgNames.joinToString()}){}"
            }
        }
        createStep(description, stepInputs, stepOutputs) { evc ->
            val boundVar = evc.getOrInParent(objVarName)
            val resolvedObj = when {
                boundVar != null -> boundVar

                parentName != null -> {
                    val parentObj = evc.getOrInParent(parentName)
                    if (parentObj == null && parentType.isCollection) {
                        val args = conArgTemplates.associate {
                            val argValName = "$objVarName$${it.propertyName.value}"
                            it.propertyName.value to (evc.getOrInParent(argValName) ?: error("$argValName not found"))
                        }
                        val fresh = accessorMutator.createStructureValue(template.type.qualifiedTypeName, args)
                        evc.setNamedValue(objVarName, fresh)
                        fresh
                    } else if (parentObj == null) {
                        error("$parentName not found")
                    } else if (parentType.isCollection) {
                        var matchedElement: TypedObject? = null
                        accessorMutator.forEachIndexed(parentObj) { _, elValue ->
                            if (matchedElement == null && elValue.type == template.type) {
                                matchedElement = elValue
                            }
                        }

                        if (matchedElement != null) {
                            evc.setNamedValue(objVarName, matchedElement)
                            matchedElement!!
                        } else {
                            val args = conArgTemplates.associate {
                                val argValName = "$objVarName$${it.propertyName.value}"
                                it.propertyName.value to (evc.getOrInParent(argValName) ?: error("$argValName not found"))
                            }
                            val fresh = accessorMutator.createStructureValue(template.type.qualifiedTypeName, args)
                            evc.setNamedValue(objVarName, fresh)
                            fresh
                        }
                    } else {
                        val existing = parentObj.getProperty(lhsName)
                        if (!accessorMutator.isNothing(existing)) {
                            evc.setNamedValue(objVarName, existing)
                            existing
                        } else {
                            val args = conArgTemplates.associate {
                                val argValName = "$objVarName$${it.propertyName.value}"
                                it.propertyName.value to (evc.getOrInParent(argValName) ?: error("$argValName not found"))
                            }
                            val fresh = accessorMutator.createStructureValue(template.type.qualifiedTypeName, args)
                            evc.setNamedValue(objVarName, fresh)
                            fresh
                        }
                    }
                }

                else -> {
                    val args = conArgTemplates.associate {
                        val argValName = "$objVarName$${it.propertyName.value}"
                        it.propertyName.value to (evc.getOrInParent(argValName) ?: error("$argValName not found"))
                    }
                    val fresh = accessorMutator.createStructureValue(template.type.qualifiedTypeName, args)
                    evc.setNamedValue(objVarName, fresh)
                    fresh
                }
            }

            evc.setNamedValue(lhsFullName, resolvedObj)
        }

        if (!isAlreadyBound) {
            _locallyBoundVars.add(objVarName)
        }

        if (setLhs) {
            createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
        }

        // 4. Recursively resolve non-constructor properties
        traverseObjectTemplateProperties(true, objVarName, lhsType, template.propertyOnlyTemplates)
    }

    fun traverseObjectTemplateProperties(setLhs: Boolean, parentVarName: String, parentType: TypeInstance, propertyTemplates: Collection<PropertyTemplate>) {
        for (pt in propertyTemplates) {
            val propLhsName = pt.propertyName.value
            val propType = parentType.resolvedDefinition.findAllPropertyOrNull(PropertyName(propLhsName))?.typeInstance ?: StdLibDefault.AnyType
            // Pass the parent's actual variable name down as the parentName parameter
            traversePropertyTemplateRhs(setLhs, parentVarName, parentType, propLhsName, propType, pt.rhs)
        }
    }

    fun traverseCollectionTemplate(setLhs: Boolean, parentName: String?, parentType: TypeInstance, lhsName: String, lhsType: TypeInstance, template: CollectionTemplate) {
        val lhsFullName = parentName?.let { "$parentName$$lhsName" } ?: lhsName
        val templateVarName = template.identifier?.value
        // Named collections bind their value to the lhs location; unnamed ones use a synthetic backing var.
        val colVarName = templateVarName?.let { lhsFullName } ?: "$lhsFullName\$col"

        val isColAlreadyBound = _locallyBoundVars.contains(colVarName) || (templateVarName != null && _locallyBoundVars.contains(templateVarName))

        // 1. Generate purely local LHS name segments to pass to children (e.g., "el0")
        val elementLhsNames = template.elements.mapIndexed { i, et ->
            val explicitVarName = (et as? ObjectTemplate)?.identifier?.value
            explicitVarName ?: "el$i"
        }

        // 2. Pre-calculate the exact single-nested full names the children will use to save their values
        val elementFullNames = elementLhsNames.map { "$colVarName$$it" }

        // 3. Compile the elements by passing down the local segment name
        val elType = lhsType.typeArguments.firstOrNull()?.type ?: StdLibDefault.AnyType
        template.elements.forEachIndexed { i, et ->
            val elLhsName = elementLhsNames[i]
            traversePropertyTemplateRhs(false, colVarName, lhsType, elLhsName, elType, et)
        }

        // 4. Map the inputs using the pre-calculated full names so the sorter behaves
        val stepInputs = mutableListOf<String>().apply {
            addAll(elementFullNames) // Wait for the children to write to their full names
            if (template.isSubset && parentName != null) add(parentName)
            if (isColAlreadyBound) add(colVarName)
        }

        val stepOutputs = mutableListOf(lhsFullName).apply {
            if (!isColAlreadyBound) add(colVarName)
            if (!isColAlreadyBound && templateVarName != null && templateVarName != colVarName) add(templateVarName)
        }

        if (!isColAlreadyBound) {
            _locallyBoundVars.add(colVarName)
            if (templateVarName != null) _locallyBoundVars.add(templateVarName)
        }

        val listExpr = "List(${elementFullNames.joinToString(", ")})"
        val collectionStepDescription = when {
            template.isSubset -> "Synchronize Collection $lhsFullName"
            templateVarName != null -> "$lhsFullName := $templateVarName := $listExpr"
            else -> "$lhsFullName := $listExpr"
        }

        // 5. Emit the collection step using the matching elementFullNames keys.
        createStep(collectionStepDescription, stepInputs, stepOutputs) { evc ->
            val resolvedElements = elementFullNames.map { evc.getOrInParent(it) ?: error("$it not found") }
            val newElementColl = accessorMutator.createCollection(lhsType, resolvedElements)

            val finalCol = if (template.isSubset) {
                val parent = parentName?.let { evc.getOrInParent(it) }
                val existingCol = when {
                    templateVarName != null -> evc.getOrInParent(templateVarName) ?: parent?.getProperty(lhsName)
                    else -> parent?.getProperty(lhsName)
                }
                val col = if (existingCol != null && !accessorMutator.isNothing(existingCol)) {
                    existingCol
                } else {
                    accessorMutator.createCollection(lhsType, emptyList())
                }
                accessorMutator.collectionUnion(col, newElementColl)
            } else {
                newElementColl
            }

            evc.setNamedValue(lhsFullName, finalCol)
            evc.setNamedValue(colVarName, finalCol)
            if (templateVarName != null) {
                evc.setNamedValue(templateVarName, finalCol)
            }
        }

        if (setLhs) {
            createSetLhsStep(parentName, parentType, lhsName, lhsFullName)
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
                        traversePropertyTemplateRhs(false, null, lhsType, elLhsName, elType, et)
                    }
                }

                lhs.type.isCollection -> {
                    accessorMutator.forEachIndexed(lhs) { elIndex, elValue ->
                        template.elements.forEachIndexed { etIndex, et ->
                            // TODO: if elValue matches el template ?
                            val elLhsName = "${elParentName}\$el$elIndex"
                            traversePropertyTemplateRhs(false, null, lhsType, elLhsName, elType, et)
                        }
                    }
                }

                else -> error("lhs is not a collection")
            }

            else -> error("lhsType is not a collection")
        }
    }

}
