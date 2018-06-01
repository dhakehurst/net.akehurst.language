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

import java.util.List;

import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.ogl.semanticStructure.ConcatenationDefault;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItemAbstract;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Node2Concatenation extends AbstractSemanticAnalysisRule<ConcatenationDefault> {

    @Override
    public String getNodeName() {
        return "concatenation";
    }

    @Override
    public boolean isValidForRight2Left(final ConcatenationDefault right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final SPPTBranch left, final ConcatenationDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ConcatenationDefault constructLeft2Right(final SPPTBranch left, final BinaryTransformer transformer) {

        final List<? extends SPPTNode> allLeft = left.getNonSkipChildren();
        List<? extends ConcatenationItemAbstract> allRight;

        allRight = transformer.transformAllLeft2Right((Class<BinaryRule<SPPTNode, ConcatenationItemAbstract>>) (Class<?>) Node2ConcatenationItem.class, allLeft);

        final ConcatenationDefault right = new ConcatenationDefault(allRight.toArray(new ConcatenationItemAbstract[allRight.size()]));
        return right;

    }

    @Override
    public SPPTBranch constructRight2Left(final ConcatenationDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final SPPTBranch left, final ConcatenationDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRight2Left(final SPPTBranch left, final ConcatenationDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
