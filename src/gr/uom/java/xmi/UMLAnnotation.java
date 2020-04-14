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
			this.value = new AbstractExpression(cu, filePath, singleMemberAnnotation.getValue(), CodeElementType.SINGLE_MEMBER_ANNOTATION_VALUE);
		}
		else if(annotation instanceof NormalAnnotation) {
			NormalAnnotation normalAnnotation = (NormalAnnotation)annotation;
			List<MemberValuePair> pairs = normalAnnotation.values();
			for(MemberValuePair pair : pairs) {
				AbstractExpression value = new AbstractExpression(cu, filePath, pair.getValue(), CodeElementType.NORMAL_ANNOTATION_MEMBER_VALUE_PAIR);
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
			sb.append("(");
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
}
