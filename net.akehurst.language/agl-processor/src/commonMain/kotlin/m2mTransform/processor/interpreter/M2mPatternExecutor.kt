package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.kotlinx.collections.transitveClosure
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.RootExpression
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

class M2mPatternExecution<OT : Any>(
    val description: String,
    val inputs: List<String>,
    val outputs: List<String>,
    val execution: (evc: EvaluationContext<OT>) -> EvaluationContext<OT>
) {
    /** do 'self' execution before all of these */
    val doAfterMe = mutableSetOf<M2mPatternExecution<OT>>()

    val doMeBeforeAll get() = this.doAfterMe.transitveClosure { it.doAfterMe }

    fun mustComeBefore(other: M2mPatternExecution<OT>): Boolean = when {
        this.doMeBeforeAll.contains(other) -> true
        else -> false
    }

    override fun toString(): String = "${description} | ${inputs} -> ${outputs} ^ [${doAfterMe.joinToString { it.description }}]"
}

class M2mPatternExecutor<OT : Any>(
    val issues: IssueHolder,
    val accessorMutator: ObjectGraphAccessorMutator<OT>,
    initialExes: List<M2mPatternExecution<OT>>
) {

    companion object {
        const val RESULT = $$"$result"
    }

    val matchedVars = mutableMapOf<String,OT>()

    internal var _nextTempVarNum = 0
    internal val _executions = initialExes.toMutableList()
    //internal lateinit var _simplifiedTemplate: PropertyTemplateRhs

    fun executionPlan(): List<M2mPatternExecution<OT>> = _executions.topologicalSort(::compareExecutions)

    fun buildAndExecute(template: PropertyTemplateRhs, lhsType: TypeInstance, variables: Map<String, TypedObject<OT>>, src: TypedObject<OT>) {
        build(template, lhsType)
        execute(variables, src)
    }

    fun build(template: PropertyTemplateRhs, lhsType: TypeInstance) {
        constructExecutions(RESULT, emptySet(),emptySet(), template, lhsType)
    }

    fun execute(variables: Map<String, TypedObject<OT>>, src: TypedObject<OT>): TypedObject<OT> {
        val sorted = executionPlan()
        var evc = EvaluationContext.of(variables)
        for (pe in sorted) {
            evc = pe.execution.invoke(evc)
        }
        return evc.getOrInParent(RESULT) ?: accessorMutator.nothing()
    }

    private fun createTempVariable() = SimpleName("temp${_nextTempVarNum++}")

    /**
     * returns a simplified template where all property assignments are from variables
     * any non-identified template is given an artificial id.
     */
    private fun constructExecutions(tgtName: String, doBeforeMe: Set<M2mPatternExecution<OT>>, doAfterMe: Set<M2mPatternExecution<OT>>, template: PropertyTemplateRhs, lhsType: TypeInstance) {
        when (template) {
            is PropertyTemplateExpression -> constructExecutionsFromPropertyTemplateExpression(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            is ObjectTemplate -> constructExecutionsFromObjectTemplate(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            is CollectionTemplate -> constructExecutionsFromCollectionTemplate(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            else -> error("Unknown rhs type ${template::class}")
        }
    }

    private fun constructExecutionsFromPropertyTemplateExpression(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution<OT>>,
        doAfterMe: Set<M2mPatternExecution<OT>>,
        template: PropertyTemplateExpression,
        lhsType: TypeInstance
    ) {
        val inputs = when {
            template.expression is RootExpression -> listOf((template.expression as RootExpression).name)
            else -> emptyList()
        }
        val outputs = template.identifier?.let { listOf(it.value) } ?: emptyList()
        val exe = M2mPatternExecution($$"Execute expression: push EVC with $self := $${template.expression}", inputs, outputs) { evc ->
            val lhsType = StdLibDefault.AnyType // maybe not used!
            val v = createFromExpression(evc, lhsType, template.expression)
            evc.childSelf(v).also { it.setNamedValue(tgtName, v) } //TODO: would be nice not to store this twice !
        }.also {self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
        }
        _executions.add(exe)
    }

    private fun constructExecutionsFromObjectTemplate(tgtName: String, doBeforeMe: Set<M2mPatternExecution<OT>>, doAfterMe: Set<M2mPatternExecution<OT>>, template: ObjectTemplate, lhsType: TypeInstance) {
        val decl = template.type.resolvedDeclaration

        val inputs = emptyList<String>()
        val (id, outputs) = template.identifier?.let { Pair(it, listOf(it.value)) } ?: Pair(createTempVariable(), emptyList())

        val finishSetProperties = M2mPatternExecution<OT>($$"} // Finish set properties for $${decl.name.value}: pop EVC; $$tgtName := oldEvc.$self", emptyList(), emptyList()) { evc ->
            val self = evc.self
            evc.parent!!.also{ poppedEvc ->
                self?.let { poppedEvc.setNamedValue(tgtName, self) }
            } // pop the self and other temp vars
        }.also {self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
        }

        val startSetProperties = M2mPatternExecution<OT>($$"{ // Start set properties for $${decl.name.value}", emptyList(), emptyList()) { evc ->
            evc
        }.also { self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
            self.doAfterMe.add(finishSetProperties)
        }

        val creation = M2mPatternExecution($$"Create object: pop EVC; push EVC with $self := $${decl.name.value}(...) {...}", inputs, outputs) { evc ->
            val rhsMv = createFromRhs(evc, template.type, template)
            evc.parent!!.childSelf(rhsMv[RESULT]!!)
        }.also { self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
            self.doAfterMe.add(startSetProperties)
        }

        val startConstructorArgs = M2mPatternExecution($$"start collect constructor args push new EVC", inputs, outputs) { evc ->
            evc.child(emptyMap<String, TypedObject<OT>>())
        }.also { self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
            self.doAfterMe.add(creation)
        }

        _executions.add(startSetProperties)
        _executions.add(finishSetProperties)
        _executions.add(startConstructorArgs)
        _executions.add(creation)
        val constructors = when (decl) {
            is DataType -> decl.constructors
            is ValueType -> decl.constructors
            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
        }
        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
        template.propertyTemplate.forEach { (k, v) ->
            val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
            if (possibleConArgNames.contains(k.value)) {
                constructExecutionForConstructorArg(k.value, setOf(startConstructorArgs), setOf(creation), v, propType)
            } else {
                constructExecutionsFromPropertyTemplate(k.value, setOf(startSetProperties),setOf(finishSetProperties), v, propType)
            }
        }
    }

    private fun constructExecutionForConstructorArg(tgtName: String, doBeforeMe: Set<M2mPatternExecution<OT>>, doAfterMe: Set<M2mPatternExecution<OT>>, template: PropertyTemplate, lhsType: TypeInstance) {
        val inputs = emptyList<String>()
        val outputs = emptyList<String>()
        val computeArgument = M2mPatternExecution<OT>($$"Constructor argument: pop EVC; $$tgtName := oldEvc.$self", inputs, outputs) { evc ->
            val arg = evc.self
            evc.parent!!.also { poppedEvc ->
                arg?.let {
                    poppedEvc.setNamedValue(tgtName, arg)
                }
            }
        }.also { self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
        }
        val start = M2mPatternExecution<OT>($$"{ // Constructor argument", inputs, outputs) { evc ->
            evc
        }.also {self ->
            self.doAfterMe.addAll(doAfterMe)
            self.doAfterMe.add(computeArgument)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
        }
        _executions.add(start)
        _executions.add(computeArgument)
        val pn = template.propertyName.value
        val propType = lhsType.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
        constructExecutions(tgtName, setOf(start), setOf(computeArgument), template.rhs, propType)
    }

    private fun constructExecutionsFromPropertyTemplate(tgtName: String, doBeforeMe: Set<M2mPatternExecution<OT>>, doAfterMe: Set<M2mPatternExecution<OT>>, template: PropertyTemplate, lhsType: TypeInstance) {
        val inputs = emptyList<String>()
        val outputs = emptyList<String>()
        val setProperty = M2mPatternExecution($$"Set property: pop EVC; $self.$$tgtName := oldEvc.$self", inputs, outputs) { evc ->
            val pv = evc.self
            evc.parent!!.also { poppedEvc ->
                val obj = poppedEvc.self!!
                pv?.let {
                    setPropertyIfNothing(obj, tgtName,pv)
                }
            }
        }.also { self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
        }
        val start = M2mPatternExecution<OT>("{ // start set Property", inputs, outputs) { evc ->
            evc
        }.also { self ->
            self.doAfterMe.addAll(doAfterMe)
            self.doAfterMe.add(setProperty)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
        }
        _executions.add(start)
        _executions.add(setProperty)
        val pn = template.propertyName.value
        val propType = lhsType.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
        constructExecutions(tgtName, setOf(start), setOf(setProperty), template.rhs, propType)
    }

    private fun constructExecutionsFromCollectionTemplate(tgtName: String, doBeforeMe: Set<M2mPatternExecution<OT>>, doAfterMe: Set<M2mPatternExecution<OT>>, template: CollectionTemplate, lhsType: TypeInstance) {
        val inputs = emptyList<String>()
        val outputs = template.identifier?.let { listOf(it.value) } ?: emptyList()
        val createCollection = M2mPatternExecution("Create Collection: $tgtName := ${lhsType.typeName.value}(...)", inputs, outputs) { evc ->
            val rhsMv = createFromRhs(evc, lhsType, template)
            evc.childSelf(rhsMv[RESULT]!!)
        }.also { self ->
            self.doAfterMe.addAll(doAfterMe)
            doBeforeMe.forEach { it.doAfterMe.add(self) }
        }
        template.elements.forEach { elT ->
            val id = elT.identifier?.value ?: createTempVariable().value
            constructExecutions(id, setOf(), setOf(createCollection), elT, lhsType)
        }
        _executions.add(createCollection)
    }

    /**
     * +1 if e1 comes after e2
     * -1 if e2 comes after e1
     * exe1 comes after exe2 when:
     *  exe1.input contains any of exe2.outputs
     *  exe2.doAfterMe contains exe1
     */
    private fun compareExecutions(exe1: M2mPatternExecution<OT>, exe2: M2mPatternExecution<OT>): Int {
        return when {
            exe2.doAfterMe.contains(exe1) -> 1 // do exe1 after exe2
            exe1.doAfterMe.contains(exe2) -> -1 // do exe2 after exe1
            //exe2.mustComeBefore(exe1) -> 1 // do parent after child
            //exe1.mustComeBefore(exe2) -> -1 // do parent after child
            exe2.outputs.any { exe1.inputs.contains(it) } -> 1 // do outputs before inputs
            exe1.outputs.any { exe2.inputs.contains(it) } -> -1 // do outputs before inputs
            else -> 0
        }
    }

    // --- creation ---
    private fun createFromRhs(evc: EvaluationContext<OT>, lhsType: TypeInstance, rhs: PropertyTemplateRhs): Map<String, TypedObject<OT>> = when (rhs) {
        is PropertyTemplateExpression -> createFromPropertyTemplateExpression(evc, lhsType, rhs)
        is ObjectTemplate -> createFromObjectTemplate(evc, lhsType, rhs)
        is CollectionTemplate -> createFromCollectionTemplate(evc, lhsType, rhs)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    private fun createFromPropertyTemplateExpression(evc: EvaluationContext<OT>, lhsType: TypeInstance, rhs: PropertyTemplateExpression): Map<String, TypedObject<OT>> {
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
    private fun createFromExpression(evc: EvaluationContext<OT>, lhsType: TypeInstance, expression: Expression): TypedObject<OT> {
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(accessorMutator, issues)
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

    private fun createFromObjectTemplate(evc: EvaluationContext<OT>, lhsType: TypeInstance, template: ObjectTemplate): Map<String, TypedObject<OT>> {
        val id = template.identifier?.value
        val existing = evc.namedValues[id]
        return when (existing) {
            null -> {
                val decl = template.type.resolvedDeclaration
                when (decl) {
                    is DataType, is ValueType -> {
                        val matchedVars = mutableMapOf<String, TypedObject<OT>>()
                        val constructors = when (decl) {
                            is DataType -> decl.constructors
                            is ValueType -> decl.constructors
                            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
                        }
                        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
                        val conArgs = mutableMapOf<String, TypedObject<OT>>()
                        template.propertyTemplate.forEach { (k, v) ->
                            if (possibleConArgNames.contains(k.value)) {
                                val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
                                val rhsMv = createFromRhs(evc, propType, v.rhs) //TODO: Ideally fetch from variable !
                                matchedVars.putAll(rhsMv)
                                rhsMv[RESULT]?.let { conArgs[k.value] = it }
                            }
                        }
                        //.resolveType(tgtObjectGraph.typesDomain)
                        val o = accessorMutator.createStructureValue(template.type.qualifiedTypeName, conArgs)
                        val mv = template.identifier?.let { matchedVars + Pair(it.value, o) } ?: matchedVars
                        mv + Pair(RESULT,o)
                    }

                    else -> error("Cannot construct object of type ${decl.qualifiedName.value}")
                }
            }

            else -> emptyMap()
        }
    }

    private fun createFromCollectionTemplate(evc: EvaluationContext<OT>, lhsType: TypeInstance, collectionTemplate: CollectionTemplate): Map<String, TypedObject<OT>> {
        //collection may already have been created, (via when/where/etc) and be a captured variable
        val existing = collectionTemplate.identifier?.let { evc.namedValues[it.value] }
        return when {
            null == existing -> {
                // create new collection from template elements
                val matchedVars = mutableMapOf<String, TypedObject<OT>>()
                val elements = collectionTemplate.elements.map {
                    val rhsMv = createFromRhs(evc, lhsType.typeArguments[0].type, it)
                    matchedVars.putAll(rhsMv)
                    rhsMv[RESULT]!! //FIXME
                }
                val col = accessorMutator.createCollection(lhsType.qualifiedTypeName, elements)
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                mv
            }

            else -> {
                // try to match template elements against existing collection elements, if not matched then create them.
                val elements = mutableListOf<TypedObject<OT>>()
                accessorMutator.forEachIndexed(existing) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                TODO()
            }
        }
    }

    /**
     * sets the property value from the template (which should be a simple variable reference)
     * returns any new variable matches - I think none
     */
    private fun setPropertyIfNothing(obj: TypedObject<OT>, pn:String, pv:TypedObject<OT>) {
        val possiblePv = accessorMutator.getProperty(obj, pn)
        when {
            accessorMutator.isNothing(possiblePv) ->  accessorMutator.setProperty(obj, pn, pv)
            else -> Unit
        }
    }

}