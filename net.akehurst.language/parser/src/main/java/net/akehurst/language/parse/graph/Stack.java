package net.akehurst.language.parse.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Stack {

	public Stack() {
		this.stack = new HashMap<>();
	}

	public static final class GrowingNodeIndex {
		public GrowingNodeIndex(final int ruleNumber, final int startPosition, final int endPosition, final int nextItemIndex) {// , final boolean hasStack) {//
																																// ,
																																// final
			// int[]
			// stack) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.nextItemIndex = nextItemIndex;
			// this.hasStack = hasStack;
			// this.stackHash = stack;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition, endPosition, nextItemIndex);// , hasStack);// , Arrays.hashCode(stack));
		}

		private final int ruleNumber;
		private final int startPosition;
		private final int endPosition;
		private final int nextItemIndex;
		// private final boolean hasStack;
		// private final int[] stackHash;

		private final int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(final Object arg) {
			if (!(arg instanceof GrowingNodeIndex)) {
				return false;
			}
			final GrowingNodeIndex other = (GrowingNodeIndex) arg;
			return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition && this.endPosition == other.endPosition
					&& this.nextItemIndex == other.nextItemIndex;// && this.hasStack == other.hasStack;// && Arrays.equals(this.stackHash, other.stackHash);
		}

		@Override
		public String toString() {
			final StringBuilder b = new StringBuilder();
			b.append('(');
			b.append(this.ruleNumber);
			b.append(',');
			b.append(this.startPosition);
			b.append(',');
			b.append(this.endPosition);
			b.append(',');
			b.append(this.nextItemIndex);
			// b.append(',');
			// b.append(this.hasStack);
			// b.append(',');
			// b.append(this.stackHash);
			b.append(')');
			return b.toString();
		}
	}

	Map<GrowingNodeIndex, IGrowingNode> stack;

}
