package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import br.ufmg.dcc.labsoft.refactoringanalyzer.util.AstUtils;
import br.ufmg.dcc.labsoft.refdetector.model.SDContainerEntity;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDPackage;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;
import br.ufmg.dcc.labsoft.refdetector.model.SourceCode;

public class BindingsRecoveryAstVisitor extends ASTVisitor {

	private final SDModel.Snapshot model;
	private final String sourceFilePath;
	private final LinkedList<SDContainerEntity> containerStack;
	private final Map<SDMethod, List<String>> postProcessInvocations;
	private final Map<SDType, List<String>> postProcessSupertypes;
	
	public BindingsRecoveryAstVisitor(SDModel.Snapshot model, String sourceFilePath, SDPackage sdPackage, Map<SDMethod, List<String>> postProcessMap, Map<SDType, List<String>> postProcessSupertypes) {
		this.model = model;
		this.sourceFilePath = sourceFilePath;
		this.containerStack = new LinkedList<SDContainerEntity>();
		this.containerStack.push(sdPackage);
		this.postProcessInvocations = postProcessMap;
		this.postProcessSupertypes = postProcessSupertypes;
	}
	
	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		if (node.getParent() instanceof ClassInstanceCreation) {
			ClassInstanceCreation parent = (ClassInstanceCreation) node.getParent();
			ITypeBinding typeBinding = parent.getType().resolveBinding();
			if (typeBinding != null && typeBinding.isFromSource()) {
				SDType type = model.createAnonymousType(containerStack.peek(), sourceFilePath);
				containerStack.push(type);
				extractSupertypesForPostProcessing(type, typeBinding);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void endVisit(AnonymousClassDeclaration node) {
		if (node.getParent() instanceof ClassInstanceCreation) {
			ClassInstanceCreation parent = (ClassInstanceCreation) node.getParent();
			ITypeBinding typeBinding = parent.getType().resolveBinding();
			if (typeBinding != null && typeBinding.isFromSource()) {
				containerStack.pop();
			}
		}
	}
	
	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		return false;
	}
	
	@Override
	public boolean visit(EnumDeclaration node) {
		String typeName = node.getName().getIdentifier();
		containerStack.push(visitTypeDeclaration(typeName, null, node.modifiers()));
		return true;
	}
	public void endVisit(EnumDeclaration node) {
		containerStack.pop();
	}
	
	public boolean visit(TypeDeclaration typeDeclaration) {
		String typeName = typeDeclaration.getName().getIdentifier();
		Type superType = typeDeclaration.getSuperclassType();
		containerStack.push(visitTypeDeclaration(typeName, superType, typeDeclaration.modifiers()));
		return true;
	}
	public void endVisit(TypeDeclaration node) {
		containerStack.pop();
	}

	private SDType visitTypeDeclaration(String typeName, Type superType, List<?> modifiers) {
		SDType type = model.createType(typeName, containerStack.peek(), sourceFilePath);
		
		Set<String> annotations = extractAnnotationTypes(modifiers);
		type.setDeprecatedAnnotation(annotations.contains("Deprecated"));
		
    	if (superType != null) {
    		ITypeBinding superTypeBinding = superType.resolveBinding();
    		extractSupertypesForPostProcessing(type, superTypeBinding);
    	}
    	return type;
	}

	private void extractSupertypesForPostProcessing(SDType type, ITypeBinding superTypeBinding) {
		List<String> supertypes = new ArrayList<String>();
		while (superTypeBinding != null && superTypeBinding.isFromSource()) {
			String superTypeName = superTypeBinding.getErasure().getQualifiedName();
			supertypes.add(superTypeName);
			superTypeBinding = superTypeBinding.getSuperclass();
		}
		if (!supertypes.isEmpty()) {
			postProcessSupertypes.put(type, supertypes);
		}
	}
	
	public boolean visit(MethodDeclaration methodDeclaration) {
//		ASTNode parentNode = methodDeclaration.getParent();
//		if (!(parentNode instanceof TypeDeclaration)) {
//			// ignore methods from anonymous classes
//			return false;
//		}
		String methodSignature = AstUtils.getSignatureFromMethodDeclaration(methodDeclaration);
		SDMethod method = model.createMethod(methodSignature, containerStack.peek());
//		System.out.println("Method: " + methodSignature);
		
	//	boolean testAnnotation = false;
//		boolean deprecatedAnnotation = false;
		List<?> modifiers = methodDeclaration.modifiers();
		Set<String> annotations = extractAnnotationTypes(modifiers);
		method.setTestAnnotation(annotations.contains("Test"));
		method.setDeprecatedAnnotation(annotations.contains("Deprecated"));
		
		method.setNumberOfStatements(AstUtils.countNumberOfStatements(methodDeclaration));
		method.setBody(new SourceCode(methodDeclaration.getBody().toString()));
		
		final List<String> invocations = new ArrayList<String>();
//		if (method.toString().endsWith("IterableSubject#isPartiallyOrdered()")) {
//			System.out.print(' ');
//		}
		methodDeclaration.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation node) {
				IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null && binding.getDeclaringClass().isFromSource()) {
					String methodKey = AstUtils.getKeyFromMethodBinding(binding);
					invocations.add(methodKey);
				}
				return true;
			}
		});
		
		postProcessInvocations.put(method, invocations);
		return true;
	}

	private static Set<String> extractAnnotationTypes(List<?> modifiers) {
		Set<String> annotations = new HashSet<String>();
		for (Object modifier : modifiers) {
			if (modifier instanceof Annotation) {
				Annotation a = (Annotation) modifier;
				annotations.add(a.getTypeName().toString());
			}
		}
		return annotations;
	}

//	@Override
//	public void endVisit(MethodDeclaration methodDeclaration) {
//		if (methodDeclaration.getName().toString().endsWith("testFixedMembershipTokenIPv4")) {
//			System.out.print(' ');
//		}
//	}
	
	
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
