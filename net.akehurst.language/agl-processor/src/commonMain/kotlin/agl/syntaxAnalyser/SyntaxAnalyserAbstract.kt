/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.syntaxAnalyser.GrammarLoader
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyserException

typealias BranchHandler<T> = (SPPTBranch, List<SPPTBranch>, Any?) -> T

abstract class SyntaxAnalyserAbstract : SyntaxAnalyser, SharedPackedParseTreeVisitor<Any, Any?> {

    private var grammarLoader: GrammarLoader? = null
    private val branchHandlers: MutableMap<String, BranchHandler<*>> = mutableMapOf()

    override val locationMap = mutableMapOf<Any, InputLocation>()

    protected fun <T> register(branchName: String, handler: BranchHandler<T>) {
        this.branchHandlers[branchName] = handler
    }

    private fun <T> findBranchHandler(branchName: String): BranchHandler<T> {
        val handler: BranchHandler<T>? = this.branchHandlers[branchName] as BranchHandler<T>?
        return handler ?: throw SyntaxAnalyserException("Cannot find SyntaxAnalyser branch handler method named $branchName", null)
    }

    protected fun <T> transformBranch(branch: SPPTBranch, arg: Any?): T {
        return this.transformBranchOpt(branch, arg) ?: throw SyntaxAnalyserException("cannot transform ${branch}", null)
    }

    protected fun <T> transformBranchOpt(branch: SPPTBranch?, arg: Any?): T? {
        return if (null == branch){
            null
        }else {
            val asm = this.visitBranch(branch, arg) as T
            this.locationMap[asm as Any] = branch.location
            asm
        }
    }

    // --- IParseTreeVisitor ---
    override fun visitTree(target: SharedPackedParseTree, arg: Any?): Any {
        val root = target.root
        return this.visitNode(root, arg)
    }

    override fun visitLeaf(target: SPPTLeaf, arg: Any?): Any {
        return target.matchedText
    }

    override fun visitBranch(target: SPPTBranch, arg: Any?): Any {
        val branchName = target.name
        val handler = this.findBranchHandler<Any>(branchName)
        val branchChildren = target.branchNonSkipChildren// .stream().map(it -> it.getIsEmpty() ? null :
        // it).collect(Collectors.toList());
        try {
            return handler.invoke(target, branchChildren, arg)
        } catch (e: Exception) {
            throw SyntaxAnalyserException("Exception trying to transform ${target}", e)
        }
    }

}