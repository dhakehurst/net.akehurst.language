/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.editor.worker

import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.editor.common.*
import net.akehurst.language.processor.Agl
import org.w3c.dom.DedicatedWorkerGlobalScope

external val self: DedicatedWorkerGlobalScope

class AglWorker {

    val agl = AglComponents()
    val tokenizer = AglTokenizer(this.agl)

    init {
        start()
    }

    fun start() {
        self.onmessage = {
            val msg: dynamic = it.data
            when (msg.action) {
                "MessageProcessorCreate" -> this.createProcessor(msg.grammarStr)
                "MessageParserInterruptRequest" -> this.interrupt(msg.reason)
                "MessageParseRequest" -> this.parse(msg.text)
                "MessageSetStyle" -> this.setStyle(msg.css)
            }
        }
    }

    fun createProcessor(grammarStr: String?) {
        if (null == grammarStr) {
            this.agl.processor = null
        } else {
            //cheet because I don't want to serialise grammars
            when (grammarStr) {
                "@Agl.grammarProcessor@" -> this.agl.processor = Agl.grammarProcessor
                "@Agl.styleProcessor@" -> this.agl.processor = Agl.styleProcessor
                "@Agl.formatProcessor@" -> this.agl.processor = Agl.formatProcessor
                else -> this.agl.processor = Agl.processor(grammarStr)
            }
        }
    }

    fun interrupt(reason: String) {
        this.agl.processor?.interrupt(reason)
    }

    fun setStyle(css: String) {
        val rules: List<AglStyleRule> = Agl.styleProcessor.process(css)
        rules.forEach { rule ->
            var cssClass = this.agl.tokenToClassMap.get(rule.selector)
            if (null == cssClass) {
                cssClass = this.agl.cssClassPrefix + this.agl.nextCssClassNum++
                this.agl.tokenToClassMap.set(rule.selector, cssClass);
            }
        }
    }

    fun parse(sentence: String) {
        try {
            this.agl.sppt = null
            val proc = this.agl.processor
            if (null == proc) {
                //do nothing
            } else {
                val sppt = proc.parse(sentence)
                val tree = createParseTree(sppt.root)
                self.postMessage(MessageParseSuccess(tree))
                this.sendParseLineTokens(sppt)
                this.process(sppt)
            }
        } catch (e: ParseFailedException) {
            val sppt = e.longestMatch
            val tree = createParseTree(sppt!!.root)
            self.postMessage(MessageParseFailure(e.message!!, e.location, tree))
            this.sendScanLineTokens(sentence)
        } catch (t: Throwable) {
            self.postMessage(MessageParseFailure(t.message!!, null, null))
        }
    }

    fun process(sppt:SharedPackedParseTree) {
        try {
            val asm = this.agl.processor!!.process<Any>(sppt)
            val asmTree = createAsmTree(asm) ?: "No Asm"
            self.postMessage(MessageProcessSuccess(asmTree))
        } catch (t:Throwable) {
            self.postMessage(MessageProcessFailure(t.message!!))
        }
    }

    fun sendScanLineTokens(sentence: String) {
        val lines = sentence.split("\n")
        var state = AglLineState(0, "", emptyList())
        val lineTokens = mutableListOf<AglLineState>()
        lines.forEachIndexed { lineNum, lineText ->
            state = this.tokenizer.getLineTokens(lineText, state, lineNum)
            lineTokens.add(state)
        }
        val lt = lineTokens.map {
            it.tokens.toTypedArray()
        }.toTypedArray()
        self.postMessage(MessageLineTokens(lt))
    }

    fun sendParseLineTokens(sppt: SharedPackedParseTree) {
        if (null == sppt) {
            //nothing
        } else {
            val lineTokens = sppt.tokensByLineAll().mapIndexed { lineNum, leaves ->
                this.tokenizer.transformToTokens(leaves)
            }
            val lt = lineTokens.map {
                it.toTypedArray()
            }.toTypedArray()
            self.postMessage(MessageLineTokens(lt))
        }
    }

    fun createParseTree(spptNode: SPPTNode): Any {
        return when (spptNode) {
            is SPPTLeaf -> object : Any() {
                val isBranch = false
                val name = spptNode.name
                val nonSkipMatchedText = spptNode.nonSkipMatchedText
            }
            is SPPTBranch -> object : Any() {
                val isBranch = true
                val name = spptNode.name
                val children = spptNode.children.map {
                    createParseTree(it)
                }.toTypedArray()
            }
            else -> error("Not supported")
        }
    }

    fun createAsmTree(asm: Any?): Any? {
        return if (null==asm) {
            null
        } else {
            when (asm) {
                is AsmElementSimple -> {
                    object : Any() {
                        val isAsmElementSimple = true
                        val typeName = asm.typeName
                        val properties = asm.properties.map {
                            object : Any() {
                                val isAsmElementProperty = true
                                val name = it.name
                                val value = createAsmTree(it.value)
                            }
                        }.toTypedArray()
                    }
                }
                is List<*> -> asm.map {
                    createAsmTree(it)
                }.toTypedArray()
                else -> asm.toString()
            }
        }
    }
}