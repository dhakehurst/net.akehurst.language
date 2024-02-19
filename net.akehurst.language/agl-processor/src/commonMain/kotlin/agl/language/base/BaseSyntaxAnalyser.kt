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

package net.akehurst.language.agl.agl.language.base

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.collections.toSeparatedList

class BaseSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Any>() {

    override fun registerHandlers() {
        super.register(this::namespace)
        super.register(this::import)
        super.register(this::qualifiedName)
    }

    // namespace = 'namespace' qualifiedName ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> =
        children[1] as List<String>

    // import = 'import' qualifiedName ;
    private fun import(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> =
        children[1] as List<String>

    // qualifiedName = [IDENTIFIER / '.']+ ;
    private fun qualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> =
        (children as List<String>).toSeparatedList().items

}