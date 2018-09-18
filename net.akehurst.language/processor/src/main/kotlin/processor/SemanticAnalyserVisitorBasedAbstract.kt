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

package net.akehurst.language.processor

import net.akehurst.language.api.analyser.GrammarLoader
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor
import net.akehurst.language.api.sppt2ast.Sppt2AstTransformer
import net.akehurst.language.api.sppt2ast.UnableToTransformSppt2AstExeception

typealias BranchHandler<T> = (SPPTBranch, List<SPPTBranch>, Any?) -> T

abstract class Sppt2AstTransformerVisitorBasedAbstract : Sppt2AstTransformer, SharedPackedParseTreeVisitor<Any, Any?> {

    private var grammarLoader: GrammarLoader? = null
    private val branchHandlers: MutableMap<String, BranchHandler<*>> = mutableMapOf()

    protected fun <T> register(branchName: String, handler: BranchHandler<T>) {
        this.branchHandlers[branchName] = handler
    }

    private fun <T> findBranchHandler(branchName: String): BranchHandler<T> {
        var handler: BranchHandler<T>? = this.branchHandlers[branchName] as BranchHandler<T>?
        if (null == handler) {
            /*
            try {
                val m = Sppt2AstTransformerVisitorBasedAbstract::class.constructors.find {
                    it.parameters[0].type == SPPTBranch::class
                            && it.parameters[1].type == List::class
                            && it.parameters[2].type == Any::class
                }
                val handler = {target:SPPTBranch, children:List<SPPTBranch>, arg:Any? ->
                    try {
                        m?.call(this, target, children, arg) as T
                    } catch (e: Exception) {
                        throw UnableToTransformSppt2AstExeception("Error invoking method named $branchName", e)
                    }
                }
                this.register<T>(branchName, handler)
            } catch (e: Exception) {
            */
                throw UnableToTransformSppt2AstExeception("Cannot find sppt2ast method named $branchName", null)
            //}

        }
        return handler ?: throw UnableToTransformSppt2AstExeception("Cannot find sppt2ast method named $branchName", null)
    }

    protected fun <T> transform(branch: SPPTBranch?, arg: Any?): T? {
        return if (null == branch) null else branch.accept(this, arg) as T
    }

    // --- IParseTreeVisitor ---
    override fun visit(target: SharedPackedParseTree, arg: Any?): Any {
        val root = target.root
        return root.accept(this, arg)
    }

    override fun visit(target: SPPTLeaf, arg: Any?): Any {
        return target.matchedText
    }

    override fun visit(target: SPPTBranch, arg: Any?): Any {
        val branchName = target.name
        val handler = this.findBranchHandler<Any>(branchName)
        if (null == handler) {
            throw UnableToTransformSppt2AstExeception("Branch not handled in sppt2ast $branchName", null)
        } else {
            val branchChildren = target.branchNonSkipChildren// .stream().map(it -> it.getIsEmpty() ? null :
            // it).collect(Collectors.toList());
            try {
                return handler.invoke(target, branchChildren, arg)
            } catch (e: Exception) {
                throw UnableToTransformSppt2AstExeception("Exception trying to transform ${target}",e)
            }
        }

    }

}