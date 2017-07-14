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
package net.akehurst.language.ogl.semanticStructure;

import java.util.regex.Pattern;

import net.akehurst.language.core.lexicalAnalyser.ITokenType;

public class TokenType implements ITokenType {
	public TokenType(String identity, String pattern, boolean isRegex) {
		this.pattern = Pattern.compile(pattern, isRegex ? Pattern.MULTILINE : Pattern.LITERAL);
		this.identity = identity;
		this.isRegEx = isRegex;
	}

	boolean isRegEx;

	@Override
	public boolean getIsRegEx() {
		return this.isRegEx;
	}

	@Override
	public String getPatternString() {
		return this.getPattern().pattern();
	}

	Pattern pattern;

	@Override
	public Pattern getPattern() {
		return pattern;
	}

	String identity;

	@Override
	public String getIdentity() {
		return identity;
	}

	@Override
	public boolean equals(Object arg) {
		if (arg instanceof ITokenType) {
			ITokenType other = (ITokenType) arg;
			return this.getPattern().equals(other.getPattern());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.getPattern().hashCode();
	}
}