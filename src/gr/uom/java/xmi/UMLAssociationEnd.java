package gr.uom.java.xmi;

public class UMLAssociationEnd {
	private String participant;
	private String name;
	private boolean isNavigable;
	private String aggregation;
	private int multiplicityLower;
	private int multiplicityUpper;
	
	public UMLAssociationEnd(String participant) {
		this.participant = participant;
	}

	public String getParticipant() {
		return participant;
	}

	public void setParticipant(String participant) {
		this.participant = participant;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isNavigable() {
		return isNavigable;
	}

	public void setNavigable(boolean isNavigable) {
		this.isNavigable = isNavigable;
	}

	public String getAggregation() {
		return aggregation;
	}

	public void setAggregation(String aggregation) {
		this.aggregation = aggregation;
	}

	public int getMultiplicityLower() {
		return multiplicityLower;
	}

	public void setMultiplicityLower(int multiplicityLower) {
		this.multiplicityLower = multiplicityLower;
	}

	public int getMultiplicityUpper() {
		return multiplicityUpper;
	}

	public void setMultiplicityUpper(int multiplicityUpper) {
		this.multiplicityUpper = multiplicityUpper;
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLAssociationEnd) {
    		UMLAssociationEnd umlAssociationEnd = (UMLAssociationEnd)o;
    		return this.participant.equals(umlAssociationEnd.participant);
    	}
    	return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(aggregation.equals("aggregate") || aggregation.equals("composite"))
			sb.append("<>");
		else if(multiplicityLower == 1 && multiplicityUpper == 1)
			sb.append("(1)");
		else if(multiplicityLower == 0 && multiplicityUpper == 1)
			sb.append("(0..1)");
		else if(multiplicityLower == 1 && multiplicityUpper == -1)
			sb.append("(1..*)");
		else if(multiplicityLower == 0 && multiplicityUpper == -1)
			sb.append("(0..*)");
		return sb.toString();
	}
}
