package net.akehurst.language.ogl.semanticModel;

public class Namespace {

	public Namespace(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}
	
	String qualifiedName;
	public String getQualifiedName() {
		return this.qualifiedName;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getQualifiedName();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Namespace) {
			Namespace other = (Namespace)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
