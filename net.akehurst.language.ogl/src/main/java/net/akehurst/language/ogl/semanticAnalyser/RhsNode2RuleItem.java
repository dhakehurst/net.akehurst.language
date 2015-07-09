package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.transform.binary.Relation;

abstract
public class RhsNode2RuleItem<R extends RuleItem> implements Relation<INode, R>{

}
