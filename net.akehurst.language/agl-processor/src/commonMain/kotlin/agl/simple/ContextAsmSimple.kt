package net.akehurst.language.agl.simple

import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scope.api.ItemInScope
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.sentence.api.InputLocation
import kotlin.collections.set

typealias CreateScopedItem<ItemType, ItemInScopeType> = (referableName: String, item: ItemType, location: InputLocation?) -> ItemInScopeType
typealias ResolveScopedItem<ItemType, ItemInScopeType> = (itemInScope: ItemInScopeType) -> ItemType?

open class ContextWithScope<ItemType : Any, ItemInScopeType : Any>(
    val createScopedItem: CreateScopedItem<ItemType, ItemInScopeType>,
    val resolveScopedItem: ResolveScopedItem<ItemType, ItemInScopeType>
) : SentenceContext {

    companion object {
        private val NULL_SENTENCE_IDENTIFIER = object : Any() {
            override fun toString() = "NULL_SENTENCE_IDENTIFIER"
        }
    }

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    //var rootScope = ScopeSimple<ItemInScopeType>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
    val scopeForSentence = mutableMapOf<Any, ScopeSimple<ItemInScopeType>>()

    val isEmpty: Boolean get() = scopeForSentence.all { (k,v) -> v.isEmpty }

    fun clear() {
        scopeForSentence.clear()
    }

    fun newScopeForSentence(sentenceIdentity: Any?): Scope<ItemInScopeType> {
        val newScope = ScopeSimple<ItemInScopeType>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
        scopeForSentence[sentenceIdentity ?: NULL_SENTENCE_IDENTIFIER] = newScope
        return newScope
    }

    fun getScopeForSentenceOrNull(sentenceIdentity: Any?): Scope<ItemInScopeType>? {
        return if (null == sentenceIdentity) {
            scopeForSentence[NULL_SENTENCE_IDENTIFIER]
                ?: newScopeForSentence(NULL_SENTENCE_IDENTIFIER)
        } else {
            scopeForSentence[sentenceIdentity]
        }
    }

    fun findItemsConformingTo(conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> {
        return scopeForSentence.flatMap { it.value.findItemsConformingTo(conformsToFunc) }
    }

    fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> {
        return scopeForSentence.flatMap { it.value.findItemsNamedConformingTo(name, conformsToFunc) }
    }

    fun asString(): String = "context scope ${scopeForSentence.entries.joinToString("\n") { (k, v) -> "$k = ${v.asString()}" }}"

    override fun hashCode(): Int = scopeForSentence.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextWithScope<*, *> -> false
        this.scopeForSentence != other.scopeForSentence -> false
        else -> true
    }

    override fun toString(): String = "ContextWithScope"
}

open class ContextAsmSimple(
    createScopedItem: CreateScopedItem<AsmStructure, Any> = { referableName, item, location -> Pair(location?.sentenceIdentity, item) },
    resolveScopedItem: ResolveScopedItem<AsmStructure, Any> = { itemInScope -> (itemInScope as Pair<*, *>).second as AsmStructure }
) : ContextWithScope<AsmStructure, Any>(createScopedItem, resolveScopedItem)

//FIXME: this does not work as currently AsmPath is incorrectly calculated by SyntaxAnalyserFromAsmTransformAbstract
class ContextAsmSimpleWithAsmPath(
    map: MutableMap<String, AsmStructure> = mutableMapOf(),
) : ContextAsmSimple(
    { referableName, item, location -> map[item.parsePath.value] = item; item.parsePath.value },
    { itemInScope -> map[itemInScope] }
)