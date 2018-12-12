/**
 * Copyright (C) 2016 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.util;

import java.util.regex.Pattern;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor;

public class ToJsonVisitor implements SharedPackedParseTreeVisitor<JsonObject, JsonBuilderFactory, RuntimeException> {

	@Override
	public JsonObject visit(final SharedPackedParseTree target, final JsonBuilderFactory arg) throws RuntimeException {
		final SPPTNode root = target.getRoot();
		return root.accept(this, arg);
	}

	// TODO: reverse the 'isPattern' into something like 'canBeUsedAsClassifier' or 'isValidClassifier'

	@Override
	public JsonObject visit(final SPPTLeaf target, final JsonBuilderFactory arg) throws RuntimeException {
		final JsonObjectBuilder builder = arg.createObjectBuilder();
		final String name = target.getName();
		boolean isPattern = false;
		if (Pattern.matches("[a-zA-Z_][a-zA-Z0-9_]*", name) && !name.contains("$")) {
			// ok use name
		} else {
			// if pattern or parser generated node..mark as a pattern
			isPattern = true;
		}
		final String text = target.getMatchedText();
		final String noLines = text.replaceAll(System.lineSeparator(), "");
		final int numEOL = text.length() - noLines.length();
		builder.add("name", target.getName());
		builder.add("start", target.getStartPosition());
		builder.add("length", target.getMatchedTextLength());
		builder.add("isPattern", isPattern);
		builder.add("numEol", numEOL);
		final JsonObject jo = builder.build();
		return jo;
	}

	@Override
	public JsonObject visit(final SPPTBranch target, final JsonBuilderFactory arg) throws RuntimeException {
		final JsonObjectBuilder builder = arg.createObjectBuilder();
		builder.add("name", target.getName());
		builder.add("start", target.getStartPosition());
		builder.add("length", target.getMatchedTextLength());
		final String name = target.getName();
		boolean isPattern = false;
		if (Pattern.matches("[a-zA-Z_][a-zA-Z0-9_]*", name) && !name.contains("$")) {
			// ok use name
		} else {
			// if pattern or parser generated node..mark as a pattern
			isPattern = true;
		}

		builder.add("isPattern", isPattern);
		final JsonArrayBuilder ab = arg.createArrayBuilder();
		for (final SPPTNode n : target.getChildren()) {
			final JsonObject nobj = n.accept(this, arg);
			ab.add(nobj);
		}
		final JsonArray array = ab.build();
		builder.add("children", array);

		final JsonObject jo = builder.build();
		return jo;
	}

}