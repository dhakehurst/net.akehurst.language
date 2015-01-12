package net.akehurst.language.core.analyser;

import net.akehurst.language.core.parser.IParseTree;

public interface ISemanticAnalyser {

	<T> T analyse(Class<T> targetType, IParseTree tree) throws UnableToAnalyseExeception;
}
