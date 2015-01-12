package net.akehurst.language.ogl.semanticModel;

public interface Visitable {

	<T,E extends Throwable> T accept(Visitor<T,E> visitor, Object... arg) throws E;
	
}
