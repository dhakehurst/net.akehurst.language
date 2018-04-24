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
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Node2NonTerminal extends AbstractNode2TangibleItem<NonTerminal> {

    @Override
    public String getNodeName() {
        return "nonTerminal";
    }

    @Override
    public boolean isValidForRight2Left(final NonTerminal right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final ISPBranch left, final NonTerminal right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NonTerminal constructLeft2Right(final ISPBranch left, final BinaryTransformer transformer) {

        // final INode ruleOwner = left.getParent();

        final String referencedRuleName = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, left.getChild(0));
        // final Rule owner = transformer.transformLeft2Right(NormalRuleNode2Rule.class, left);
        // final List<Integer> index = new ArrayList<>();
        final NonTerminal right = new NonTerminal(referencedRuleName);// , owner, index);
        return right;

    }

    @Override
    public ISPBranch constructRight2Left(final NonTerminal right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final ISPBranch left, final NonTerminal right, final BinaryTransformer transformer) {

    }

    @Override
    public void updateRight2Left(final ISPBranch left, final NonTerminal right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
