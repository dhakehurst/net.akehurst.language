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
package net.akehurst.language.grammar.parser.converter.rules;

import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimpleDefault;
import net.akehurst.language.ogl.semanticStructure.ConcatenationDefault;
import net.akehurst.language.ogl.semanticStructure.MultiDefault;
import net.akehurst.language.ogl.semanticStructure.RuleItemAbstract;
import net.akehurst.language.ogl.semanticStructure.SeparatedListDefault;
import net.akehurst.transform.binary.api.BinaryRule;

abstract public class AbstractRuleItem2RuntimeRuleItem<L extends RuleItemAbstract> implements BinaryRule<L, RuntimeRuleItem> {

    RuntimeRuleItemKind getRuleItemKind(final RuleItemAbstract item) {
        if (item instanceof ChoiceSimpleDefault) {
            return RuntimeRuleItemKind.CHOICE;
        } else if (item instanceof ConcatenationDefault) {
            return RuntimeRuleItemKind.CONCATENATION;
        } else if (item instanceof MultiDefault) {
            return RuntimeRuleItemKind.MULTI;
        } else if (item instanceof SeparatedListDefault) {
            return RuntimeRuleItemKind.SEPARATED_LIST;
        } else {
            throw new RuntimeException("Internal Error, type of RuleItem not recognised");
        }
    }

}
