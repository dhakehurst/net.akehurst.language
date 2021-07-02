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

package net.akehurst.language.api.grammar

interface GrammarVisitor<R, A> {

    fun visitNamespace(target: Namespace, arg: A): R
    fun visitGrammar(target: Grammar, arg: A): R
    fun visitRule(target: Rule, arg: A): R
    fun visitChoicePriority(target: ChoicePriority, arg: A): R
    fun visitChoiceEqual(target: ChoiceEqual, arg: A): R
    fun visitChoiceAmbiguous(target: ChoiceAmbiguous, arg: A): R
    fun visitConcatenation(target: Concatenation, arg: A): R
    fun visitGroup(target: Group, arg: A): R
    fun visitMulti(target: Multi, arg: A): R
    fun visitSeparatedList(target: SeparatedList, arg: A): R
    fun visitNonTerminal(target: NonTerminal, arg: A): R
    fun visitTerminal(target: Terminal, arg: A): R
    fun visitEmptyRule(target: EmptyRule, arg: A): R

}
