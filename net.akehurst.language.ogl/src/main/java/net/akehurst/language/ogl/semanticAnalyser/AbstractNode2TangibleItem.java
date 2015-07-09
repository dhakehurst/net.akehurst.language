package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.transform.binary.Relation;

abstract
public class AbstractNode2TangibleItem<R extends TangibleItem> implements Relation<INode, R> {

}
