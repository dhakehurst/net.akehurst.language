/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.base.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.*
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class BaseSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Any>() {

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::import)
        super.register(this::definition)
        super.register(this::option)
        super.register(this::possiblyQualifiedName)
    }

    // unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ModelDefault {
        val options = children[0] as List<Option>
        val namespace = children[1] as List<NamespaceDefault>
        val result = ModelDefault(SimpleName("Unit"), options, namespace)
        return result
    }

    // namespace = 'namespace' possiblyQualifiedName option* import* definition*;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NamespaceDefault {
        val pqn = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Option>
        val import = children[3] as List<Import>
        val definition = children[4] as List<(NamespaceDefault) -> DefinitionDefault>

        val ns = NamespaceDefault(pqn.asQualifiedName(null),options, import)
        definition.forEach {
            val def = it.invoke(ns)
            ns.addDefinition(def)
        }
        return ns
    }

    // import = 'import' possiblyQualifiedName ;
    private fun import(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Import =
       Import( (children[1] as PossiblyQualifiedName).value )

    //definition = 'definition' IDENTIFIER ;
    private fun definition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (NamespaceDefault) -> DefinitionDefault {
        val id = SimpleName(children[1] as String)
        return { ns -> DefinitionDefault(ns,id) }
    }

    // option = '#' IDENTIFIER ':' IDENTIFIER ;
    private fun option(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): OptionDefault {
        val name = children[1] as String
        val value = children[3] as String
        return OptionDefault(name,value)
    }

    // possiblyQualifiedName = [IDENTIFIER / '.']+ ;
    private fun possiblyQualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        (children as List<String>).joinToString(separator = "").asPossiblyQualifiedName

}