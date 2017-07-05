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
package net.akehurst.language.ogl.semanticAnalyser;

import java.util.Objects;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;

abstract public class AbstractSemanticAnalysisRule<R> implements IBinaryRule<IBranch, R> {

	abstract public String getNodeName();

	@Override
	public boolean isValidForLeft2Right(final IBranch left) {
		return this.getNodeName().equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(final R right) {
		return true;
	}

	@Override
	public boolean isAMatch(final IBranch left, final R right, final ITransformer transformer) throws RuleNotFoundException {
		return Objects.equals(left.getName(), this.getNodeName());
	}
}
