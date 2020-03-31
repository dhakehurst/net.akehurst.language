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

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.editor.common.*
import org.w3c.dom.*

class AglWorkerClient(
        val workerScriptName: String
) {

    lateinit var worker: Worker
    var setStyleResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    var processorCreateSuccess: (message: String) -> Unit = { _ -> }
    var processorCreateFailure: (message: String) -> Unit = { _ -> }
    var parseSuccess: (tree: Any) -> Unit = { _ -> }
    var parseFailure: (message: String, location: InputLocation?, tree: Any?) -> Unit = { _, _, _ -> }
    var lineTokens: (Array<Array<AglToken>>) -> Unit = { _ -> }
    var processSuccess: (tree: Any) -> Unit = { _ -> }
    var processFailure: (message: String) -> Unit = { _ -> }

    fun initialise() {
        // currently can't make SharedWorker work
        //this.worker = SharedWorker(workerScriptName, options=WorkerOptions(type = WorkerType.MODULE))
        this.worker = Worker(workerScriptName, options = WorkerOptions(type = WorkerType.MODULE))
        this.worker.onerror = {
            console.error(it)
        }
        worker.onmessage = {
            val msg = it.data.asDynamic()
            when (msg.action) {
                "MessageSetStyleResult" -> this.setStyleResult(msg.success, msg.message)
                "MessageProcessorCreateSuccess" -> this.processorCreateSuccess(msg.message)
                "MessageProcessorCreateFailure" -> this.processorCreateFailure(msg.message)
                "MessageParseSuccess" -> this.parseSuccess(msg.tree)
                "MessageParseFailure" -> this.parseFailure(msg.message, msg.location, msg.tree)
                "MessageLineTokens" -> this.lineTokens(msg.lineTokens)
                "MessageProcessSuccess" -> this.processSuccess(msg.asm)
                "MessageProcessFailure" -> this.processFailure(msg.message)
                else -> error("Unknown Message type")
            }
        }
        //worker.port.onmessageerror
        //worker.port.start()
    }

    fun sendToWorker(msg: Any, transferables: Array<dynamic> = emptyArray()) {
        this.worker.postMessage(msg, transferables)
    }

    fun createProcessor(languageId: String, editorId: String, grammarStr: String?) {
        this.sendToWorker(MessageProcessorCreate(languageId, editorId, grammarStr))
    }

    fun interrupt(languageId: String, editorId: String) {
        this.sendToWorker(MessageParserInterruptRequest(languageId, editorId, "New parse request"))
    }

    fun tryParse(languageId: String, editorId: String, sentence: String) {
        this.sendToWorker(MessageParseRequest(languageId, editorId, sentence))
    }

    fun setStyle(languageId: String, editorId: String, css: String) {
        this.sendToWorker(MessageSetStyle(languageId, editorId, css))
    }

}