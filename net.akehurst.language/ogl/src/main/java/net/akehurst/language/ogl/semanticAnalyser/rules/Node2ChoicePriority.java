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

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.sppt.SPPTBranch;
import net.akehurst.language.core.sppt.SPPTNode;
import net.akehurst.language.ogl.semanticStructure.ChoicePriority;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Node2ChoicePriority extends AbstractNode2Choice<ChoicePriority> {

    @Override
    public String getNodeName() {
        return "priorityChoice";
    }

    @Override
    public boolean isValidForRight2Left(final ChoicePriority right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final SPPTBranch left, final ChoicePriority right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ChoicePriority constructLeft2Right(final SPPTBranch left, final BinaryTransformer transformer) {

        final List<? extends SPPTNode> allLeft = left.getNonSkipChildren();
        List<? extends Concatenation> allRight;

        final List<SPPTNode> concatenationNodes = new ArrayList<>();
        for (final SPPTNode n : allLeft) {
            if ("concatenation".equals(n.getName())) {
                concatenationNodes.add(n);
            }
        }

        allRight = transformer.transformAllLeft2Right((Class<BinaryRule<SPPTNode, Concatenation>>) (Class<?>) Node2Concatenation.class, concatenationNodes);

        final ChoicePriority right = new ChoicePriority(allRight.toArray(new Concatenation[0]));
        return right;

    }

    @Override
    public SPPTBranch constructRight2Left(final ChoicePriority right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final SPPTBranch left, final ChoicePriority right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRight2Left(final SPPTBranch left, final ChoicePriority right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
