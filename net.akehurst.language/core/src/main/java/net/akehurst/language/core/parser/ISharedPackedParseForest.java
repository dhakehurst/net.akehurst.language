package net.akehurst.language.core.parser;

import java.util.Set;

public interface ISharedPackedParseForest {

	Set<INode> getRoots();

	boolean contains(IParseTree tree);

}
