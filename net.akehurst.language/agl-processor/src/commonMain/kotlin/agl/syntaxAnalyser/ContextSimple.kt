/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.Scope
import net.akehurst.language.api.processor.SentenceContext

class ContextSimple() : SentenceContext<AsmElementPath> {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    override var rootScope = ScopeSimple<AsmElementPath>(null, "", ScopeModelAgl.ROOT_SCOPE_TYPE_NAME)

}

fun ScopeModelAgl.createReferenceLocalToScope(scope: ScopeSimple<AsmElementPath>, element: AsmElementSimple): String? {
    val prop = this.getReferablePropertyNameFor(scope.forTypeName, element.typeName)
    return when (prop) {
        null -> null
        ScopeModelAgl.IDENTIFY_BY_NOTHING -> ""
        else -> element.getPropertyAsString(prop)
    }
}

class ScopeSimple<AsmElementIdType>(
    val parent: ScopeSimple<AsmElementIdType>?,
    val forReferenceInParent:String,
    val forTypeName:String
) : Scope<AsmElementIdType> {

    //should only be usde for rootScope
    val scopeMap = mutableMapOf<AsmElementIdType, ScopeSimple<AsmElementIdType>>()

    private val _childScopes = mutableMapOf<String,ScopeSimple<AsmElementIdType>>()

    // typeName -> referableName -> item
    private val _items: MutableMap<String,MutableMap<String,AsmElementIdType>> = mutableMapOf()

    val rootScope:ScopeSimple<AsmElementIdType> by lazy {
        var s = this
        while(null!=s.parent) s = s.parent!!
        s
    }

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    val childScopes:Map<String,ScopeSimple<AsmElementIdType>> = _childScopes

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    val items:Map<String,Map<String,AsmElementIdType>> = _items

    val path:List<String> by lazy {
        if (null==parent) emptyList() else parent.path + forReferenceInParent
    }

    fun createOrGetChildScope(forReferenceInParent:String, forTypeName:String, elementId:AsmElementIdType): ScopeSimple<AsmElementIdType> {
        var child = this._childScopes[forReferenceInParent]
        if (null==child) {
            child = ScopeSimple<AsmElementIdType>(this, forReferenceInParent, forTypeName)
            this._childScopes[forReferenceInParent] = child
        }
        this.rootScope.scopeMap[elementId] = child
        return child
    }

    fun addToScope(referableName:String, typeName:String, asmElementId:AsmElementIdType) {
        var m = this._items[typeName]
        if (null==m) {
            m = mutableMapOf()
            this._items[typeName] = m
        }
        m[referableName]=asmElementId
    }

    fun findOrNull(referableName:String, typeName:String):AsmElementIdType? = this._items[typeName]?.get(referableName)

    override fun isMissing(referableName:String, typeName:String):Boolean = null==this.findOrNull(referableName,typeName)

    override fun toString(): String = when {
        null==parent -> "/$forTypeName"
        else -> "$parent/$forTypeName"
    }
}


//fun ScopeModelAgl.createReferenceFromRoot(scope: ScopeSimple<AsmElementPath>, element: AsmElementSimple): AsmElementPath {
//    return element.asmPath
//}

//fun ScopeModelAgl.resolveReference(asm:AsmSimple, rootScope: ScopeSimple<AsmElementPath>, reference: AsmElementPath): AsmElementSimple? {
//    return asm.index[reference]
//}