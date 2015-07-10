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
package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.RuleItem;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;

abstract
public class AbstractRuleItem2RuntimeRuleItem<L extends RuleItem> implements Relation<L, RuntimeRuleItem> {

	RuntimeRuleItemKind getRuleItemKind(RuleItem item) {
		if (item instanceof ChoiceSimple) {
			return RuntimeRuleItemKind.CHOICE;
		} else if (item instanceof Concatenation) {
			return RuntimeRuleItemKind.CONCATENATION;
		} else if (item instanceof Multi) {
			return RuntimeRuleItemKind.MULTI;
		} else if (item instanceof SeparatedList) {
			return RuntimeRuleItemKind.SEPARATED_LIST;			
		} else {
			throw new RuntimeException("Internal Error, type of RuleItem not recognised");
		}
	}
	
}
