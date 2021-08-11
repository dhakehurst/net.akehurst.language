/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.ast

import net.akehurst.language.api.grammar.*

internal abstract class GrammarVisitorAbstract<R, A> : GrammarVisitor<R, A> {

    protected fun visitConcatenationItem(target: ConcatenationItem, arg: A): R = when (target) {
        is Multi -> this.visitMulti(target, arg)
        is SeparatedList -> this.visitSeparatedList(target, arg)
        is SimpleItem -> this.visitSimpleItem(target, arg)
        else -> error("${target::class} is not a supported subtype of ConcatenationItem")
    }

    protected fun visitChoice(target: Choice, arg: A): R = when (target) {
        is ChoiceEqual -> this.visitChoiceEqual(target, arg)
        is ChoicePriority -> this.visitChoicePriority(target, arg)
        is ChoiceAmbiguous -> this.visitChoiceAmbiguous(target, arg)
        else -> error("${target::class} is not a supported subtype of Choice")
    }

    protected fun visitSimpleItem(target: SimpleItem, arg: A): R = when (target) {
        is Group -> this.visitGroup(target, arg)
        is TangibleItem -> this.visitTangibleItem(target, arg)
        else -> error("${target::class} is not a supported subtype of SimpleItem")
    }

    protected fun visitTangibleItem(target: TangibleItem, arg: A): R = when (target) {
        is EmptyRule -> this.visitEmptyRule(target, arg)
        is NonTerminal -> this.visitNonTerminal(target, arg)
        is Terminal -> this.visitTerminal(target, arg)
        else -> error("${target::class} is not a supported subtype of TangibleItem")
    }
}