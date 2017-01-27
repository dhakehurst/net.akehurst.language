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
package net.akehurst.language.processor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.language.core.analyser.IGrammar;
import net.akehurst.language.core.analyser.IGrammarLoader;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticAnalyser.SemanicAnalyser;

public class OGLanguageProcessor extends LanguageProcessor {

	public OGLanguageProcessor() {
		super(new OGLGrammar(), new SemanicAnalyser());
		this.getSemanticAnalyser().setGrammarLoader(new Loader());
	}

	class Loader implements IGrammarLoader {

		public Loader() {
			this.resolved = new HashMap<>();
		}

		Map<String, IGrammar> resolved;

		@Override
		public List<IGrammar> resolve(String... qualifiedGrammarNames) {
			List<IGrammar> grammars = new ArrayList<>();

			for (String qualifiedGrammarName : qualifiedGrammarNames) {
				IGrammar grammar = this.resolved.get(qualifiedGrammarName);
				if (null == grammar) {
					grammar = resolve(qualifiedGrammarName);
					this.resolved.put(qualifiedGrammarName, grammar);
				}
				grammars.add(grammar);
			}

			return grammars;
		}

		private IGrammar resolve(String qualifiedGrammarName) {
			try {
				String resourcePath = qualifiedGrammarName.replaceAll("::", "/") + ".ogl";
				InputStream input = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
				Reader reader = new InputStreamReader(input);
				IParseTree tree = getParser().parse("grammarDefinition", reader);
				IGrammar grammar = getSemanticAnalyser().analyse(IGrammar.class, tree);
				return grammar;
			} catch (Exception e) {
				throw new RuntimeException("Unable to resolve grammar "+qualifiedGrammarName,e);
			}
		}

	}
}
