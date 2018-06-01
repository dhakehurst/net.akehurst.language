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

import net.akehurst.language.core.sppt.SPPTBranch;
import net.akehurst.language.core.sppt.SPPTLeaf;
import net.akehurst.language.core.sppt.SPPTNode;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class IDENTIFIERBranch2String implements BinaryRule<SPPTNode, String> {

    @Override
    public boolean isAMatch(final SPPTNode left, final String right, final BinaryTransformer transformer) {
        return true;
    }

    @Override
    public void updateLeft2Right(final SPPTNode arg0, final String arg1, final BinaryTransformer arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRight2Left(final SPPTNode arg0, final String arg1, final BinaryTransformer arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public String constructLeft2Right(final SPPTNode left, final BinaryTransformer transformer) {
        final SPPTLeaf leaf = (SPPTLeaf) ((SPPTBranch) left).getChildren().get(0);
        final String right = leaf.getMatchedText();
        return right;
    }

    @Override
    public SPPTBranch constructRight2Left(final String arg0, final BinaryTransformer arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidForLeft2Right(final SPPTNode left) {
        return left.getName().equals("IDENTIFIER");
    }

    @Override
    public boolean isValidForRight2Left(final String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}
