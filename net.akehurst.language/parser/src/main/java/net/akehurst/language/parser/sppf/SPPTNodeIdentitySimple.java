package net.akehurst.language.parser.sppf;

import java.util.Objects;

import net.akehurst.language.core.sppt.SPNodeIdentity;

public class SPPTNodeIdentitySimple implements SPNodeIdentity {

    private final int ruleNumber;
    private final int startPosition;
    private final int matchedLength;
    private final int hashCode_cache;

    public SPPTNodeIdentitySimple(final int ruleNumber, final int startPosition, final int matchedLength) {
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
        if (obj instanceof SPNodeIdentity) {
            final SPNodeIdentity other = (SPNodeIdentity) obj;
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
