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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.Terminal;

public abstract class TerminalAbstract extends TangibleItemAbstract implements Terminal {

    public TerminalAbstract(final String value) {
        this.value = value;
    }

    private final String value;

    abstract public Pattern getPattern();

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getName() {
        return this.getValue();
    }

    public boolean matches(final String value) {
        return this.getPattern().matcher(value).matches();
    }

    @Override
    public Set<Terminal> findAllTerminal() {
        final Set<Terminal> result = new HashSet<>();
        result.add(this);
        return result;
    }

    @Override
    public Set<NonTerminal> findAllNonTerminal() {
        final Set<NonTerminal> result = new HashSet<>();
        return result;
    }

}
