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
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.asm.DefinitionDefault
import net.akehurst.language.base.asm.DomainDefault
import net.akehurst.language.base.asm.NamespaceDefault
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class BaseSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Any>() {

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::import)
        super.register(this::definition)
        super.register(this::option)
        super.register(this::optionValue)
        super.register(this::possiblyQualifiedName)
        super.register(this::literal)
    }

    // unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): DomainDefault {
        val options = children[0] as List<Pair<String, Any>>
        val namespace = children[1] as List<NamespaceDefault>
        val optHolder = OptionHolderDefault(null, options.toMap())
        namespace.forEach { (it.options as OptionHolderDefault).parent = optHolder }
        val result = DomainDefault(SimpleName("Unit"), optHolder, namespace)
        return result
    }

    // namespace = 'namespace' possiblyQualifiedName option* import* definition*;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NamespaceDefault {
        val pqn = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Pair<String, Any>>
        val import = children[3] as List<Import>
        val definition = children[4] as List<(NamespaceDefault) -> DefinitionDefault>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val ns = NamespaceDefault(pqn.asQualifiedName(null), optHolder, import)
        definition.forEach {
            val def = it.invoke(ns)
            ns.addDefinition(def)
        }
        return ns
    }

    // import = 'import' possiblyQualifiedName ;
    private fun import(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Import =
        Import((children[1] as PossiblyQualifiedName).value)

    //definition = 'definition' IDENTIFIER ;
    private fun definition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (NamespaceDefault) -> DefinitionDefault {
        val id = SimpleName(children[1] as String)
        return { ns -> DefinitionDefault(ns, id).also { setLocationFor(it, nodeInfo, sentence) } }
    }

    // option = '#' IDENTIFIER (':' optionValue)? ;
    private fun option(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, Any> {
        val name = children[1] as String
        val value = (children[2] as List<Any>?)?.let { it[1] } ?: true
        return Pair(name, value)
    }

    // optionValue = IDENTIFIER | literal ;
    private fun optionValue(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Any =
        children[0] as Any

    // possiblyQualifiedName = [IDENTIFIER / '.']+ ;
    private fun possiblyQualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        (children as List<String>).joinToString(separator = "").asPossiblyQualifiedName

    // literal = BOOLEAN | INTEGER | REAL | STRING ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Any = when (nodeInfo.alt.option.asIndex) {
        0 -> (children[0] as String).toBoolean()
        1 -> (children[0] as String).toLong()
        2 -> (children[0] as String).toDouble()
        3 -> (children[0] as String).trim('\'')
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
    }
}