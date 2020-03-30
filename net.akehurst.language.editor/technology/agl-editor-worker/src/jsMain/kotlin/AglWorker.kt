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
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.editor.common.*
import net.akehurst.language.processor.Agl
import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.dom.MessagePort
import org.w3c.dom.SharedWorkerGlobalScope

external val self: DedicatedWorkerGlobalScope

class AglWorker {

    class AglWorkerComponents(
            val languageId: String,
            val processor: LanguageProcessor
    ) {
        var nextCssClassNum = 1
        val cssClassPrefix = "agl-${languageId}-"
        val tokenToClassMap = mutableMapOf<String, String>()

        private fun mapTokenTypeToClass(tokenType: String): String? {
            var cssClass = this.tokenToClassMap.get(tokenType)
            return cssClass
        }

        private fun mapToCssClasses(leaf: SPPTLeaf): List<String> {
            val metaTagClasses = leaf.metaTags.mapNotNull { this.mapTokenTypeToClass(it) }
            val otherClasses = if (!leaf.tagList.isEmpty()) {
                leaf.tagList.mapNotNull { this.mapTokenTypeToClass(it) }
            } else {
                listOf(this.mapTokenTypeToClass(leaf.name)).mapNotNull { it }
            }
            val classes = metaTagClasses + otherClasses
            return if (classes.isEmpty()) {
                listOf("nostyle")
            } else {
                classes.toSet().toList()
            }
        }

        fun transformToTokens(leafs: List<SPPTLeaf>): List<AglToken> {
            return leafs.map { leaf ->
                val cssClasses = this.mapToCssClasses(leaf)
                var beforeEOL = leaf.matchedText
                val eolIndex = leaf.matchedText.indexOf('\n');
                if (-1 !== eolIndex) {
                    beforeEOL = leaf.matchedText.substring(0, eolIndex);
                }
                AglToken(
                        cssClasses.toSet().toTypedArray(),
                        beforeEOL,
                        leaf.location.line, //ace first line is 0
                        leaf.location.column
                )
            }
        }
    }

    val processors = mutableMapOf<String, AglWorkerComponents>()

    init {
        start()
    }

    fun start() {
        self.onmessage = {
            val msg: dynamic = it.data
            when (msg.action) {
                "MessageProcessorCreate" -> this.createProcessor(self, msg.languageId, msg.editorId, msg.grammarStr)
                "MessageParserInterruptRequest" -> this.interrupt(self, msg.languageId, msg.editorId, msg.reason)
                "MessageParseRequest" -> this.parse(self, msg.languageId, msg.editorId, msg.text)
                "MessageSetStyle" -> this.setStyle(self, msg.languageId, msg.editorId, msg.css)
            }
        }
    }

    fun startShared() {
        (self as SharedWorkerGlobalScope).onconnect = { e ->
            val port = e.asDynamic().ports[0] as MessagePort
            port.onmessage = {
                val msg: dynamic = it.data
                when (msg.action) {
                    "MessageProcessorCreate" -> this.createProcessor(port, msg.languageId, msg.editorId, msg.grammarStr)
                    "MessageParserInterruptRequest" -> this.interrupt(port, msg.languageId, msg.editorId, msg.reason)
                    "MessageParseRequest" -> this.parse(port, msg.languageId, msg.editorId, msg.text)
                    "MessageSetStyle" -> this.setStyle(port, msg.languageId, msg.editorId, msg.css)
                }
            }
            true //onconnect insists on having a return value!
        }
    }

    fun createProcessor(port: dynamic, languageId: String, editorId: String, grammarStr: String?) {
        if (null == grammarStr) {
            this.processors.remove(languageId)
        } else {

            //cheet because I don't want to serialise grammars
            when (grammarStr) {
                "@Agl.grammarProcessor@" -> this.processors[languageId] = AglWorkerComponents(languageId, Agl.grammarProcessor)
                "@Agl.styleProcessor@" -> this.processors[languageId] = AglWorkerComponents(languageId, Agl.styleProcessor)
                "@Agl.formatProcessor@" -> this.processors[languageId] = AglWorkerComponents(languageId, Agl.formatProcessor)
                else -> this.processors[languageId] = AglWorkerComponents(languageId, Agl.processor(grammarStr))
            }
        }
    }

    fun interrupt(port: dynamic, languageId: String, editorId: String, reason: String) {
        val proc = this.processors[languageId]?.processor ?: throw RuntimeException("Processor with languageId $languageId not found")
        proc.interrupt(reason)
    }

    fun setStyle(port: dynamic, languageId: String, editorId: String, css: String) {
        val cmps = this.processors[languageId] ?: throw RuntimeException("Processor with languageId $languageId not found")
        val rules: List<AglStyleRule> = Agl.styleProcessor.process(css)
        rules.forEach { rule ->
            var cssClass = cmps.tokenToClassMap.get(rule.selector)
            if (null == cssClass) {
                cssClass = cmps.cssClassPrefix + cmps.nextCssClassNum++
                cmps.tokenToClassMap.set(rule.selector, cssClass);
            }
        }
    }

    fun parse(port: dynamic, languageId: String, editorId: String, sentence: String) {
        try {
            val proc = this.processors[languageId]?.processor ?: throw RuntimeException("Processor with languageId $languageId not found")
            if (null == proc) {
                //do nothing
            } else {
                val sppt = proc.parse(sentence)
                val tree = createParseTree(sppt.root)
                self.postMessage(MessageParseSuccess(languageId, editorId, tree))
                this.sendParseLineTokens(port, languageId, editorId, sppt)
                this.process(port, languageId, editorId, sppt)
            }
        } catch (e: ParseFailedException) {
            val sppt = e.longestMatch
            val tree = createParseTree(sppt!!.root)
            port.postMessage(MessageParseFailure(languageId, editorId, e.message!!, e.location, tree))
        } catch (t: Throwable) {
            port.postMessage(MessageParseFailure(languageId, editorId, t.message!!, null, null))
        }
    }

    fun process(port: dynamic, languageId: String, editorId: String, sppt: SharedPackedParseTree) {
        try {
            val proc = this.processors[languageId]?.processor ?: throw RuntimeException("Processor with languageId $languageId not found")
            val asm = proc.process<Any>(sppt)
            val asmTree = createAsmTree(asm) ?: "No Asm"
            port.postMessage(MessageProcessSuccess(languageId, editorId, asmTree))
        } catch (t: Throwable) {
            port.postMessage(MessageProcessFailure(languageId, editorId, t.message!!))
        }
    }

    fun sendParseLineTokens(port: dynamic, languageId: String, editorId: String, sppt: SharedPackedParseTree) {
        if (null == sppt) {
            //nothing
        } else {
            val cmps = this.processors[languageId] ?: throw RuntimeException("Processor with languageId $languageId not found")
            val lineTokens = sppt.tokensByLineAll().mapIndexed { lineNum, leaves ->
                cmps.transformToTokens(leaves)
            }
            val lt = lineTokens.map {
                it.toTypedArray()
            }.toTypedArray()
            port.postMessage(MessageLineTokens(languageId, editorId, lt))
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
        return if (null == asm) {
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