package net.akehurst.language.parser.forrest;

public class Input {

	public Input(CharSequence text) {
		this.text = text;
	}
	CharSequence text;
	
	public CharSequence get(int start, int end) {
		return this.text.subSequence(start, end);
	}

}
