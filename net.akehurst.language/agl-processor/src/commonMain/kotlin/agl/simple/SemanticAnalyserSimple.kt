/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.processor.SemanticTag
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault

class SemanticAnalyserSimple(
    val typesDomain: TypesDomain,
    val crossReferenceDomain: CrossReferenceDomain
) : SemanticAnalyser<Asm, SentenceContext> {

    companion object {

        /**
         * The identifying expression for an element of type 'typeQualifiedName' in the scope created by an element of type 'scopeForTypeQualifiedName',
         * found by type conformance (see CrossReferenceDomain.identifyingExpressionFor).
         * null if none, or if ambiguous (then onAmbiguous is called).
         */
        fun identifyingExpressionFor(
            typesDomain: TypesDomain,
            crossReferenceDomain: CrossReferenceDomain,
            scopeForTypeQualifiedName: QualifiedName,
            typeQualifiedName: QualifiedName,
            onAmbiguous: ((message: String) -> Unit)? = null
        ): Expression? {
            val match = crossReferenceDomain.identifyingExpressionFor(typesDomain, scopeForTypeQualifiedName, typeQualifiedName)
            return when {
                match.isAmbiguous -> {
                    val alts = match.candidates.joinToString(separator = ", ") { "'identify ${it.typeName.value} by ${it.identifiedBy.asString()}'" }
                    onAmbiguous?.invoke("Ambiguous identification of type '${typeQualifiedName.value}' in scope for '${scopeForTypeQualifiedName.value}', candidates: $alts")
                    null
                }

                else -> match.singleOrNull?.identifiedBy
            }
        }

        /**
         * value of the identifying expression for 'self' in the scope created by an element of type 'inScopeForTypeQualifiedName',
         * declarations are found by type conformance
         */
        fun identifyingValueInFor(
            interpreter: ExpressionsInterpreterOverTypedObjectCombined,
            crossReferenceDomain: CrossReferenceDomain,
            inScopeForTypeQualifiedName: QualifiedName,
            self: AsmStructure,
            specialValuesFor: (AsmStructure) -> Map<String, AsmStructure?> = NO_SPECIAL_VALUES,
            onAmbiguous: ((message: String) -> Unit)? = null
        ): Any? {
            return identifyingValuesInFor(interpreter, crossReferenceDomain, inScopeForTypeQualifiedName, self, specialValuesFor, onAmbiguous).firstOrNull()
        }

        /** no special values ('\$owner', '\$container', '\$root') for an identifying expression */
        val NO_SPECIAL_VALUES: (AsmStructure) -> Map<String, AsmStructure?> = { emptyMap() }

        /**
         * the identifying expressions ('identify T by a | b') for an element of type 'typeQualifiedName' in the scope created by an element of type 'scopeForTypeQualifiedName',
         * empty if none or ambiguous
         */
        fun identifyingAlternativesFor(
            typesDomain: TypesDomain,
            crossReferenceDomain: CrossReferenceDomain,
            scopeForTypeQualifiedName: QualifiedName,
            typeQualifiedName: QualifiedName,
            onAmbiguous: ((message: String) -> Unit)? = null
        ): List<Expression> {
            val exp = identifyingExpressionFor(typesDomain, crossReferenceDomain, scopeForTypeQualifiedName, typeQualifiedName, onAmbiguous)
            return when (exp) {
                null -> emptyList()
                else -> crossReferenceDomain.identifyingExpressionFor(typesDomain, scopeForTypeQualifiedName, typeQualifiedName).singleOrNull?.identifiedByAlternatives ?: listOf(exp)
            }
        }

        /**
         * the values of all identifying expressions ('identify T by a | b') of 'self' that have one, in order, without repeats.
         * The first is the element's name, the others are further names for it.
         * 'specialValuesFor' gives the elements for the special values ('\$owner', '\$container', '\$root') of 'self'.
         */
        fun identifyingValuesInFor(
            interpreter: ExpressionsInterpreterOverTypedObjectCombined,
            crossReferenceDomain: CrossReferenceDomain,
            inScopeForTypeQualifiedName: QualifiedName,
            self: AsmStructure,
            specialValuesFor: (AsmStructure) -> Map<String, AsmStructure?> = NO_SPECIAL_VALUES,
            onAmbiguous: ((message: String) -> Unit)? = null
        ): List<Any> {
            val exps = identifyingAlternativesFor(interpreter.typesDomain, crossReferenceDomain, inScopeForTypeQualifiedName, self.qualifiedTypeName, onAmbiguous)
            return when {
                exps.size > 1 -> exps.mapNotNull { exp ->
                    // an alternative is often absent (e.g. no short name): no warnings (e.g. navigating $nothing) for an alternative that has no value
                    interpreter.objectGraph.issues.dropIssuesRaisedIf(LanguageIssueKind.WARNING, { identifyingValueOf(interpreter, exp, self, specialValuesFor) }) { it == null }
                }.distinct()

                else -> exps.mapNotNull { identifyingValueOf(interpreter, it, self, specialValuesFor) }.distinct()
            }
        }

        private fun identifyingValueOf(
            interpreter: ExpressionsInterpreterOverTypedObjectCombined,
            exp: Expression?,
            self: AsmStructure,
            specialValuesFor: (AsmStructure) -> Map<String, AsmStructure?>
        ): Any? = when (exp) {
            null -> null
            else -> {
                val elType = interpreter.typesDomain.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                val specials = specialValuesFor(self).mapValues { (_, el) ->
                    when (el) {
                        null -> interpreter.objectGraph.nothing()
                        else -> interpreter.objectGraph.typedAs(el, interpreter.typesDomain.findByQualifiedNameOrNull(el.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType)
                    }
                }
                val value = interpreter.evaluateExpression(EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, elType), specials), exp).self
                when {
                    value is String -> value
                    value is AsmPrimitive && value.isStdString -> value.value as String
                    value is AsmList && value.elements.all { it is AsmPrimitive && it.isStdString } -> value.elements.map { (it as AsmPrimitive).value as String }
                    value is AsmAny && value.value is String -> value.value
                    else -> null
                }
            }
        }

         fun identifyingValueInFor(interpreter: ExpressionsInterpreterOverTypedObjectCombined, crossReferenceDomain: CrossReferenceDomain, inScopeForTypeName: SimpleName, self: AsmStructure): Any? {
            return when {
                //crossReferenceModel.isScopeDefinedFor(self.qualifiedTypeName).not() -> null
                else -> {
                    val exp = crossReferenceDomain.identifyingExpressionFor(inScopeForTypeName, self.qualifiedTypeName)
                    when (exp) {
                        null -> null
                        else -> {
                            val elType = interpreter.typesDomain.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                            val value = interpreter.evaluateExpression(EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, elType)), exp).self
                            when {
                                value is String -> value
                                value is AsmPrimitive && value.isStdString -> value.value as String
                                value is AsmList && value.elements.all { it is AsmPrimitive && it.isStdString } -> value.elements.map { (it as AsmPrimitive).value as String }
                                value is AsmAny && value.value is String -> value.value
                                else -> null//error("Cannot get identifying value for $value")
                            }
                        }
                    }
                }
            }
        }
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _resolvedReferences = mutableListOf<ResolvedReference>()
    private lateinit var _locationMap: LocationMap
    /** element -> names it is declared with (added to a scope under) */
    private val _declared = mutableListOf<Pair<AsmStructure, String>>()
    /** type qualified name -> qualified names of the types it conforms to (itself first) */
    private val _conformingTypeNames = mutableMapOf<QualifiedName, List<String>>()

    private val _accessor by lazy {
        objectGraphSimpleAsm(typesDomain, null, _issues, _locationMap)
    }
    private val _interpreter by lazy {
        ExpressionsInterpreterOverTypedObjectCombined(_accessor)
    }

    /** the elements for the special values ('\$owner', '\$container', '\$root') in identifying expressions, set by the phase that is running (scope creation or reference resolution) */
    private var _specialValuesFor: (AsmStructure) -> Map<String, AsmStructure?> = NO_SPECIAL_VALUES

    override fun clear() {
        _resolvedReferences.clear()
        _issues.clear()
        _declared.clear()
    }

    override  fun analyse(
        sentenceIdentity: Any?,
        asm: Asm,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        val context = options.sentenceContext
        this._locationMap = locationMap ?: LocationMapDefault()
        when {
            null == context -> _issues.info(null, "No context provided, references not built, checked or resolved, switch off semanticAnalysis or provide a context.")
            else -> {
                buildScope(options, sentenceIdentity, asm, context)
                checkAndResolveReferences(options, sentenceIdentity, asm, _locationMap, context)
            }
        }
        return SemanticAnalysisResultDefault(_resolvedReferences, _issues, semanticTags(asm))
    }

    /**
     * The semantic tags of the sentence (for styling):
     * 'type T' on every element (T = its type and each type it conforms to),
     * 'identify T' on the identifier that declares an element, 'refers-to T' on the identifier of each resolved reference (T from the target).
     * An identifier gets tags only where the syntax analyser recorded the location of the property that holds it.
     */
    private fun semanticTags(asm: Asm): List<SemanticTag> {
        val result = mutableListOf<SemanticTag>()
        val visited = mutableSetOf<AsmStructure>()
        fun walk(v: Any?) {
            when (v) {
                is AsmStructure -> if (visited.add(v)) {
                    _locationMap[v]?.let { loc -> result.add(SemanticTag(loc, conformingTypeNames(v.qualifiedTypeName).map { SemanticTag.tag(SemanticTag.KIND_TYPE, it) })) }
                    v.children.forEach { walk(it) }
                }

                is AsmList -> v.elements.forEach { walk(it) }
                is Collection<*> -> v.forEach { walk(it) }
                else -> Unit
            }
        }
        asm.root.forEach { walk(it) }
        _declared.distinct().forEach { (el, name) ->
            identifierLocation(el, name)?.let { loc ->
                result.add(SemanticTag(loc, conformingTypeNames(el.qualifiedTypeName).map { SemanticTag.tag(SemanticTag.KIND_IDENTIFY, it) }))
            }
        }
        _resolvedReferences.forEach { ref ->
            val loc = ref.referringLocation
            val target = ref.target as? AsmStructure
            if (null != loc && null != target) {
                result.add(SemanticTag(loc, conformingTypeNames(target.qualifiedTypeName).map { SemanticTag.tag(SemanticTag.KIND_REFERS_TO, it) }))
            }
        }
        return result
    }

    /** location of the property of 'el' whose value is 'name' (or a list whose last part is 'name') */
    private fun identifierLocation(el: AsmStructure, name: String): InputLocation? =
        el.propertyOrdered.firstNotNullOfOrNull { prop ->
            val pv = prop.value
            val holdsName = pv == name || (pv is List<*> && pv.lastOrNull() == name)
            if (holdsName) _locationMap.getPropertyLocation(el, prop.name.value) else null
        }

    private fun conformingTypeNames(typeName: QualifiedName): List<String> = _conformingTypeNames.getOrPut(typeName) {
        val def = typesDomain.findByQualifiedNameOrNull(typeName)
        when (def) {
            null -> listOf(typeName.value)
            else -> listOf(typeName.value) + typesDomain.allDefinitions
                .filter { it != def && def.conformsTo(it) && it != StdLibDefault.AnyType.resolvedDefinition }
                .map { it.qualifiedName.value }
        }
    }

    private  fun checkAndResolveReferences(options: SemanticAnalysisOptions<SentenceContext>, sentenceIdentity: Any?, asm: Asm, locationMap: LocationMap, context: SentenceContext) {
        when {
            options.checkReferences.not() -> _issues.info(null, "Semantic Analysis option 'checkReferences' is off, references not checked.")
            crossReferenceDomain.isEmpty -> _issues.info(null, "The language has no references definition (CrossReferenceDomain is empty), so no references are checked or resolved.")
            else -> {
                val resolve = if (options.resolveReferences) {
                    true
                } else {
                    _issues.info(null, "Semantic Analysis option 'resolveReferences' is off, references checked but not resolved.")
                    false
                }
                this.walkReferences(sentenceIdentity, asm, _locationMap, context, resolve)
            }
        }
    }

    private  fun walkReferences(sentenceId: Any?, asm: Asm, locationMap: LocationMap, context: SentenceContext, resolve: Boolean) {
        val resFunc: ((ref: Any) -> AsmStructure?)? = if (resolve) {
            { ref -> context.resolveScopedItem.invoke(ref) as? AsmStructure }
        } else {
            null
        }
        val sentenceScope = context.getScopeForSentenceOrNull(sentenceId)
        if(null!=sentenceScope) {
            val resolver = ReferenceResolverSimple(
                typesDomain,
                crossReferenceDomain,
                context,
                sentenceId,
                { scopeType: QualifiedName, el: AsmStructure -> identifyingValueInFor(scopeType, el) },
                resFunc,
                locationMap, _accessor
            )
            _specialValuesFor = resolver::specialValuesFor
            asm.traverseDepthFirst(resolver)
            _specialValuesFor = NO_SPECIAL_VALUES
            _resolvedReferences = resolver.resolvedReferences
        } else {
            _issues.info(null, "Scope for sentence with Identity '${sentenceId}' not found, so cannot resolve references.")
            false
        }
    }

    private  fun buildScope(options: SemanticAnalysisOptions<SentenceContext>, sentenceIdentity: Any?, asm: Asm, context: SentenceContext) {
        when {
            options.buildScope.not() -> _issues.info(null, "Semantic Analysis option 'buildScope' is off, scope is not built.")
            else -> {
                val scopeCreator = ScopeCreator(
                    typesDomain,
                    crossReferenceDomain as CrossReferenceDomainDefault,
                    context,
                    sentenceIdentity,
                    options.replaceIfItemAlreadyExistsInScope,
                    options.ifItemAlreadyExistsInScopeIssueKind,
                    { scopeType: QualifiedName, el: AsmStructure -> identifyingValueInFor(scopeType, el) },
                    _locationMap, _issues,
                    { scopeType: QualifiedName, el: AsmStructure -> identifyingValuesInFor(_interpreter, crossReferenceDomain, scopeType, el, _specialValuesFor).drop(1) },
                    { el: AsmStructure, name: String -> _declared.add(Pair(el, name)) }
                )
                _specialValuesFor = scopeCreator::specialValuesFor
                asm.traverseDepthFirst(scopeCreator)
                _specialValuesFor = NO_SPECIAL_VALUES
            }
        }
    }

    private  fun identifyingValueInFor(inScopeNamed: SimpleName, self: AsmStructure): Any? {
        val id = identifyingValueInFor(_interpreter, crossReferenceDomain, inScopeNamed, self)
        return id
    }

    private fun identifyingValueInFor(inScopeForTypeQualifiedName: QualifiedName, self: AsmStructure): Any? =
        identifyingValueInFor(_interpreter, crossReferenceDomain, inScopeForTypeQualifiedName, self, _specialValuesFor) { msg ->
            _issues.error(_locationMap[self], msg)
        }

}