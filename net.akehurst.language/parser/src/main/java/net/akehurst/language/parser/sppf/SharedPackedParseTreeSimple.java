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
package net.akehurst.language.parser.sppf;

import java.util.Set;

import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor;
import net.akehurst.language.grammar.parser.ParseTreeToInputText;
import net.akehurst.language.grammar.parser.ToStringVisitor;

public class SharedPackedParseTreeSimple implements SharedPackedParseTree {

    private final SPPTNode root;

    public SharedPackedParseTreeSimple(final SPPTNode root) {
        this.root = root;
    }

    // --- IParseTree ---
    @Override
    public String asString() {
        final ParseTreeToInputText visitor = new ParseTreeToInputText();
        final String s = this.accept(visitor, "");
        return s;
    }

    // --- ISharedPackedParseForest ---
    @Override
    public SPPTNode getRoot() {
        return this.root;
    }

    @Override
    public boolean contains(final SharedPackedParseTree other) {
        final boolean result = this.getRoot().contains(other.getRoot());
        return result;
    }

    @Override
    public String toStringAll() {
        final ToStringVisitor v = new ToStringVisitor();
        final Set<String> all = this.accept(v, new ToStringVisitor.Indent(""));
        final StringBuilder b = new StringBuilder();
        final int total = all.size();
        int cur = 0;
        for (final String pt : all) {
            cur++;
            b.append("Tree " + cur + " of " + total + System.lineSeparator());
            b.append(pt);
            b.append(System.lineSeparator());
        }
        return b.toString();
    }

    // --- IParseTreeVisitable ---
    @Override
    public <T, A, E extends Throwable> T accept(final SharedPackedParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
        return visitor.visit(this, arg);
    }

    // --- Object ---
    @Override
    public String toString() {
        final ToStringVisitor v = new ToStringVisitor();
        return this.accept(v, new ToStringVisitor.Indent("")).iterator().next();
    }

    @Override
    public int hashCode() {
        return this.getRoot().hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg instanceof SharedPackedParseTree) {
            final SharedPackedParseTree other = (SharedPackedParseTree) arg;
            // return Objects.equals(this.getRoot(), other.getRoot());
            // TODO: not the fastest way to do this, but will do for now
            return this.contains(other) && other.contains(this);
        } else {
            return false;
        }
    }
}
