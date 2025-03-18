package net.akehurst.language.agl.simple

import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.sentence.api.InputLocation

typealias CreateScopedItem< ItemType, ItemInScopeType> = ( referableName: String, item: ItemType, location:InputLocation?) -> ItemInScopeType
typealias ResolveScopedItem< ItemType, ItemInScopeType> = (itemInScope: ItemInScopeType) -> ItemType?

open class ContextWithScope<ItemType:Any, ItemInScopeType : Any>(
    val createScopedItem: CreateScopedItem< ItemType, ItemInScopeType>,
    val resolveScopedItem: ResolveScopedItem< ItemType, ItemInScopeType>
) : SentenceContext {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    var rootScope = ScopeSimple<ItemInScopeType>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)

    val isEmpty: Boolean get() = rootScope.isEmpty

    fun asString(): String = "context scope §root ${rootScope.asString()}"

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextWithScope<*,*> -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextWithScope"
}

open class ContextAsmSimple(
    createScopedItem: CreateScopedItem< AsmStructure, Any> = {  referableName, item, location -> Pair(location?.sentenceIdentity, item) },
    resolveScopedItem: ResolveScopedItem< AsmStructure, Any> = {  itemInScope -> (itemInScope as Pair<*,*>).second as AsmStructure }
) : ContextWithScope<AsmStructure, Any>(createScopedItem,resolveScopedItem)

//FIXME: this does not work as currently AsmPath is incorrectly calculated by SyntaxAnalyserFromAsmTransformAbstract
class ContextAsmSimpleWithAsmPath(
    map: MutableMap<String, AsmStructure> = mutableMapOf(),
) : ContextAsmSimple(
    {  referableName, item, location -> map[item.parsePath.value] = item; item.parsePath.value },
    {  itemInScope -> map[itemInScope] }
)