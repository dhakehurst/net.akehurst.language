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

package net.akehurst.language.objectgraph.api

/**
 * The kind of a [TraceStep].
 */
enum class TraceStepKind {
    /** running a model-to-model transform rule set */
    TRANSFORM,

    /** applying a model-to-model rule (mapping, relation or table) */
    RULE,

    /** evaluating the 'when' clause of a model-to-model rule */
    WHEN,

    /** executing a 'where' call of a model-to-model rule */
    WHERE,

    /** evaluating one expression node */
    EXPRESSION
}

/**
 * One step of an execution (an interpreter run), as given to an [ExecutionTraceListener].
 * Steps nest: a step started after another step started, and before that step finished, is part of it.
 *
 * @param kind what is executed
 * @param element the thing executed, e.g. the Expression or the M2mTransformRule
 * @param self the value of 'self' when the step started, if there is one
 * @param namedValues the named values visible when the step started (inner scopes hide outer values of the same name)
 * @param textProvider gives [text], only computed when it is read
 */
class TraceStep(
    val kind: TraceStepKind,
    val element: Any,
    val self: TypedObject?,
    val namedValues: Map<String, TypedObject>,
    textProvider: () -> String
) {
    /** a description of the step, e.g. "mapping rule 'X'", or the text of the expression */
    val text: String by lazy(textProvider)

    override fun toString(): String = "$kind $text"
}

/**
 * Receives the steps of an execution, e.g. of the expression interpreter or the model-to-model transform interpreter.
 * Given to an interpreter, it is told about every step that interpreter executes; with no listener nothing is traced.
 * The calls for one execution are made in order, from the thread or coroutine running it.
 */
interface ExecutionTraceListener {
    /** [step] starts */
    fun stepStarted(step: TraceStep)

    /** [step] finished, giving [result]; null if the step gives no value or it ended with an exception */
    fun stepFinished(step: TraceStep, result: TypedObject?)

    /** a detail of [step] (null if outside any step), e.g. that a 'when' clause did not hold */
    fun note(step: TraceStep?, message: String)
}

/**
 * Runs [body] as [step] (only created if there is a [listener]), telling the listener when it starts and finishes.
 * If [body] throws, the step is finished with a null result and the exception passes on.
 */
inline fun <T> ExecutionTraceListener?.traceStep(step: () -> TraceStep, resultOf: (T) -> TypedObject?, body: (TraceStep?) -> T): T {
    val listener = this
    return if (null == listener) {
        body(null)
    } else {
        val st = step()
        listener.stepStarted(st)
        var result: TypedObject? = null
        try {
            val r = body(st)
            result = resultOf(r)
            r
        } finally {
            listener.stepFinished(st, result)
        }
    }
}

/** the named values visible in [evc] and its parents, inner values hide outer ones of the same name */
fun EvaluationContext.allVisibleNamedValues(): Map<String, TypedObject> {
    val frames = generateSequence(this) { it.parent }.toList().asReversed()
    val result = mutableMapOf<String, TypedObject>()
    frames.forEach { result.putAll(it.namedValues) }
    return result
}
