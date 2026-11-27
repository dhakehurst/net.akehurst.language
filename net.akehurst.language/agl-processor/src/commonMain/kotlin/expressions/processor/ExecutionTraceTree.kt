/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.expressions.processor

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.objectgraph.api.ExecutionTraceListener
import net.akehurst.language.objectgraph.api.TraceStep
import net.akehurst.language.objectgraph.api.TraceStepKind
import net.akehurst.language.objectgraph.api.TypedObject

/**
 * An [ExecutionTraceListener] that builds the trace as a tree of [Node]s, one per step, nested as the steps nest.
 *
 * @param kinds only steps of these kinds are recorded; the steps inside a step that is not recorded become part of the nearest recorded step
 */
class ExecutionTraceTree(
    val kinds: Set<TraceStepKind> = TraceStepKind.entries.toSet()
) : ExecutionTraceListener {

    class Node(val step: TraceStep) {
        val children = mutableListOf<Node>()
        val notes = mutableListOf<String>()
        var finished = false
            internal set
        var result: TypedObject? = null
            internal set

        fun asString(indent: Indent = Indent()): String {
            val sb = StringBuilder()
            sb.append("${indent}${step.kind} ${step.text}")
            if (finished) result?.let { sb.append(" = $it") }
            sb.append("\n")
            val ci = indent.inc
            notes.forEach { sb.append("${ci}note: $it\n") }
            children.forEach { sb.append(it.asString(ci)) }
            return sb.toString()
        }

        override fun toString(): String = asString()
    }

    private val _roots = mutableListOf<Node>()
    private val _open = mutableListOf<Node?>() // null for a step that is not recorded
    private val _notesOutsideSteps = mutableListOf<String>()

    /** the steps that are not part of another (recorded) step */
    val roots: List<Node> get() = _roots

    /** notes given outside any recorded step */
    val notes: List<String> get() = _notesOutsideSteps

    private val currentNode: Node? get() = _open.lastOrNull { null != it }

    override fun stepStarted(step: TraceStep) {
        if (step.kind in kinds) {
            val node = Node(step)
            val parent = currentNode
            if (null == parent) _roots.add(node) else parent.children.add(node)
            _open.add(node)
        } else {
            _open.add(null)
        }
    }

    override fun stepFinished(step: TraceStep, result: TypedObject?) {
        // steps are finished in the reverse order they started, so the step finishing is the last one open
        if (_open.isEmpty()) return
        val node = _open.removeAt(_open.size - 1)
        node?.let {
            it.result = result
            it.finished = true
        }
    }

    override fun note(step: TraceStep?, message: String) {
        val node = when {
            null == step -> currentNode
            else -> _open.lastOrNull { null != it && it.step === step } ?: currentNode
        }
        if (null == node) _notesOutsideSteps.add(message) else node.notes.add(message)
    }

    fun clear() {
        _roots.clear()
        _open.clear()
        _notesOutsideSteps.clear()
    }

    fun asString(): String {
        val sb = StringBuilder()
        _notesOutsideSteps.forEach { sb.append("note: $it\n") }
        _roots.forEach { sb.append(it.asString()) }
        return sb.toString()
    }

    override fun toString(): String = asString()
}
