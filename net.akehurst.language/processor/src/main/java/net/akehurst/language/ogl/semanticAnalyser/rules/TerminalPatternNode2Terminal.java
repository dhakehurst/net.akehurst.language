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

import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class TerminalPatternNode2Terminal extends AbstractNode2Terminal<TerminalPatternDefault> {

    @Override
    public String getNodeName() {
        return "PATTERN";
    }

    @Override
    public boolean isValidForRight2Left(final TerminalPatternDefault right) {
        return true;
    }

    @Override
    public boolean isAMatch(final SPPTBranch left, final TerminalPatternDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public TerminalPatternDefault constructLeft2Right(final SPPTBranch left, final BinaryTransformer transformer) {
        final SPPTNode child = left.getChildren().get(0);
        final SPPTLeaf leaf = (SPPTLeaf) child;
        final String text = leaf.getMatchedText();
        final String pattern = text.substring(1, text.length() - 1);
        final TerminalPatternDefault right = new TerminalPatternDefault(pattern);
        return right;
    }

    @Override
    public SPPTBranch constructRight2Left(final TerminalPatternDefault arg0, final BinaryTransformer arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final SPPTBranch left, final TerminalPatternDefault right, final BinaryTransformer arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRight2Left(final SPPTBranch left, final TerminalPatternDefault right, final BinaryTransformer arg2) {
        // TODO Auto-generated method stub

    }

}
