package net.akehurst.language.agl.m2mTransform.testing

import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.AsmSimpleBuilder
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.reference.builder.CrossReferenceDomainBuilder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.TypesNamespace
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.TypeDomainBuilder

data class TransformTestSuit(
    val description: String,
) {
    val typeDomains = mutableMapOf<DomainReference, TypesDomain>()
    val crossReferenceDomains = mutableMapOf<DomainReference, CrossReferenceDomain>()
    var transform: String = ""
    val testCase = mutableMapOf<String, TransformTestCase>()
}

data class TransformTestCase(
    val description: String,
) {
    val input = mutableMapOf<DomainReference, Asm>()
    var target: DomainReference? = null
    var expected: Asm? = null
    val expectedIssues: Set<LanguageIssue> = mutableSetOf<LanguageIssue>()
}

fun m2mTransformTestSuits(init: M2MTransformTestSuitListBuilder.() -> Unit): Map<String, TransformTestSuit> {
    val b = M2MTransformTestSuitListBuilder()
    b.init()
    return b.build()
}

@DslMarker
annotation class M2MTransformTestSuitDslMarker

@M2MTransformTestSuitDslMarker
class M2MTransformTestSuitListBuilder {

    private val testSuitList = mutableMapOf<String, TransformTestSuit>()

    fun testSuit(description: String, init: M2MTransformTestSuitBuilder.() -> Unit) {
        check(testSuitList.containsKey(description).not()) { "Duplicate test suit: '$description'" }
        val b = M2MTransformTestSuitBuilder(description)
        b.init()
        testSuitList[description] = b.build()
    }

    fun build(): Map<String, TransformTestSuit> {
        return testSuitList
    }

}

@M2MTransformTestSuitDslMarker
class M2MTransformTestSuitBuilder(
    val description: String
) {
    private var _transform = ""
    private var _typeDomains = mutableMapOf<DomainReference, TypesDomain>()
    private var _crossReferenceDomain = mutableMapOf<DomainReference, CrossReferenceDomain>()
    private var _testCase = mutableMapOf<String, TransformTestCase>()

    fun typesDomain(
        domainReference: String,
        domainName: String,
        resolveImports: Boolean,
        namespaces: List<TypesNamespace> = listOf(StdLibDefault),
        init: TypeDomainBuilder.() -> Unit
    ) {
        val b = TypeDomainBuilder(SimpleName(domainName), resolveImports, namespaces)
        b.init()
        val td = b.build()
        _typeDomains[DomainReference(domainReference)] = td
    }

    fun crossReferenceDomain(
        domainReference: String,
        name: String,
        init: CrossReferenceDomainBuilder.() -> Unit
    ) {
        val b = CrossReferenceDomainBuilder(name)
        b.init()
        val crd = b.build()
        _crossReferenceDomain[DomainReference(domainReference)] = crd
    }

    fun transform(transformString: String) {
        _transform = transformString.trimIndent()
    }

    fun testCase(description: String, init: M2MTransformTestCaseBuilder.() -> Unit) {
        check(_testCase.containsKey(description).not()) { "Duplicate test suit: '$description'" }
        val b = M2MTransformTestCaseBuilder(description, _typeDomains,_crossReferenceDomain)
        b.init()
        _testCase[description] = b.build()
    }

    fun build() = TransformTestSuit(description).also {
        it.transform = _transform
        it.typeDomains.putAll(_typeDomains)
        it.crossReferenceDomains.putAll(_crossReferenceDomain)
        it.testCase.putAll(_testCase)
    }
}

@M2MTransformTestSuitDslMarker
class M2MTransformTestCaseBuilder(
    val description: String,
    val typeDomains: Map<DomainReference, TypesDomain>,
    val crossReferenceDomain: MutableMap<DomainReference, CrossReferenceDomain>
) {

    private val _input = mutableMapOf<DomainReference, Asm>()
    private lateinit var _target: DomainReference
    private var _expected: Asm? = null
    private val _expectedIssues = mutableSetOf<LanguageIssue>()

    fun input(
        domainReference: String,
        defaultNamespace: QualifiedName = StdLibDefault.qualifiedName,
        sentenceId: Any? = null,
        context: SentenceContextAny? = null,
        /** need to pass in a context if you want to resolveReferences */
        resolveReferences: Boolean = true,
        failIfIssues: Boolean = true,
        resolvedReferences: MutableList<ResolvedReference> = mutableListOf(),
        init: AsmSimpleBuilder.() -> Unit
    ) {
        val dr = DomainReference(domainReference)
        val typesDomain: TypesDomain = this.typeDomains[dr]!!
        val defNs = typesDomain.findNamespaceOrNull(defaultNamespace) ?: StdLibDefault
        val crd = crossReferenceDomain[dr] ?: CrossReferenceDomainDefault(SimpleName("CrossReference"))
        val b = AsmSimpleBuilder(typesDomain, defNs, crd, sentenceId, context, resolveReferences, failIfIssues, resolvedReferences)
        b.init()
        _input[dr] = b.build()
    }

    fun expectIssue(kind: LanguageIssueKind, message: String, phase: LanguageProcessorPhase = LanguageProcessorPhase.INTERPRET, location: InputLocation? = null, data: Any? = null) {
        _expectedIssues.add(LanguageIssue(kind, phase, location, message, data))
    }

    fun target(
        domainReference: String,
        defaultNamespace: QualifiedName = StdLibDefault.qualifiedName,
        crossReferenceDomain: CrossReferenceDomain = CrossReferenceDomainDefault(SimpleName("CrossReference")),
        sentenceId: Any? = null,
        context: SentenceContextAny? = null,
        /** need to pass in a context if you want to resolveReferences */
        resolveReferences: Boolean = true,
        failIfIssues: Boolean = true,
        resolvedReferences: MutableList<ResolvedReference> = mutableListOf(),
        init: AsmSimpleBuilder.() -> Unit
    ) {
        val dr = DomainReference(domainReference)
        val typesDomain: TypesDomain = this.typeDomains[dr]!!
        val defNs = typesDomain.findNamespaceOrNull(defaultNamespace) ?: StdLibDefault
        val b = AsmSimpleBuilder(typesDomain, defNs, crossReferenceDomain, sentenceId, context, resolveReferences, failIfIssues, resolvedReferences)
        b.init()
        _target = dr
        _expected = b.build()
    }

    fun build() = TransformTestCase(description).also {
        it.target = _target
        it.input.putAll(_input)
        it.expected = _expected
        (it.expectedIssues as MutableSet).addAll(_expectedIssues)
    }
}