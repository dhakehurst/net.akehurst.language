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
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.ogl.semanticStructure.SeparatedListDefault;
import net.akehurst.language.ogl.semanticStructure.TangibleItemAbstract;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Node2SeparatedList extends AbstractNode2ConcatenationItem<SeparatedListDefault> {

    @Override
    public String getNodeName() {
        return "separatedList";
    }

    @Override
    public boolean isValidForRight2Left(final SeparatedListDefault right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final SPPTBranch left, final SeparatedListDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public SeparatedListDefault constructLeft2Right(final SPPTBranch left, final BinaryTransformer transformer) {

        final SPPTNode itemNode = left.getChild(1);

        final TangibleItemAbstract item = transformer.transformLeft2Right((Class<BinaryRule<SPPTNode, TangibleItemAbstract>>) (Class<?>) Node2SimpleItem.class, itemNode);

        final SPPTNode separatorNode = left.getChild(3);

        final TerminalLiteralDefault separator = transformer.transformLeft2Right((Class<BinaryRule<SPPTNode, TerminalLiteralDefault>>) (Class<?>) AbstractNode2Terminal.class,
                separatorNode);

        final SPPTNode multiplicityNode = left.getChild(5);
        // TODO: this should really be done with transform rules!
        final String multiplicityString = ((SPPTBranch) multiplicityNode).getChild(0).getName();
        int min = -1;
        int max = -1;
        if ("*".equals(multiplicityString)) {
            min = 0;
            max = -1;
        } else if ("+".equals(multiplicityString)) {
            min = 1;
            max = -1;
        } else if ("?".equals(multiplicityString)) {
            min = 0;
            max = 1;
        }

        final SeparatedListDefault right = new SeparatedListDefault(min, max, separator, item);
        return right;

    }

    @Override
    public SPPTBranch constructRight2Left(final SeparatedListDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final SPPTBranch left, final SeparatedListDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRight2Left(final SPPTBranch left, final SeparatedListDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
