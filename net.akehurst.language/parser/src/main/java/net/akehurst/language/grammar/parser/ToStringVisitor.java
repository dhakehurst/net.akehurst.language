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
package net.akehurst.language.grammar.parser;

import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.core.sppt.FixedList;
import net.akehurst.language.core.sppt.IParseTreeVisitor;
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPLeaf;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;

public class ToStringVisitor implements IParseTreeVisitor<Set<String>, String, RuntimeException> {

    public ToStringVisitor() {
        this(System.lineSeparator(), "  ");
    }

    public ToStringVisitor(final String lineSeparator, final String indentIncrement) {
        this.lineSeparator = lineSeparator;
        this.indentIncrement = indentIncrement;
    }

    private final String lineSeparator;
    private final String indentIncrement;

    @Override
    public Set<String> visit(final ISharedPackedParseTree target, final String indent) throws RuntimeException {
        // String s = indent;
        final ISPNode root = target.getRoot();
        final Set<String> r = root.accept(this, indent);
        return r;
    }

    @Override
    public Set<String> visit(final ISPLeaf target, final String indent) throws RuntimeException {
        final String s = indent + target.getName() + " : \"" + target.getMatchedText().replace("\n", new String(Character.toChars(0x23CE))) + "\"";
        final Set<String> r = new HashSet<>();
        r.add(s);
        return r;
    }

    @Override
    public Set<String> visit(final ISPBranch target, final String indent) throws RuntimeException {
        final Set<String> r = new HashSet<>();

        for (final FixedList<ISPNode> children : target.getChildrenAlternatives()) {
            String s = indent;
            s += target.getName();
            s += target.getChildrenAlternatives().size() > 1 ? "*" : "";
            s += " {";
            if (children.isEmpty()) {
                s += "}";
                r.add(s);
            } else {
                s += this.lineSeparator;

                Set<String> currentSet = new HashSet<>();
                currentSet.add(s);
                for (int i = 0; i < children.size(); ++i) {
                    currentSet = this.visitChild(currentSet, children, i, indent);
                }

                for (final String sc : currentSet) {
                    String sc1 = sc;
                    sc1 += indent;
                    sc1 += "}";
                    r.add(sc1);
                }
            }
        }
        return r;
    }

    private Set<String> visitChild(final Set<String> currentSet, final FixedList<ISPNode> children, final int index, final String indent) {
        final Set<String> r = new HashSet<>();
        final Set<String> ssc = children.get(index).accept(this, indent + this.indentIncrement);

        for (final String current : currentSet) {
            for (final String sc : ssc) {
                final StringBuilder b = new StringBuilder(current);
                b.append(sc);
                b.append(this.lineSeparator);
                r.add(b.toString());
            }
        }
        return r;
    }

}
