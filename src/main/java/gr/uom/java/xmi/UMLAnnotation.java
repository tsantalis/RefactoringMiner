package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLAnnotation implements Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private String typeName;
	private AbstractExpression value;
	private Map<String, AbstractExpression> memberValuePairs = new LinkedHashMap<>();
	
	public UMLAnnotation(CompilationUnit cu, String filePath, Annotation annotation) {
		this.typeName = annotation.getTypeName().getFullyQualifiedName();
		this.locationInfo = new LocationInfo(cu, filePath, annotation, CodeElementType.ANNOTATION);
		if(annotation instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation)annotation;
			this.value = new AbstractExpression(cu, filePath, singleMemberAnnotation.getValue(), CodeElementType.SINGLE_MEMBER_ANNOTATION_VALUE, null);
		}
		else if(annotation instanceof NormalAnnotation) {
			NormalAnnotation normalAnnotation = (NormalAnnotation)annotation;
			List<MemberValuePair> pairs = normalAnnotation.values();
			for(MemberValuePair pair : pairs) {
				AbstractExpression value = new AbstractExpression(cu, filePath, pair.getValue(), CodeElementType.NORMAL_ANNOTATION_MEMBER_VALUE_PAIR, null);
				memberValuePairs.put(pair.getName().getIdentifier(), value);
			}
		}
	}

	public String getTypeName() {
		return typeName;
	}

	public AbstractExpression getValue() {
		return value;
	}

	public Map<String, AbstractExpression> getMemberValuePairs() {
		return memberValuePairs;
	}

	public boolean isMarkerAnnotation() {
		return value == null && memberValuePairs.isEmpty();
	}
	
	public boolean isSingleMemberAnnotation() {
		return value != null;
	}

 	public boolean isNormalAnnotation() {
 		return memberValuePairs.size() > 0;
 	}
 
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("@").append(typeName);
		if(value != null) {
			sb.append("(");
			sb.append(value.getExpression());
			sb.append(")");
		}
		if(!memberValuePairs.isEmpty()) {
			sb.append("(");
			int i = 0;
			for(String key : memberValuePairs.keySet()) {
				sb.append(key).append(" = ").append(memberValuePairs.get(key).getExpression());
				if(i < memberValuePairs.size() - 1)
					sb.append(", ");
				i++;
			}
			sb.append(")");
		}
		return sb.toString();
	}

	@Override
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	@Override
	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((memberValuePairs == null) ? 0 : memberValuePairsHashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		result = prime * result + ((value == null) ? 0 : value.getExpression().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLAnnotation other = (UMLAnnotation) obj;
		if (memberValuePairs == null) {
			if (other.memberValuePairs != null)
				return false;
		} else if (!this.memberValuePairsEquals(other))
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else {
			if (other.value == null)
				return false;
			if (!value.getExpression().equals(other.value.getExpression()))
				return false;
		}
		return true;
	}

	private boolean memberValuePairsEquals(UMLAnnotation other) {
		Map<String, AbstractExpression> m = other.memberValuePairs;
		int thisSize = this.memberValuePairs.size();
		int otherSize = other.memberValuePairs.size();
		if(thisSize != otherSize) {
			return false;
		}
		for (Map.Entry<String, AbstractExpression> entry : memberValuePairs.entrySet()) {
			String thisKey = entry.getKey();
			AbstractExpression thisValue = entry.getValue();
			if (thisValue == null) {
				if (!(m.get(thisKey) == null && m.containsKey(thisKey)))
					return false;
			} else {
				if (m.get(thisKey) == null)
					return false;
				if (!thisValue.getExpression().equals(m.get(thisKey).getExpression()))
					return false;
			}
		}
		return true;
	}

	private int memberValuePairsHashCode() {
		int h = 0;
		for (Map.Entry<String, AbstractExpression> entry : memberValuePairs.entrySet())
			h += (entry.getKey() == null ? 0 : entry.getKey().hashCode()) ^ (entry.getValue() == null ? 0 : entry.getValue().getExpression().hashCode());
		return h;
	}
}
