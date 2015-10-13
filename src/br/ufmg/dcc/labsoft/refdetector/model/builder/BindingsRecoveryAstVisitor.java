package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import br.ufmg.dcc.labsoft.refdetector.model.SDContainerEntity;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDPackage;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;

public class BindingsRecoveryAstVisitor extends ASTVisitor {

	private final SDModel model;
	private final String sourceFilePath;
	private final LinkedList<SDContainerEntity> containerStack;
	private final Map<SDMethod, List<String>> postProcessMap;
	
	public BindingsRecoveryAstVisitor(SDModel model, String sourceFilePath, SDPackage sdPackage, Map<SDMethod, List<String>> postProcessMap) {
		this.model = model;
		this.sourceFilePath = sourceFilePath;
		this.containerStack = new LinkedList<SDContainerEntity>();
		this.containerStack.push(sdPackage);
		this.postProcessMap = postProcessMap;
	}
	
	public boolean visit(TypeDeclaration typeDeclaration) {
		String typeName = typeDeclaration.getName().getIdentifier();
		SDType type = model.createType(typeName, containerStack.peek(), sourceFilePath);
		containerStack.push(type);
		
		System.out.println("Type: " + typeName);
//		UMLClass umlClass = new UMLClass(packageName, className, null, sourceFile, typeDeclaration.isPackageMemberTypeDeclaration());
//		
//		if(typeDeclaration.isInterface()) {
//			umlClass.setInterface(true);
//    	}
//    	
//    	int modifiers = typeDeclaration.getModifiers();
//    	if((modifiers & Modifier.ABSTRACT) != 0)
//    		umlClass.setAbstract(true);
//    	
//    	if((modifiers & Modifier.PUBLIC) != 0)
//    		umlClass.setVisibility("public");
//    	else if((modifiers & Modifier.PROTECTED) != 0)
//    		umlClass.setVisibility("protected");
//    	else if((modifiers & Modifier.PRIVATE) != 0)
//    		umlClass.setVisibility("private");
//    	else
//    		umlClass.setVisibility("package");
//		
//    	Type superclassType = typeDeclaration.getSuperclassType();
//    	if (superclassType != null) {
//    		String typeName = this.getTypeName(superclassType);
//    		System.out.println("Superclass: " + typeName);
//    	}
		
		return true;
	};
	
	public void endVisit(TypeDeclaration node) {
		containerStack.pop();
	}
	
	public boolean visit(MethodDeclaration methodDeclaration) {
		String methodSignature = getSignatureFromMethodDeclaration(methodDeclaration);
		SDMethod method = model.createMethod(methodSignature, containerStack.peek());
		System.out.println("Method: " + methodSignature);
		
		boolean testAnnotation = false;
		boolean deprecatedAnnotation = false;
		List<?> modifiers = methodDeclaration.modifiers();
		for (Object modifier : modifiers) {
			if (modifier instanceof Annotation) {
				Annotation a = (Annotation) modifier;
				testAnnotation = testAnnotation || a.getTypeName().toString().equals("Test");
				deprecatedAnnotation = deprecatedAnnotation || a.getTypeName().toString().equals("Deprecated");
			}
		}
		method.setTestAnnotation(testAnnotation);
		method.setDeprecatedAnnotation(deprecatedAnnotation);
		
		method.setNumberOfStatements(countNumberOfStatements(methodDeclaration));
		
		final List<String> invocations = new ArrayList<String>();
//		if (method.toString().endsWith("testFixedMembershipTokenIPv4()")) {
//			System.out.print(' ');
//		}
		methodDeclaration.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation node) {
				IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null && binding.getDeclaringClass().isFromSource()) {
					String methodKey = getKeyFromMethodBinding(binding);
					invocations.add(methodKey);
				}
				return true;
			}
		});
		
		postProcessMap.put(method, invocations);
		return true;
	}

//	@Override
//	public void endVisit(MethodDeclaration methodDeclaration) {
//		if (methodDeclaration.getName().toString().endsWith("testFixedMembershipTokenIPv4")) {
//			System.out.print(' ');
//		}
//	}
	
	private String getKeyFromMethodBinding(IMethodBinding binding) {
		StringBuilder sb = new StringBuilder();
		String className = binding.getDeclaringClass().getQualifiedName();
		sb.append(className);
		sb.append('#');
		String methodName = binding.getName();
		sb.append(methodName);
		sb.append('(');
		ITypeBinding[] parameters = binding.getParameterTypes();
		for (int i = 0; i < parameters.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			ITypeBinding type = parameters[i];
			sb.append(type.getName());
		}
		sb.append(')');
		return sb.toString();
	}
	
	private String getSignatureFromMethodDeclaration(MethodDeclaration methodDeclaration) {
		String methodName = methodDeclaration.getName().getIdentifier();
		StringBuilder sb = new StringBuilder();
		sb.append(methodName);
		sb.append('(');
		Iterator<SingleVariableDeclaration> parameters = methodDeclaration.parameters().iterator();
		while (parameters.hasNext()) {
			SingleVariableDeclaration parameter = parameters.next();
			Type parameterType = parameter.getType();
			sb.append(parameterType.toString());
			if (parameters.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(')');
		String methodSignature = sb.toString();
		return methodSignature;
	}
	
//	private String getTypeName(Type type) {
//		ITypeBinding binding = type.resolveBinding();
//		ITypeBinding y = binding;
//		while (y != null && y.isFromSource()) {
//			System.out.println(y.getQualifiedName());
//			y = y.getSuperclass();
//		}
//		
//		if (binding != null) {
//			return binding.getQualifiedName();
//		}
//		return type.toString();
//	}
	
	public static int countNumberOfStatements(MethodDeclaration decl) {
		return new StatementCounter().countStatements(decl);
	}
	
	private static class StatementCounter extends ASTVisitor {
		private int counter;
		public int countStatements(MethodDeclaration methodDeclaration) {
			counter = 0;
			methodDeclaration.accept(this);
			return counter;
		}
		@Override
		public void preVisit(ASTNode node) {
			if (node instanceof Statement && !(node instanceof Block)) {
				counter++;
			}
		}
	}
}
