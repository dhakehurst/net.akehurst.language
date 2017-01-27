package net.akehurst.language.core.analyser;

import java.util.List;

public interface IGrammarLoader {

	List<IGrammar> resolve(String... qualifiedGrammarNames);

}
