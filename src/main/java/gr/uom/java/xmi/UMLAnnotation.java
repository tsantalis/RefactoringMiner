package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.ValueArgument;

import extension.ast.node.LangASTNode;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.unit.LangCompilationUnit;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLAnnotation implements Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private String typeName;
	private AbstractExpression value;
	private Map<String, AbstractExpression> memberValuePairs = new LinkedHashMap<>();
	private List<AbstractExpression> arguments = new ArrayList<>();
	
	public UMLAnnotation(CompilationUnit cu, String sourceFolder, String filePath, Annotation annotation, String javaFileContent) {
		this.typeName = annotation.getTypeName().getFullyQualifiedName();
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, annotation, CodeElementType.ANNOTATION);
		if(annotation instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation)annotation;
			this.value = new AbstractExpression(cu, sourceFolder, filePath, singleMemberAnnotation.getValue(), CodeElementType.SINGLE_MEMBER_ANNOTATION_VALUE, null, new LinkedHashMap<>(), javaFileContent);
		}
		else if(annotation instanceof NormalAnnotation) {
			NormalAnnotation normalAnnotation = (NormalAnnotation)annotation;
			List<MemberValuePair> pairs = normalAnnotation.values();
			for(MemberValuePair pair : pairs) {
				AbstractExpression value = new AbstractExpression(cu, sourceFolder, filePath, pair.getValue(), CodeElementType.NORMAL_ANNOTATION_MEMBER_VALUE_PAIR, null, new LinkedHashMap<>(), javaFileContent);
				memberValuePairs.put(pair.getName().getIdentifier(), value);
			}
		}
	}

	public UMLAnnotation(KtFile ktFile, String sourceFolder, String filePath, KtAnnotationEntry annotationEntry, String fileContent) {
		this.typeName = annotationEntry.getShortName().asString();
		this.locationInfo = new LocationInfo(ktFile, sourceFolder, filePath, annotationEntry, CodeElementType.ANNOTATION);
		for(ValueArgument argument : annotationEntry.getValueArguments()) {
			AbstractExpression expression = new AbstractExpression(ktFile, sourceFolder, filePath, (KtValueArgument)argument, CodeElementType.SINGLE_MEMBER_ANNOTATION_VALUE, null, new LinkedHashMap<>(), fileContent);
			this.arguments.add(expression);
		}
	}

	public UMLAnnotation(LangCompilationUnit cu, String sourceFolder, String filePath, LangAnnotation annotation, String fileContent) {
		this.typeName = annotation.getName().getIdentifier();
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, annotation, CodeElementType.ANNOTATION);

		// Handle single-member annotations (e.g., @decorator(value))
		if (annotation.isSingleMemberAnnotation()) {
			this.value = new AbstractExpression(cu, sourceFolder, filePath,
					annotation.getValue(), CodeElementType.SINGLE_MEMBER_ANNOTATION_VALUE, null, new LinkedHashMap<>(), fileContent);
		}
		// Handle normal annotations with named parameters (e.g., @dataclass(frozen=True, order=False))
		else if (annotation.isNormalAnnotation()) {
			Map<String, LangASTNode> langPairs = annotation.getMemberValuePairs();
			for (Map.Entry<String, LangASTNode> entry : langPairs.entrySet()) {
				AbstractExpression value = new AbstractExpression(cu, sourceFolder, filePath,
						entry.getValue(), CodeElementType.NORMAL_ANNOTATION_MEMBER_VALUE_PAIR, null, new LinkedHashMap<>(), fileContent);
				memberValuePairs.put(entry.getKey(), value);
			}
		}
		// Handle positional arguments (e.g., @lru_cache(128))
		else if (!annotation.getArguments().isEmpty()) {
			// For now, treat the first argument as the primary value
			// This matches common Python decorator patterns like @lru_cache(128)
			if (annotation.getArguments().size() == 1) {
				this.value = new AbstractExpression(cu, sourceFolder, filePath,
						annotation.getArguments().get(0), CodeElementType.SINGLE_MEMBER_ANNOTATION_VALUE, null, new LinkedHashMap<>(), fileContent);
			}
			// For multiple positional arguments, we could store them as indexed pairs
			// e.g., "0" -> first_arg, "1" -> second_arg, etc.
			else {
				for (int i = 0; i < annotation.getArguments().size(); i++) {
					LangASTNode node = annotation.getArguments().get(i);
					AbstractExpression value = new AbstractExpression(cu, sourceFolder, filePath,
							node, CodeElementType.NORMAL_ANNOTATION_MEMBER_VALUE_PAIR, null, new LinkedHashMap<>(), fileContent);
					arguments.add(value);
				}
			}
		}
		// Marker annotation - no additional processing needed
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

	public List<AbstractExpression> getArguments() {
		return arguments;
	}

	public boolean isMarkerAnnotation() {
		return value == null && memberValuePairs.isEmpty() && arguments.isEmpty();
	}
	
	public boolean isSingleMemberAnnotation() {
		return value != null;
	}

 	public boolean isNormalAnnotation() {
 		return memberValuePairs.size() > 0 || arguments.size() > 0;
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
		if(!arguments.isEmpty()) {
			sb.append("(");
			int i = 0;
			for(AbstractExpression argument : arguments) {
				sb.append(argument.getExpression().replaceAll("\n\\s+", " "));
				if(i < arguments.size() - 1)
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
		result = prime * result + ((arguments == null) ? 0 : argumentHashCode());
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
		boolean equalArguments = this.argumentEquals(other);
		if (memberValuePairs == null) {
			if (other.memberValuePairs != null)
				return false;
		} else if (!this.memberValuePairsEquals(other))
			return false;
		if (arguments == null) {
			if (other.arguments != null)
				return false;
		} else if (!equalArguments)
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		if (value == null) {
			if (other.value != null && !equalArguments)
				return false;
		} else {
			if (other.value == null && !equalArguments)
				return false;
			if (!equalArguments && !value.getExpression().equals(other.value.getExpression()))
				return false;
		}
		return true;
	}

	public boolean argumentEquals(UMLAnnotation other) {
		int thisSize = this.arguments.size();
		int otherSize = other.arguments.size();
		if(thisSize != otherSize) {
			if(this.arguments.size() == 1 && other.arguments.size() == 0 && other.value != null) {
				if(this.arguments.get(0).getString().equals(other.value.getString()))
					return true;
			}
			else if(other.arguments.size() == 1 && this.arguments.size() == 0 && this.value != null) {
				if(other.arguments.get(0).getString().equals(this.value.getString()))
					return true;
			}
			return false;
		}
		for(int i=0; i<this.arguments.size(); i++) {
			AbstractExpression thisValue = this.arguments.get(i);
			AbstractExpression otherValue = other.arguments.get(i);
			if(!thisValue.getExpression().equals(otherValue.getExpression())) {
				return false;
			}
		}
		return true;
	}

	private int argumentHashCode() {
		int hashCode = 1;
		for (AbstractExpression e : arguments) {
		    hashCode = 31 * hashCode + (e == null ? 0 : e.getString().hashCode());
		}
		return hashCode;
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
