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
package net.akehurst.language.grammar.parser.forrest;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.grammar.INodeType;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.parser.sppf.EmptyLeaf;
import net.akehurst.language.parser.sppf.IInput;
import net.akehurst.language.parser.sppf.Leaf;

public class InputForReader implements IInput {

    public InputForReader(final RuntimeRuleSetBuilder ffactory, final Reader reader) {
        this.ffactory = ffactory;
        this.NO_LEAF = this.ffactory.createLeaf(null, -1, -1, null);
        this.reader = reader;
        this.leaf_cache = new HashMap<>();
        // this.bud_cache = new HashMap<>();
        this.alreadyRead = new StringBuilder();
    }

    private final RuntimeRuleSetBuilder ffactory;
    private final Reader reader;
    private final StringBuilder alreadyRead;

    private CharSequence getTextTo(final int pos) {
        // TODO:
        if (pos > this.alreadyRead.length()) {
            try (final Scanner s = new Scanner(this.reader);) {
                // s.match()
                return "";
            }
        } else {
            return this.alreadyRead.subSequence(0, pos);
        }
    }

    @Override
    public CharSequence getText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getIsStart(final int pos) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getIsEnd(final int pos) {
        // TODO:
        return true;// pos >= this.text.length();
    }

    final Leaf NO_LEAF;

    class IntPair {
        public IntPair(final int nodeType, final int position) {
            this.nodeType = nodeType;
            this.position = position;
        }

        int nodeType;
        int position;

        @Override
        public int hashCode() {
            return this.nodeType ^ this.position;
        }

        @Override
        public boolean equals(final Object arg) {
            if (!(arg instanceof IntPair)) {
                return false;
            }
            final IntPair other = (IntPair) arg;
            return this.nodeType == other.nodeType && this.position == other.position;
        }

        @Override
        public String toString() {
            return "(".concat(Integer.toString(this.nodeType)).concat(",").concat(Integer.toString(this.position)).concat(")");
        }
    }

    Map<IntPair, Leaf> leaf_cache;

    @Override
    public Leaf fetchOrCreateBud(final RuntimeRule terminalRule, final int pos) {
        // TODO:
        final int terminalTypeNumber = terminalRule.getRuleNumber();
        final IntPair key = new IntPair(terminalTypeNumber, pos);
        final Leaf l = this.leaf_cache.get(key);
        if (null == l) {
            if (terminalRule.getIsEmptyRule()) {
                return new EmptyLeaf(pos, terminalRule);
            }
            final Matcher m = Pattern.compile(terminalRule.getTerminalPatternText(), terminalRule.getPatternFlags()).matcher(this.alreadyRead);
            m.region(pos, 0);
            if (m.lookingAt()) {
                final String matchedText = m.group();
                final int start = m.start();
                final int end = m.end();
                final Leaf leaf = this.ffactory.createLeaf(matchedText, start, end, terminalRule);
                this.leaf_cache.put(key, leaf);
                return leaf;
            } else {
                this.leaf_cache.put(key, null);
                return null;
            }
        } else {
            return l;
        }
    }

    int nextI;
    Map<INodeType, Integer> map;

    private int getNodeType(final INodeType nodeType) {
        Integer i = this.map.get(nodeType);
        if (null == i) {
            i = this.nextI++;
            this.map.put(nodeType, i);
        }
        return i;
    }
}
