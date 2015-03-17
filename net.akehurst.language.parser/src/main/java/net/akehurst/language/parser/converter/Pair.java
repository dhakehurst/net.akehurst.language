package net.akehurst.language.parser.converter;

public class Pair<F,S> {

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}
	private F first;
	public F getFirst() {
		return first;
	}
	private S second;
	public S getSecond() {
		return second;
	}
}
