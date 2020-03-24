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

package net.akehurst.language.editor.comon

import net.akehurst.language.editor.common.*
import org.w3c.dom.MODULE
import org.w3c.dom.Worker
import org.w3c.dom.WorkerOptions
import org.w3c.dom.WorkerType

class AglWorkerClient {

    lateinit var worker: Worker
    var processorCreateSuccess: () -> Unit = {}
    var processorCreateFailure: () -> Unit = {}
    var parseSuccess: () -> Unit = {}
    var parseFailure: (e:Throwable) -> Unit = {}
    var lineTokens: (Array<Array<AglToken>>)->Unit = {  }

    fun initialise() {
        this.worker = Worker("./agl-worker.js", WorkerOptions(type = WorkerType.MODULE))
        worker.onmessage = {
            val msg = it.data.asDynamic()
            when (msg.action) {
                "MessageProcessorCreateSuccess" -> this.processorCreateSuccess()
                "MessageProcessorCreateFailure" -> this.processorCreateFailure()
                "MessageParseResponseSuccess" -> this.parseSuccess()
                "MessageParseResponseFailure" -> this.parseFailure(msg.exception)
                "MessageLineTokens" -> this.lineTokens(msg.lineTokens)
                else -> error("Unknown Message type")
            }
        }
    }

    fun sendToWorker(msg: Any, transferables: Array<dynamic> = emptyArray()) {
        this.worker.postMessage(msg, transferables)
    }

    fun createProcessor(grammarStr: String?) {
        this.sendToWorker(MessageProcessorCreate(grammarStr))
    }

    fun interrupt() {
        this.sendToWorker(MessageParserInterruptRequest("New parse request"))
    }

    fun tryParse(sentence: String) {
        this.sendToWorker(MessageParseRequest(sentence))
    }

}