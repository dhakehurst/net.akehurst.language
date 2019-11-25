/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.ogl.semanticStructure;

import java.util.List;
import java.util.Set;

import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.Terminal;

public abstract class RuleItemAbstract implements GramarVisitable, RuleItem {

    RuleDefault owningRule;

    @Override
    public RuleDefault getOwningRule() {
        return this.owningRule;
    }

    public abstract void setOwningRule(RuleDefault value, List<Integer> index);

    public abstract List<Integer> getIndex();

    @Override
    public abstract Set<Terminal> findAllTerminal();

    @Override
    public abstract Set<NonTerminal> findAllNonTerminal();

}
