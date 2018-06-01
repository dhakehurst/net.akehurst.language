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
import net.akehurst.language.ogl.semanticStructure.ChoiceAbstract;
import net.akehurst.language.ogl.semanticStructure.GroupDefault;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Node2Group extends AbstractNode2TangibleItem<GroupDefault> {

    @Override
    public String getNodeName() {
        return "group";
    }

    @Override
    public boolean isValidForRight2Left(final GroupDefault right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final SPPTBranch left, final GroupDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public GroupDefault constructLeft2Right(final SPPTBranch left, final BinaryTransformer transformer) {

        final SPPTNode choiceNode = left.getChild(1);
        final ChoiceAbstract choice = transformer.transformLeft2Right((Class<BinaryRule<SPPTNode, ChoiceAbstract>>) (Class<?>) AbstractNode2Choice.class,
                ((SPPTBranch) choiceNode).getChild(0));
        final GroupDefault right = new GroupDefault(choice);
        return right;

    }

    @Override
    public SPPTBranch constructRight2Left(final GroupDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final SPPTBranch left, final GroupDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRight2Left(final SPPTBranch left, final GroupDefault right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
