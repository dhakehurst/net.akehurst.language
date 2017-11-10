package net.akehurst.language.core.sppf;

import java.util.Objects;

/**
 *
 * The identity of a node in a Shared Packed Parse Forest is composed from:
 * <ul>
 * <li>a unique rule number,
 * <li>a starting position indicating the index of a position in the input text of the parse at which this node starts,
 * <li>the length (number of characters) of the input text that is matched by this node
 * </ul>
 *
 * If a grammar is ambiguous, a parse result may contain multiple nodes with the same identity but with different children. An SPPF combines these nodes (to
 * avoid duplication) but supports the alternative lists of children.
 *
 */
public class SPPFNodeIdentity implements ISPPFNodeIdentity {

	private final int ruleNumber;
	private final int startPosition;
	private final int matchedLength;
	private final int hashCode_cache;

	public SPPFNodeIdentity(final int ruleNumber, final int startPosition, final int matchedLength) {
		this.ruleNumber = ruleNumber;
		this.startPosition = startPosition;
		this.matchedLength = matchedLength;
		this.hashCode_cache = Objects.hash(ruleNumber, startPosition, matchedLength);
	}

	@Override
	public int getRuntimeRuleNumber() {
		return this.ruleNumber;
	}

	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

	@Override
	public int getMatchedTextLength() {
		return this.matchedLength;
	}

	// --- Object ---

	@Override
	public int hashCode() {
		return this.hashCode_cache;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ISPPFNodeIdentity) {
			final ISPPFNodeIdentity other = (ISPPFNodeIdentity) obj;
			return this.getRuntimeRuleNumber() == other.getRuntimeRuleNumber() && this.getStartPosition() == other.getStartPosition()
					&& this.getMatchedTextLength() == other.getMatchedTextLength();
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append('(');
		b.append(this.getRuntimeRuleNumber());
		b.append(',');
		b.append(this.getStartPosition());
		b.append(',');
		b.append(this.getMatchedTextLength());
		b.append(')');
		return b.toString();
	}
}
