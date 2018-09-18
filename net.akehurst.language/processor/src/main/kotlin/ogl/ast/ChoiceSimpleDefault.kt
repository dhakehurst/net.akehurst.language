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

package net.akehurst.language.ogl.ast


import net.akehurst.language.api.grammar.ChoiceSimple
import net.akehurst.language.api.grammar.Concatenation
import net.akehurst.language.api.grammar.GrammarVisitor

class ChoiceSimpleDefault(override val alternative: List<Concatenation>) : ChoiceAbstract(alternative), ChoiceSimple {

    // --- GrammarVisitable ---

    override fun <T,A> accept(visitor: GrammarVisitor<T, A>, arg: A): T {
        return visitor.visit(this, arg);
    }

}
