package gr.uom.java.xmi.diff;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.decomposition.AbstractExpression;

public class UMLAnnotationDiff {
	private UMLAnnotation removedAnnotation;
	private UMLAnnotation addedAnnotation;
	private boolean typeNameChanged = false;
	private boolean valueChanged = false;
	private boolean valueRemoved = false;
	private boolean valueAdded = false;
	private List<SimpleEntry<String, AbstractExpression>> removedMemberValuePairs;
	private List<SimpleEntry<String, AbstractExpression>> addedMemberValuePairs;
	private Map<SimpleEntry<String, AbstractExpression>, SimpleEntry<String, AbstractExpression>> matchedMemberValuePairsWithDifferentExpressions;
	
	public UMLAnnotationDiff(UMLAnnotation removedAnnotation, UMLAnnotation addedAnnotation) {
		this.removedAnnotation = removedAnnotation;
		this.addedAnnotation = addedAnnotation;
		this.removedMemberValuePairs = new ArrayList<SimpleEntry<String,AbstractExpression>>();
		this.addedMemberValuePairs = new ArrayList<SimpleEntry<String,AbstractExpression>>();
		this.matchedMemberValuePairsWithDifferentExpressions = new LinkedHashMap<SimpleEntry<String,AbstractExpression>, SimpleEntry<String,AbstractExpression>>();
		Map<SimpleEntry<String,AbstractExpression>, SimpleEntry<String,AbstractExpression>> matchedMemberValuePairs =
				new LinkedHashMap<SimpleEntry<String,AbstractExpression>, SimpleEntry<String,AbstractExpression>>();
		if(!removedAnnotation.getTypeName().equals(addedAnnotation.getTypeName())) {
			typeNameChanged = true;
		}
		AbstractExpression value1 = removedAnnotation.getValue();
		AbstractExpression value2 = addedAnnotation.getValue();
		if(value1 != null && value2 != null) {
			if(!value1.getExpression().equals(value2.getExpression())) {
				valueChanged = true;
			}
		}
		else if(value1 != null && value2 == null) {
			valueRemoved = true;
		}
		else if(value1 == null && value2 != null) {
			valueAdded  = true;
		}
		Map<String, AbstractExpression> memberValuePairs1 = removedAnnotation.getMemberValuePairs();
		Map<String, AbstractExpression> memberValuePairs2 = addedAnnotation.getMemberValuePairs();
		if(!memberValuePairs1.isEmpty() || !memberValuePairs2.isEmpty()) {
			for(String key1 : memberValuePairs1.keySet()) {
				if(memberValuePairs2.containsKey(key1)) {
					matchedMemberValuePairs.put(new SimpleEntry<>(key1, memberValuePairs1.get(key1)), new SimpleEntry<>(key1, memberValuePairs2.get(key1)));
				}
				else {
					removedMemberValuePairs.add(new SimpleEntry<>(key1, memberValuePairs1.get(key1)));
				}
			}
			for(String key2 : memberValuePairs2.keySet()) {
				if(memberValuePairs1.containsKey(key2)) {
					matchedMemberValuePairs.put(new SimpleEntry<>(key2, memberValuePairs1.get(key2)), new SimpleEntry<>(key2, memberValuePairs2.get(key2)));
				}
				else {
					addedMemberValuePairs.add(new SimpleEntry<>(key2, memberValuePairs2.get(key2)));
				}
			}
		}
		for(SimpleEntry<String, AbstractExpression> key : matchedMemberValuePairs.keySet()) {
			SimpleEntry<String, AbstractExpression> value = matchedMemberValuePairs.get(key);
			if(!key.getValue().getExpression().equals(value.getValue().getExpression())) {
				matchedMemberValuePairsWithDifferentExpressions.put(key, value);
			}
		}
	}
	
	public UMLAnnotation getRemovedAnnotation() {
		return removedAnnotation;
	}

	public UMLAnnotation getAddedAnnotation() {
		return addedAnnotation;
	}

	public boolean isEmpty() {
		return !typeNameChanged && !valueChanged && !valueAdded && !valueRemoved &&
				removedMemberValuePairs.isEmpty() && addedMemberValuePairs.isEmpty() &&
				matchedMemberValuePairsWithDifferentExpressions.isEmpty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(addedAnnotation, removedAnnotation);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLAnnotationDiff other = (UMLAnnotationDiff) obj;
		return Objects.equals(addedAnnotation, other.addedAnnotation)
				&& Objects.equals(removedAnnotation, other.removedAnnotation);
	}

	@Override
	public String toString() {
		return removedAnnotation + "\t->\t" + addedAnnotation;
	}
}
