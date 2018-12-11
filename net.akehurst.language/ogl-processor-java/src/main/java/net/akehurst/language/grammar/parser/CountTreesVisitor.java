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
package net.akehurst.language.grammar.parser;

import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor;

public class CountTreesVisitor implements SharedPackedParseTreeVisitor<Integer, Void, RuntimeException> {

	public CountTreesVisitor() {}

	@Override
	public Integer visit(final SharedPackedParseTree target, final Void v) throws RuntimeException {
		final SPPTNode root = target.getRoot();
		final Integer c = root.accept(this, null);
		return c;
	}

	@Override
	public Integer visit(final SPPTLeaf target, final Void v) throws RuntimeException {
		return 1;
	}

	@Override
	public Integer visit(final SPPTBranch target, final Void v) throws RuntimeException {
		Integer currentCount = 0;
		for (final FixedList<SPPTNode> children : target.getChildrenAlternatives()) {
			if (children.isEmpty()) {
				currentCount += 1;
			} else {
				int max = 0;
				for (int i = 0; i < children.size(); ++i) {
					max = Math.max(max, children.get(i).accept(this, null));
				}
				currentCount += max;
			}
		}
		return currentCount;
	}

}
