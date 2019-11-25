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

interface GrammarVisitor<T, A> {

    fun visit(target: Namespace, arg: A): T
    fun visit(target: Grammar, arg: A): T
    fun visit(target: Rule, arg: A): T
    fun visit(target: ChoicePriority, arg: A): T
    fun visit(target: ChoiceEqual, arg: A): T
    fun visit(target: ChoiceAmbiguous, arg: A): T
    fun visit(target: Concatenation, arg: A): T
    fun visit(target: Group, arg: A): T
    fun visit(target: Multi, arg: A): T
    fun visit(target: SeparatedList, arg: A): T
    fun visit(target: NonTerminal, arg: A): T
    fun visit(target: Terminal, arg: A): T
    fun visit(target: EmptyRule, arg: A): T

}
