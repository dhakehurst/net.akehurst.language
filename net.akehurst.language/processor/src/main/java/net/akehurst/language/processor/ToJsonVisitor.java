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
package net.akehurst.language.processor;

import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;

public class ToJsonVisitor implements IParseTreeVisitor<JsonObject, JsonObjectBuilder, RuntimeException> {

	@Override
	public JsonObject visit(final IParseTree target, final JsonObjectBuilder arg) throws RuntimeException {
		return target.getRoot().accept(this, arg);
	}

	// TODO: reverse the 'isPattern' into something like 'canBeUsedAsClassifier' or 'isValidClassifier'

	@Override
	public JsonObject visit(final ILeaf target, final JsonObjectBuilder arg) throws RuntimeException {
		final JsonObjectBuilder builder = arg;
		final String name = target.getName();
		boolean isPattern = false;
		if (Pattern.matches("[a-zA-Z_][a-zA-Z0-9_]*", name) && !name.contains("$")) {
			// ok use name
		} else {
			// if pattern or parser generated node..mark as a pattern
			isPattern = true;
		}

		builder.add("name", target.getName());
		builder.add("start", target.getStartPosition());
		builder.add("length", target.getMatchedTextLength());
		builder.add("isPattern", isPattern);
		return builder.build();
	}

	@Override
	public JsonObject visit(final IBranch target, final JsonObjectBuilder arg) throws RuntimeException {
		final JsonObjectBuilder builder = arg;
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
		final JsonArrayBuilder ab = Json.createArrayBuilder();
		for (final INode n : target.getChildren()) {
			final JsonObject nobj = n.accept(this, arg);
			ab.add(nobj);
		}
		final JsonArray array = ab.build();
		builder.add("children", array);

		return builder.build();
	}

}
