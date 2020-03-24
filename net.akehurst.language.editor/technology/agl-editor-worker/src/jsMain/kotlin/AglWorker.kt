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

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.editor.common.*
import net.akehurst.language.processor.Agl
import org.w3c.dom.DedicatedWorkerGlobalScope

external val self: DedicatedWorkerGlobalScope

class AglWorker {

    val agl = AglComponents()
    val tokenizer = AglTokenizer(this.agl)

    fun start() {
        self.onmessage = {
            val msg: dynamic = it.data
            when (msg.action) {
                "MessageProcessorCreate" -> this.createProcessor(msg.grammarStr)
                "MessageParserInterruptRequest" -> this.interrupt(msg.reason)
                "MessageParseRequest" -> this.parse(msg.text)
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

    fun parse(sentence: String) {
        try {
            this.agl.sppt = null
            val proc = this.agl.processor
            if (null == proc) {
                //do nothing
            } else {
                this.agl.sppt = proc.parse(sentence)
                self.postMessage(MessageParseResponseSuccess())
                this.sendParseLineTokens()
            }
        } catch (e: ParseFailedException) {
            this.agl.sppt = e.longestMatch
            self.postMessage(MessageParseResponseFailure(e))
            this.sendScanLineTokens(sentence)
        } catch (t: Throwable) {
            self.postMessage(MessageParseResponseFailure(t))
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

    fun sendParseLineTokens() {
        val sppt = this.agl.sppt
        if (null==sppt) {
            //nothing
        } else {
            val lineTokens =  sppt.tokensByLineAll().mapIndexed { lineNum, leaves ->
                this.tokenizer.transformToTokens(leaves)
            }
            val lt = lineTokens.map {
                it.toTypedArray()
            }.toTypedArray()
            self.postMessage(MessageLineTokens(lt))
        }
    }
}