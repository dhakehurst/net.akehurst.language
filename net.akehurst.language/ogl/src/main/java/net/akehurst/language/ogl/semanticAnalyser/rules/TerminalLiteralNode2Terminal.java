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
package net.akehurst.language.ogl.semanticAnalyser.rules;

import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPLeaf;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class TerminalLiteralNode2Terminal extends AbstractNode2Terminal<TerminalLiteral> {

    @Override
    public String getNodeName() {
        return "LITERAL";
    }

    @Override
    public boolean isValidForRight2Left(final TerminalLiteral right) {
        return true;
    }

    @Override
    public boolean isAMatch(final ISPBranch left, final TerminalLiteral right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public TerminalLiteral constructLeft2Right(final ISPBranch left, final BinaryTransformer transformer) {
        final ISPNode child = left.getChildren().get(0);
        final ISPLeaf leaf = (ISPLeaf) child;
        final String text = leaf.getMatchedText();
        final String literal = text.substring(1, text.length() - 1);
        final String unescapedLiteral = TerminalLiteralNode2Terminal.unescapeJava(literal);
        final TerminalLiteral right = new TerminalLiteral(unescapedLiteral);
        return right;
    }

    @Override
    public ISPBranch constructRight2Left(final TerminalLiteral left, final BinaryTransformer right) {
        return null;
    }

    @Override
    public void updateLeft2Right(final ISPBranch left, final TerminalLiteral right, final BinaryTransformer arg2) {

    }

    @Override
    public void updateRight2Left(final ISPBranch left, final TerminalLiteral right, final BinaryTransformer arg2) {}

    /**
     * Adapted from io.vertx.core.impl.StringEscapeUtils which is itself adapted from Apache Commons code
     */
    private static String unescapeJava(final String str) {
        final StringBuilder out = new StringBuilder(str.length());
        if (str == null) {
            return null;
        }
        final int sz = str.length();
        final StringBuilder unicode = new StringBuilder();
        boolean hadSlash = false;
        boolean inUnicode = false;
        for (int i = 0; i < sz; i++) {
            final char ch = str.charAt(i);
            if (inUnicode) {
                // if in unicode, then we're reading unicode
                // values in somehow
                unicode.append(ch);
                if (unicode.length() == 4) {
                    // unicode now contains the four hex digits
                    // which represents our unicode character
                    final int value = Integer.parseInt(unicode.toString(), 16);
                    out.append((char) value);
                    unicode.setLength(0);
                    inUnicode = false;
                    hadSlash = false;
                }
                continue;
            }
            if (hadSlash) {
                // handle an escaped value
                hadSlash = false;
                switch (ch) {
                    case '\\':
                        out.append('\\');
                    break;
                    case '\'':
                        out.append('\'');
                    break;
                    case '\"':
                        out.append('"');
                    break;
                    case 'r':
                        out.append('\r');
                    break;
                    case 'f':
                        out.append('\f');
                    break;
                    case 't':
                        out.append('\t');
                    break;
                    case 'n':
                        out.append('\n');
                    break;
                    case 'b':
                        out.append('\b');
                    break;
                    case 'u': {
                        // uh-oh, we're in unicode country....
                        inUnicode = true;
                        break;
                    }
                    default:
                        out.append(ch);
                    break;
                }
                continue;
            } else if (ch == '\\') {
                hadSlash = true;
                continue;
            }
            out.append(ch);
        }
        if (hadSlash) {
            // then we're in the weird case of a \ at the end of the
            // string, let's output it anyway.
            out.append('\\');
        }
        return out.toString();
    }
}
