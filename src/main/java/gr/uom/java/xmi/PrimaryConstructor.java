package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.util.PathFileUtils;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;

public class PrimaryConstructor implements VariableDeclarationContainer {
	private List<UMLParameter> parameters = new ArrayList<>();
	private LocationInfo locationInfo;
	private Map<String, Set<VariableDeclaration>> variableDeclarationMap;
	private String constructorName;
	private String className;
	private final Constants LANG;

	public PrimaryConstructor(LocationInfo locationInfo, String constructorName, String className) {
		this.locationInfo = locationInfo;
		this.constructorName = constructorName;
		this.className = className;
		this.LANG = PathFileUtils.getLang(locationInfo.getFilePath());
	}

	public void addParameter(UMLParameter parameter) {
		this.parameters.add(parameter);
	}

	public List<UMLParameter> getParameters() {
		return parameters;
	}

	@Override
	public List<VariableDeclaration> getParameterDeclarationList() {
		List<VariableDeclaration> parameterDeclarationList = new ArrayList<VariableDeclaration>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterDeclarationList.add(parameter.getVariableDeclaration());
		}
		return parameterDeclarationList;
	}

	@Override
	public List<UMLType> getParameterTypeList() {
		List<UMLType> parameterTypeList = new ArrayList<UMLType>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterTypeList.add(parameter.getType());
		}
		return parameterTypeList;
	}

	@Override
	public List<String> getParameterNameList() {
		List<String> parameterNameList = new ArrayList<String>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterNameList.add(parameter.getName());
		}
		return parameterNameList;
	}

	@Override
	public List<UMLParameter> getParametersWithoutReturnType() {
		List<UMLParameter> params = new ArrayList<UMLParameter>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				params.add(parameter);
		}
		return params;
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
	public OperationBody getBody() {
		return null;
	}

	@Override
	public List<UMLAnonymousClass> getAnonymousClassList() {
		return Collections.emptyList();
	}

	@Override
	public List<LambdaExpressionObject> getAllLambdas() {
		return Collections.emptyList();
	}

	@Override
	public List<AbstractCall> getAllOperationInvocations() {
		return Collections.emptyList();
	}

	@Override
	public List<AbstractCall> getAllCreations() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getAllVariables() {
		return Collections.emptyList();
	}

	@Override
	public List<UMLComment> getComments() {
		return Collections.emptyList();
	}

	@Override
	public String getName() {
		return constructorName;
	}

	@Override
	public String getElementType() {
		return "primary constructor";
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getNonQualifiedClassName() {
		return className.contains(".") ? className.substring(className.lastIndexOf(".")+1, className.length()) : className;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(constructorName);
		sb.append("(");
		for(int i=0; i<parameters.size(); i++) {
			UMLParameter parameter = parameters.get(i);
			if(parameter.getKind().equals("in")) {
				if(LANG.equals(Constants.PYTHON))
					sb.append(parameter.getName());
				else
					sb.append(parameter.toString());
				if(i < parameters.size()-1)
					sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(constructorName);
		sb.append("(");
		for(int i=0; i<parameters.size(); i++) {
			UMLParameter parameter = parameters.get(i);
			if(parameter.getKind().equals("in")) {
				if(LANG.equals(Constants.PYTHON))
					sb.append(parameter.getName());
				else
					sb.append(parameter.toQualifiedString());
				if(i < parameters.size()-1)
					sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public Map<String, Set<VariableDeclaration>> variableDeclarationMap() {
		if(this.variableDeclarationMap == null) {
			this.variableDeclarationMap = new LinkedHashMap<String, Set<VariableDeclaration>>();
			for(VariableDeclaration declaration : getAllVariableDeclarations()) {
				if(variableDeclarationMap.containsKey(declaration.getVariableName())) {
					variableDeclarationMap.get(declaration.getVariableName()).add(declaration);
				}
				else {
					Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
					variableDeclarations.add(declaration);
					variableDeclarationMap.put(declaration.getVariableName(), variableDeclarations);
				}
			}
		}
		return variableDeclarationMap;
	}

}
