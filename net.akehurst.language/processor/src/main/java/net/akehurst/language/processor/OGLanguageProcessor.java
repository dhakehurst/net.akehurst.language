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

import net.akehurst.language.core.analyser.IGrammarLoader;
import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.sppf.ISharedPackedParseTree;
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
		public List<IGrammar> resolve(final String... qualifiedGrammarNames) {
			final List<IGrammar> grammars = new ArrayList<>();

			for (final String qualifiedGrammarName : qualifiedGrammarNames) {
				IGrammar grammar = this.resolved.get(qualifiedGrammarName);
				if (null == grammar) {
					grammar = this.resolve(qualifiedGrammarName);
					this.resolved.put(qualifiedGrammarName, grammar);
				}
				grammars.add(grammar);
			}

			return grammars;
		}

		private IGrammar resolve(final String qualifiedGrammarName) {
			try {
				final String resourcePath = qualifiedGrammarName.replaceAll("::", "/") + ".ogl";
				final InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
				final Reader reader = new InputStreamReader(input);
				// If we use the same Processor/Analyser..then we get errors because parseTree nodes are already maped to the wrong things
				// from analysing a previous grammar.
				final OGLanguageProcessor proc = new OGLanguageProcessor(); // OGLanguageProcessor.this
				final ISharedPackedParseTree forest = proc.getParser().parse("grammarDefinition", reader);
				final IGrammar grammar = proc.getSemanticAnalyser().analyse(IGrammar.class, forest);
				return grammar;
			} catch (final Exception e) {
				throw new RuntimeException("Unable to resolve grammar " + qualifiedGrammarName, e);
			}
		}

	}
}
