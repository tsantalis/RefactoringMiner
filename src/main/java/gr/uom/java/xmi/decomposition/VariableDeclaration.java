package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypePattern;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModifier;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.VariableDeclarationProvider;
import gr.uom.java.xmi.diff.CodeRange;

public class VariableDeclaration implements LocationInfoProvider, VariableDeclarationProvider {
	private String variableName;
	private AbstractExpression initializer;
	private UMLType type;
	private boolean varargsParameter;
	private LocationInfo locationInfo;
	private boolean isParameter;
	private boolean isAttribute;
	private boolean isEnumConstant;
	private VariableScope scope;
	private boolean isFinal;
	private List<UMLAnnotation> annotations;
	private List<UMLModifier> modifiers;
	private String actualSignature;

	public VariableDeclaration(CompilationUnit cu, String sourceFolder, String filePath, VariableDeclarationFragment fragment, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String javaFileContent) {
		this.annotations = new ArrayList<UMLAnnotation>();
		this.modifiers = new ArrayList<UMLModifier>();
		List<IExtendedModifier> extendedModifiers = null;
		if(fragment.getParent() instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement parent = (VariableDeclarationStatement)fragment.getParent();
			extendedModifiers = parent.modifiers();
			int modifiers = parent.getModifiers();
			if((modifiers & Modifier.FINAL) != 0) {
				this.isFinal = true;
			}
		}
		else if(fragment.getParent() instanceof VariableDeclarationExpression) {
			VariableDeclarationExpression parent = (VariableDeclarationExpression)fragment.getParent();
			extendedModifiers = parent.modifiers();
			int modifiers = parent.getModifiers();
			if((modifiers & Modifier.FINAL) != 0) {
				this.isFinal = true;
			}
		}
		else if(fragment.getParent() instanceof FieldDeclaration) {
			FieldDeclaration parent = (FieldDeclaration)fragment.getParent();
			extendedModifiers = parent.modifiers();
			int modifiers = parent.getModifiers();
			if((modifiers & Modifier.FINAL) != 0) {
				this.isFinal = true;
			}
		}
		int startSignatureOffset = -1;
		if(extendedModifiers != null) {
			for(IExtendedModifier extendedModifier : extendedModifiers) {
				if(extendedModifier.isAnnotation()) {
					Annotation annotation = (Annotation)extendedModifier;
					this.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
				}
				else if(extendedModifier.isModifier()) {
					Modifier modifier = (Modifier)extendedModifier;
					this.modifiers.add(new UMLModifier(cu, sourceFolder, filePath, modifier));
					if(startSignatureOffset == -1) {
						startSignatureOffset = modifier.getStartPosition();
					}
				}
			}
		}
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, fragment, extractVariableDeclarationType(fragment));
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = fragment.getInitializer() != null ? new AbstractExpression(cu, sourceFolder, filePath, fragment.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, javaFileContent) : null;
		Type astType = extractType(fragment);
		if(astType != null) {
			this.type = UMLType.extractTypeObject(cu, sourceFolder, filePath, astType, fragment.getExtraDimensions(), javaFileContent);
			if(startSignatureOffset == -1) {
				startSignatureOffset = astType.getStartPosition();
			}
		}
		if(startSignatureOffset == -1) {
			startSignatureOffset = fragment.getName().getStartPosition();
		}
		ASTNode scopeNode = getScopeNode(fragment);
		int startOffset = 0;
		if(locationInfo.getCodeElementType().equals(CodeElementType.FIELD_DECLARATION)) {
			//field declarations have the entire type declaration as scope, regardless of the location they are declared
			startOffset = scopeNode.getStartPosition();
		}
		else {
			startOffset = fragment.getStartPosition();
		}
		int endOffset = scopeNode.getStartPosition() + scopeNode.getLength();
		this.scope = new VariableScope(cu, filePath, startOffset, endOffset);
		boolean anonymousClassInitializer = fragment.getInitializer() instanceof ClassInstanceCreation && ((ClassInstanceCreation)fragment.getInitializer()).getAnonymousClassDeclaration() != null;
		int endSignatureOffset = anonymousClassInitializer ?
				((ClassInstanceCreation)fragment.getInitializer()).getAnonymousClassDeclaration().getStartPosition() + 1 :
					fragment.getStartPosition() + fragment.getLength();
		this.actualSignature = javaFileContent.substring(startSignatureOffset, endSignatureOffset);
	}

	public VariableDeclaration(CompilationUnit cu, String sourceFolder, String filePath, SingleVariableDeclaration fragment, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String javaFileContent) {
		this.annotations = new ArrayList<UMLAnnotation>();
		this.modifiers = new ArrayList<UMLModifier>();
		int modifiers = fragment.getModifiers();
		if((modifiers & Modifier.FINAL) != 0) {
			this.isFinal = true;
		}
		List<IExtendedModifier> extendedModifiers = fragment.modifiers();
		for(IExtendedModifier extendedModifier : extendedModifiers) {
			if(extendedModifier.isAnnotation()) {
				Annotation annotation = (Annotation)extendedModifier;
				this.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
			}
			else if(extendedModifier.isModifier()) {
				Modifier modifier = (Modifier)extendedModifier;
				this.modifiers.add(new UMLModifier(cu, sourceFolder, filePath, modifier));
			}
		}
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, fragment, extractVariableDeclarationType(fragment));
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = fragment.getInitializer() != null ? new AbstractExpression(cu, sourceFolder, filePath, fragment.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, javaFileContent) : null;
		Type astType = extractType(fragment);
		this.type = UMLType.extractTypeObject(cu, sourceFolder, filePath, astType, fragment.getExtraDimensions(), javaFileContent);
		int startOffset = fragment.getStartPosition();
		ASTNode scopeNode = getScopeNode(fragment);
		int endOffset = scopeNode.getStartPosition() + scopeNode.getLength();
		this.scope = new VariableScope(cu, filePath, startOffset, endOffset);
	}

	public VariableDeclaration(CompilationUnit cu, String sourceFolder, String filePath, SingleVariableDeclaration fragment, VariableDeclarationContainer container, boolean varargs, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String javaFileContent) {
		this(cu, sourceFolder, filePath, fragment, container, activeVariableDeclarations, javaFileContent);
		this.varargsParameter = varargs;
		if(varargs) {
			this.type.setVarargs();
		}
	}

	public VariableDeclaration(CompilationUnit cu, String sourceFolder, String filePath, EnumConstantDeclaration fragment, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String javaFileContent) {
		this.annotations = new ArrayList<UMLAnnotation>();
		this.modifiers = new ArrayList<UMLModifier>();
		int modifiers = fragment.getModifiers();
		if((modifiers & Modifier.FINAL) != 0) {
			this.isFinal = true;
		}
		this.isEnumConstant = true;
		int startSignatureOffset = -1;
		List<IExtendedModifier> extendedModifiers = fragment.modifiers();
		for(IExtendedModifier extendedModifier : extendedModifiers) {
			if(extendedModifier.isAnnotation()) {
				Annotation annotation = (Annotation)extendedModifier;
				this.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
			}
			else if(extendedModifier.isModifier()) {
				Modifier modifier = (Modifier)extendedModifier;
				this.modifiers.add(new UMLModifier(cu, sourceFolder, filePath, modifier));
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getStartPosition();
				}
			}
		}
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, fragment, CodeElementType.ENUM_CONSTANT_DECLARATION);
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = null;
		if(startSignatureOffset == -1) {
			startSignatureOffset = fragment.getName().getStartPosition();
		}
		if(fragment.getParent() instanceof EnumDeclaration) {
			EnumDeclaration enumDeclaration = (EnumDeclaration)fragment.getParent();
			this.type = UMLType.extractTypeObject(enumDeclaration.getName().getIdentifier());
		}
		ASTNode scopeNode = fragment.getParent();
		int startOffset = scopeNode.getStartPosition();
		int endOffset = scopeNode.getStartPosition() + scopeNode.getLength();
		this.scope = new VariableScope(cu, filePath, startOffset, endOffset);
		int endSignatureOffset = fragment.getAnonymousClassDeclaration() != null ?
				fragment.getAnonymousClassDeclaration().getStartPosition() + 1 :
				fragment.getStartPosition() + fragment.getLength();
		this.actualSignature = javaFileContent.substring(startSignatureOffset, endSignatureOffset);
	}

	public String getActualSignature() {
		return actualSignature;
	}

	public LeafExpression asLeafExpression() {
		String asString = type != null ? type.toQualifiedString() : "" + variableName;
		return new LeafExpression(asString, getLocationInfo());
	}

	public String getVariableName() {
		return variableName;
	}

	public AbstractExpression getInitializer() {
		return initializer;
	}

	public UMLType getType() {
		return type;
	}

	public VariableScope getScope() {
		return scope;
	}

	public boolean isLocalVariable() {
		return !isParameter && !isAttribute && !isEnumConstant;
	}

	public boolean isParameter() {
		return isParameter;
	}

	public void setParameter(boolean isParameter) {
		this.isParameter = isParameter;
	}

	public boolean isAttribute() {
		return isAttribute;
	}

	public void setAttribute(boolean isAttribute) {
		this.isAttribute = isAttribute;
	}

	public boolean isEnumConstant() {
		return isEnumConstant;
	}

	public boolean isVarargsParameter() {
		return varargsParameter;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public List<UMLAnnotation> getAnnotations() {
		return annotations;
	}

	public List<UMLModifier> getModifiers() {
		return modifiers;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
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
		VariableDeclaration other = (VariableDeclaration) obj;
		if (scope == null) {
			if (other.scope != null)
				return false;
		} else if (!scope.equals(other.scope))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		return true;
	}

	public boolean sameKind(VariableDeclaration other) {
		return this.isParameter == other.isParameter && this.isEnumConstant == other.isEnumConstant && this.isAttribute == other.isAttribute;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ");
        if(varargsParameter) {
        	sb.append(type.toString().substring(0, type.toString().lastIndexOf("[]")));
        	sb.append("...");
        }
        else {
        	sb.append(type);
        }
        return sb.toString();
	}

	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ");
        if(varargsParameter) {
        	sb.append(type.toQualifiedString().substring(0, type.toQualifiedString().lastIndexOf("[]")));
        	sb.append("...");
        }
        else {
        	sb.append(type.toQualifiedString());
        }
        return sb.toString();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	private static ASTNode getScopeNode(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
		if(variableDeclaration instanceof SingleVariableDeclaration) {
			if(variableDeclaration.getParent() instanceof TypePattern) {
				ASTNode grandParent = variableDeclaration.getParent().getParent();
				if(grandParent instanceof PatternInstanceofExpression) {
					ASTNode grandGrandParent = grandParent.getParent();
					if(grandGrandParent instanceof IfStatement || grandParent instanceof WhileStatement) {
						return grandGrandParent;
					}
					else if(grandGrandParent instanceof ParenthesizedExpression) {
						ASTNode p = grandGrandParent.getParent();
						if(p instanceof PrefixExpression && ((PrefixExpression)p).getOperator().equals(PrefixExpression.Operator.NOT)) {
							// The scope of a pattern variable can extend beyond the statement that introduced it
							// https://docs.oracle.com/en/java/javase/23/language/pattern-matching-instanceof.html#GUID-E8F57F2F-C14C-4822-9C70-7C76033D4331
							if(p.getParent() instanceof Statement)
								return p.getParent().getParent();
						}
					}
					else if(grandGrandParent instanceof InfixExpression) {
						return grandGrandParent.getParent();
					}
				}
			}
			return variableDeclaration.getParent();
		}
		else if(variableDeclaration instanceof VariableDeclarationFragment) {
			return variableDeclaration.getParent().getParent();
		}
		return null;
	}

	private static CodeElementType extractVariableDeclarationType(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
		if(variableDeclaration instanceof SingleVariableDeclaration) {
			return CodeElementType.SINGLE_VARIABLE_DECLARATION;
		}
		else if(variableDeclaration instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
			if(fragment.getParent() instanceof VariableDeclarationStatement) {
				return CodeElementType.VARIABLE_DECLARATION_STATEMENT;
			}
			else if(fragment.getParent() instanceof VariableDeclarationExpression) {
				return CodeElementType.VARIABLE_DECLARATION_EXPRESSION;
			}
			else if(fragment.getParent() instanceof FieldDeclaration) {
				return CodeElementType.FIELD_DECLARATION;
			}
			else if(fragment.getParent() instanceof LambdaExpression) {
				return CodeElementType.LAMBDA_EXPRESSION_PARAMETER;
			}
		}
		return null;
	}

	private static Type extractType(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
		Type returnedVariableType = null;
		if(variableDeclaration instanceof SingleVariableDeclaration) {
			SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)variableDeclaration;
			returnedVariableType = singleVariableDeclaration.getType();
		}
		else if(variableDeclaration instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
			if(fragment.getParent() instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				returnedVariableType = variableDeclarationStatement.getType();
			}
			else if(fragment.getParent() instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)fragment.getParent();
				returnedVariableType = variableDeclarationExpression.getType();
			}
			else if(fragment.getParent() instanceof FieldDeclaration) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				returnedVariableType = fieldDeclaration.getType();
			}
		}
		return returnedVariableType;
	}

	public boolean equalVariableDeclarationType(VariableDeclaration other) {
		return this.locationInfo.getCodeElementType().equals(other.locationInfo.getCodeElementType());
	}

	public boolean equalType(VariableDeclaration other) {
		if(this.getType() == null && other.getType() == null) {
			return true;
		}
		else if(this.getType() != null && other.getType() != null) {
			return this.getType().equals(other.getType());
		}
		return false;
	}

	public boolean equalQualifiedType(VariableDeclaration other) {
		if(this.getType() == null && other.getType() == null) {
			return true;
		}
		else if(this.getType() != null && other.getType() != null) {
			return this.getType().equalsQualified(other.getType());
		}
		return false;
	}

	public VariableDeclaration getVariableDeclaration() {
		return this;
	}

	public void addStatementInScope(AbstractCodeFragment statement, boolean fieldWithOverwrittenScopeByLocalVariable) {
		if(scope.subsumes(statement.getLocationInfo())) {
			List<LeafExpression> variables = statement.getVariables();
			boolean matchFound = false;
			for(LeafExpression variable : variables) {
				if(fieldWithOverwrittenScopeByLocalVariable) {
					if(variable.getString().equals(JAVA.THIS_DOT + variableName)) {
						scope.addStatementUsingVariable(statement);
						matchFound = true;
						break;
					}
				}
				else if(variable.getString().equals(variableName) || (isAttribute && variable.getString().equals(JAVA.THIS_DOT + variableName))) {
					scope.addStatementUsingVariable(statement);
					matchFound = true;
					break;
				}
			}
			if(!matchFound) {
				for(LeafExpression variable : variables) {
					if(fieldWithOverwrittenScopeByLocalVariable) {
						if(variable.getString().equals(JAVA.THIS_DOT + variableName + ".")) {
							scope.addStatementUsingVariable(statement);
							break;
						}
					}
					else if(variable.getString().startsWith(variableName + ".")) {
						scope.addStatementUsingVariable(statement);
						break;
					}
				}
			}
		}
	}

	public Set<AbstractCodeFragment> getStatementsInScopeUsingVariable() {
		return scope.getStatementsInScopeUsingVariable();
	}
}
