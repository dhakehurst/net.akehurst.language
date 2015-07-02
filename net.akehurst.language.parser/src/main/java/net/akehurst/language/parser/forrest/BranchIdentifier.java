package net.akehurst.language.parser.forrest;

public class BranchIdentifier {

	public BranchIdentifier(int ruleNumber, int start, int end) {
		this.ruleNumber = ruleNumber;
		this.start = start;
		this.end = end;
		this.hashCode_cache = ruleNumber ^ start ^ end;
	}
	int ruleNumber;
	int start;
	int end;
	
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof BranchIdentifier)) {
			return false;
		}
		BranchIdentifier other = (BranchIdentifier)arg;
		
		return this.start == other.start
			&& this.end == other.end
			&& this.ruleNumber==other.ruleNumber;
	}
	
	int hashCode_cache;
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "("+this.ruleNumber+","+this.start+","+this.end+")";
	}
	
}
