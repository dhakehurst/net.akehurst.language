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
import net.akehurst.language.ogl.semanticStructure.RuleDefault;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class AnyRuleNode2Rule extends AbstractSemanticAnalysisRule<RuleDefault> {

    @Override
    public String getNodeName() {
        return "anyRule";
    }

    @Override
    public RuleDefault constructLeft2Right(final SPPTBranch left, final BinaryTransformer transformer) {
        // final INode rule = left.getChild(1);
        final SPPTNode rule = left.getChild(0);
        final RuleDefault right = transformer.transformLeft2Right(NormalRuleNode2Rule.class, rule);
        return right;
    }

    @Override
    public SPPTBranch constructRight2Left(final RuleDefault right, final BinaryTransformer transformer) {
        return null;
    }

    @Override
    public void updateLeft2Right(final SPPTBranch left, final RuleDefault right, final BinaryTransformer transformer) {}

    @Override
    public void updateRight2Left(final SPPTBranch left, final RuleDefault right, final BinaryTransformer transformer) {}

}
