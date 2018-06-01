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
import net.akehurst.language.core.sppt.SPPTNode;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Node2SeparatedList extends AbstractNode2ConcatenationItem<SeparatedList> {

    @Override
    public String getNodeName() {
        return "separatedList";
    }

    @Override
    public boolean isValidForRight2Left(final SeparatedList right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final SPPTBranch left, final SeparatedList right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public SeparatedList constructLeft2Right(final SPPTBranch left, final BinaryTransformer transformer) {

        final SPPTNode itemNode = left.getChild(1);

        final TangibleItem item = transformer.transformLeft2Right((Class<BinaryRule<SPPTNode, TangibleItem>>) (Class<?>) Node2SimpleItem.class, itemNode);

        final SPPTNode separatorNode = left.getChild(3);

        final TerminalLiteral separator = transformer.transformLeft2Right((Class<BinaryRule<SPPTNode, TerminalLiteral>>) (Class<?>) AbstractNode2Terminal.class,
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

        final SeparatedList right = new SeparatedList(min, max, separator, item);
        return right;

    }

    @Override
    public SPPTBranch constructRight2Left(final SeparatedList right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final SPPTBranch left, final SeparatedList right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRight2Left(final SPPTBranch left, final SeparatedList right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
