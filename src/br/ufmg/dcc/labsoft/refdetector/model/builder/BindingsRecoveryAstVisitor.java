package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
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
	private final Map<SDMethod, MethodDeclaration> postProcessMap;
	
	public BindingsRecoveryAstVisitor(SDModel model, String sourceFilePath, SDPackage sdPackage) {
		this.model = model;
		this.sourceFilePath = sourceFilePath;
		this.containerStack = new LinkedList<SDContainerEntity>();
		this.postProcessMap = new HashMap<SDMethod, MethodDeclaration>();
		this.containerStack.push(sdPackage);
	}

	@Override
	public void endVisit(CompilationUnit node) {
		for (Map.Entry<SDMethod, MethodDeclaration> entry : postProcessMap.entrySet()) {
			final SDMethod method = entry.getKey();
			MethodDeclaration decl = entry.getValue();
			decl.accept(new ASTVisitor() {
				public boolean visit(MethodInvocation node) {
					IMethodBinding binding = node.resolveMethodBinding();
					if (binding != null) {
						String methodKey = getKeyFromMethodBinding(binding);
						SDMethod invoked = model.find(SDMethod.class, methodKey);
						if (invoked != null) {
							invoked.addCaller(method);
						}
					}
					return true;
				}
			});
		}
	}
	
	private String getKeyFromMethodBinding(IMethodBinding binding) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean visit(TypeDeclaration typeDeclaration) {
		String typeName = typeDeclaration.getName().getIdentifier();
		SDType type = model.createType(typeName, containerStack.peek());
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
		System.out.println("Method: " + method);
		
		postProcessMap.put(method, methodDeclaration);
		return false;
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
}
