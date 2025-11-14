/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kotlinx.komposite.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.DataTypeSimple
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesDomainSimple
import net.akehurst.language.types.asm.TypesNamespaceSimple


data class TypeRefInfo(
    val name:PossiblyQualifiedName,
    val args:List<TypeRefInfo>,
    val isNullable:Boolean
) {
    fun toTypeInstance(contextType:TypeDefinition):TypeInstance {
        val targs = args.map { it.toTypeInstance(contextType).asTypeArgument }
        return contextType.namespace.createTypeInstance(contextType.qualifiedName, name, targs, isNullable)
    }
}

//FIXME: currently using TypeModel as asm, should have its own really!
class KompositeSyntaxAnalyser2 : SyntaxAnalyserByMethodRegistrationAbstract<TypesDomain>() {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::qualifiedName)
        super.register(this::declaration)
        super.register(this::property)
        super.register(this::characteristic)
    }

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<TypesDomain>> = emptyMap()

    // unit = namespace+ ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypesDomain {
        val result = TypesDomainSimple(SimpleName("A Komposite"))
        val namespaces = (children as List<TypesNamespace?>).filterNotNull()
        namespaces.forEach { ns ->
            result.addNamespace(ns)
        }
        return result
    }

    // namespace = 'namespace' qualifiedName declaration+;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypesNamespace {
        val qualifiedName = children[1] as List<String?>
        val declaration = children[2] as List<((namespace: TypesNamespace) -> TypeDefinition)>
        val qn = QualifiedName(qualifiedName.joinToString(separator = "."))

        val ns = TypesNamespaceSimple(qn)
        declaration.forEach {
            val dec = it.invoke(ns)
            //ns.addDefinition(dec)
        }

        return ns
    }

    // qualifiedName = [ NAME / '.']+ ;
    private fun qualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return children.toSeparatedList<Any?, String, String>().items
    }

    // declaration = declKind NAME '{' property* '}' ;
    private fun declaration(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypesNamespace) -> TypeDefinition {
        val name = SimpleName(children[1] as String)
        val properties = children[3] as List<(StructuredType) -> PropertyDeclaration>
        val result = { namespace: TypesNamespace ->
            val decl = DataTypeSimple(namespace, name) //FIXME: using datatype, but maybe should distinguish!
            properties.forEach { it.invoke(decl) }
            decl
        }
        return result
    }

    // property = characteristic NAME ;
    private fun property(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (StructuredType) -> PropertyDeclaration {
        val characteristics: List<PropertyCharacteristic> = children[0] as List<PropertyCharacteristic>
        val name = PropertyName(children[1] as String)
        val result = { owner: StructuredType ->
            val typeInstance = StdLibDefault.AnyType
            owner.appendPropertyStored(name, typeInstance, characteristics.toSet())
        }
        return result
    }

    // characteristic
    //  = 'ref'    // reference
    //  | 'cmp'    // composite
    //  | 'dis'    // disregard / ignore
    //  ;
    private fun characteristic(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<PropertyCharacteristic> {
        return when (children[0] as String) {
            "ref" -> listOf(PropertyCharacteristic.REFERENCE)
            "cmp" -> listOf(PropertyCharacteristic.COMPOSITE)
            "dis" -> emptyList()
            else -> error("Value not allowed '${children[0]}'")
        }
    }

}