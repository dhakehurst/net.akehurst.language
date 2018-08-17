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

import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor;
import net.akehurst.language.grammar.parser.ToStringVisitor.Indent;

public class ToStringVisitor implements SharedPackedParseTreeVisitor<Set<String>, Indent, RuntimeException> {

    public static class Indent {
        public String text;
        public boolean onlyChild;

        public Indent(final String text) {
            this.text = text;
            this.onlyChild = false;
        }

        public Indent next(final String increment, final boolean onlyChild) {
            final Indent next = new Indent(this.text + increment);
            next.onlyChild = onlyChild;
            return next;
        }
    }

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
    public Set<String> visit(final SharedPackedParseTree target, final Indent indent) throws RuntimeException {
        // String s = indent;
        final SPPTNode root = target.getRoot();
        final Set<String> r = root.accept(this, indent);
        return r;
    }

    @Override
    public Set<String> visit(final SPPTLeaf target, final Indent indent) throws RuntimeException {
        final String s = (indent.onlyChild ? " " : indent.text) + target.getName() + " : \""
                + target.getMatchedText().replace("\n", new String(Character.toChars(0x23CE))) + "\"";
        final Set<String> r = new HashSet<>();
        r.add(s);
        return r;
    }

    @Override
    public Set<String> visit(final SPPTBranch target, final Indent indent) throws RuntimeException {
        final Set<String> r = new HashSet<>();

        for (final FixedList<SPPTNode> children : target.getChildrenAlternatives()) {
            String s = indent.onlyChild ? " " : indent.text;
            s += target.getName();
            s += target.getChildrenAlternatives().size() > 1 ? "*" : "";
            s += " {";
            if (children.isEmpty()) {
                s += "}";
                r.add(s);
            } else if (children.size() == 1) {
                Set<String> currentSet = new HashSet<>();
                currentSet.add(s);
                currentSet = this.visitOnlyChild(currentSet, children, indent);
                for (final String sc : currentSet) {
                    String sc1 = sc;
                    sc1 += "}";
                    r.add(sc1);
                }
            } else {
                s += this.lineSeparator;

                Set<String> currentSet = new HashSet<>();
                currentSet.add(s);
                for (int i = 0; i < children.size(); ++i) {
                    currentSet = this.visitChild(currentSet, children, i, indent);
                }

                for (final String sc : currentSet) {
                    String sc1 = sc;
                    sc1 += indent.text;
                    sc1 += "}";
                    r.add(sc1);
                }
            }
        }
        return r;
    }

    private Set<String> visitOnlyChild(final Set<String> currentSet, final FixedList<SPPTNode> children, final Indent indent) {
        final Set<String> r = new HashSet<>();
        final Set<String> ssc = children.get(0).accept(this, indent.next(this.indentIncrement, true));

        for (final String current : currentSet) {
            for (final String sc : ssc) {
                final StringBuilder b = new StringBuilder(current);
                b.append(sc);
                b.append(" ");
                r.add(b.toString());
            }
        }
        return r;
    }

    private Set<String> visitChild(final Set<String> currentSet, final FixedList<SPPTNode> children, final int index, final Indent indent) {
        final Set<String> r = new HashSet<>();
        final Set<String> ssc = children.get(index).accept(this, indent.next(this.indentIncrement, false));

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
