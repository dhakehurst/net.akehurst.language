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

    var processor: LanguageProcessor? = null
    var styleHandler: AglStyleHandler? = null

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
            this.processor = null
            port.postMessage(MessageProcessorCreateSuccess(languageId, editorId, "reset"))
        } else {
            try {
                //cheet because I don't want to serialise grammars
                when (grammarStr) {
                    "@Agl.grammarProcessor@" -> createAgl(languageId, Agl.grammarProcessor)
                    "@Agl.styleProcessor@" -> createAgl(languageId, Agl.styleProcessor)
                    "@Agl.formatProcessor@" -> createAgl(languageId, Agl.formatProcessor)
                    else -> createAgl(languageId, Agl.processor(grammarStr))
                }
                port.postMessage(MessageProcessorCreateSuccess(languageId, editorId, "OK"))
            } catch (t:Throwable) {
                port.postMessage(MessageProcessorCreateFailure(languageId, editorId, t.message!!))
            }
        }
    }

    fun createAgl(langId: String, proc: LanguageProcessor) {
        this.processor = proc
    }

    fun interrupt(port: dynamic, languageId: String, editorId: String, reason: String) {
        val proc = this.processor
        if (proc != null) {
            proc.interrupt(reason)
        }
    }

    fun setStyle(port: dynamic, languageId: String, editorId: String, css: String) {
        try {
            val style = AglStyleHandler(languageId)
            this.styleHandler = style
            val rules: List<AglStyleRule> = Agl.styleProcessor.process(css)
            rules.forEach { rule ->
                style.mapClass(rule.selector)
            }
            port.postMessage(MessageSetStyleResult(languageId, editorId, true, "OK"))
        } catch (t:Throwable) {
            port.postMessage(MessageSetStyleResult(languageId, editorId, false, t.message!!))
        }
    }

    fun parse(port: dynamic, languageId: String, editorId: String, sentence: String) {
        try {
            val proc = this.processor ?: throw RuntimeException("Processor for $languageId not found")
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
            val proc = this.processor ?: throw RuntimeException("Processor for $languageId not found")
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
            val style = this.styleHandler ?: throw RuntimeException("StyleHandler for $languageId not found")
            val lineTokens = sppt.tokensByLineAll().mapIndexed { lineNum, leaves ->
                style.transformToTokens(leaves)
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