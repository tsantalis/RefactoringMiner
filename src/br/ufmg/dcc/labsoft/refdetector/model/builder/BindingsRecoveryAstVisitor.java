package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import br.ufmg.dcc.labsoft.refactoringanalyzer.util.AstUtils;
import br.ufmg.dcc.labsoft.refdetector.model.SDAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.SDContainerEntity;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDPackage;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;
import br.ufmg.dcc.labsoft.refdetector.model.SourceRepresentation;
import br.ufmg.dcc.labsoft.refdetector.model.Visibility;

public class BindingsRecoveryAstVisitor extends ASTVisitor {

	private final SDModel.Snapshot model;
	private final String sourceFilePath;
	private final char[] fileContent;
	private final LinkedList<SDContainerEntity> containerStack;
	private final Map<SDMethod, List<String>> postProcessInvocations;
	private final Map<SDType, List<String>> postProcessSupertypes;
	private final SourceScanner scanner;
	
	public BindingsRecoveryAstVisitor(SDModel.Snapshot model, String sourceFilePath, char[] fileContent, SDPackage sdPackage, Map<SDMethod, List<String>> postProcessMap, Map<SDType, List<String>> postProcessSupertypes) {
		this.model = model;
		this.sourceFilePath = sourceFilePath;
		this.fileContent = fileContent;
		this.containerStack = new LinkedList<SDContainerEntity>();
		this.containerStack.push(sdPackage);
		this.postProcessInvocations = postProcessMap;
		this.postProcessSupertypes = postProcessSupertypes;
		this.scanner = new SourceScanner();
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
		containerStack.push(visitTypeDeclaration(node, null));
		return true;
	}
	public void endVisit(EnumDeclaration node) {
		containerStack.pop();
	}
	
	public boolean visit(TypeDeclaration typeDeclaration) {
		Type superType = typeDeclaration.getSuperclassType();
		containerStack.push(visitTypeDeclaration(typeDeclaration, superType));
		return true;
	}
	public void endVisit(TypeDeclaration node) {
		containerStack.pop();
	}

	private SDType visitTypeDeclaration(AbstractTypeDeclaration node, Type superType) {
	    String typeName = node.getName().getIdentifier();
		SDType type = model.createType(typeName, containerStack.peek(), sourceFilePath);
		type.setSourceCode(scanner.getLineBasedSourceRepresentation(fileContent, node.getStartPosition(), node.getLength()));
		
		Set<String> annotations = extractAnnotationTypes(node.modifiers());
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
//		if (methodDeclaration.getName().getIdentifier().equals("execMultiLineCommands")) {
//			System.out.println("x");
//			
//		}
		
		final SDMethod method = model.createMethod(methodSignature, containerStack.peek(), methodDeclaration.isConstructor());
		
		List<?> modifiers = methodDeclaration.modifiers();
		Set<String> annotations = extractAnnotationTypes(modifiers);
		method.setTestAnnotation(annotations.contains("Test"));
		method.setDeprecatedAnnotation(annotations.contains("Deprecated") || AstUtils.containsDeprecatedTag(methodDeclaration.getJavadoc()));
		
		int methodModifiers = methodDeclaration.getModifiers();
		Visibility visibility = getVisibility(methodModifiers);
		
        method.setVisibility(visibility);
        extractParametersAndReturnType(methodDeclaration, method);
		
		method.setNumberOfStatements(AstUtils.countNumberOfStatements(methodDeclaration));
		Block body = methodDeclaration.getBody();
		if (body == null) {
			method.setSourceCode(new SourceRepresentation(new long[0]));
			method.setAbstract(true);
		} else {
			method.setSourceCode(scanner.getTokenBasedSourceRepresentation(this.fileContent, body.getStartPosition() + 1, body.getLength() - 2));
		}
		
		final List<String> invocations = new ArrayList<String>();
		
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

    private Visibility getVisibility(int methodModifiers) {
        Visibility visibility;
        if((methodModifiers & Modifier.PUBLIC) != 0)
            visibility = Visibility.PUBLIC;
        else if((methodModifiers & Modifier.PROTECTED) != 0)
            visibility = Visibility.PROTECTED;
        else if((methodModifiers & Modifier.PRIVATE) != 0)
            visibility = Visibility.PRIVATE;
        else
            visibility = Visibility.PACKAGE;
        return visibility;
    }

    @Override
    public boolean visit(FieldDeclaration fieldDeclaration) {
        Type fieldType = fieldDeclaration.getType();
        int fieldModifiers = fieldDeclaration.getModifiers();
        Visibility visibility = getVisibility(fieldModifiers);
//        boolean isFinal = (fieldModifiers & Modifier.FINAL) != 0;
        boolean isStatic = (fieldModifiers & Modifier.STATIC) != 0;
        List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            String fieldName = fragment.getName().getIdentifier();
            final SDAttribute attribute = model.createAttribute(fieldName, containerStack.peek());
            attribute.setStatic(isStatic);
            attribute.setVisibility(visibility);
            attribute.setType(AstUtils.normalizeTypeName(fieldType, fragment.getExtraDimensions(), false));
        }
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

	
	public static void extractParametersAndReturnType(MethodDeclaration methodDeclaration, SDMethod method) {
	    Type returnType = methodDeclaration.getReturnType2();
	    if (returnType != null) {
	        method.setReturnType(AstUtils.normalizeTypeName(returnType, methodDeclaration.getExtraDimensions(), false));
	    } else {
	        method.setReturnType(null);
	    }
        Iterator<SingleVariableDeclaration> parameters = methodDeclaration.parameters().iterator();
        while (parameters.hasNext()) {
            SingleVariableDeclaration parameter = parameters.next();
            Type parameterType = parameter.getType();
            String typeName = AstUtils.normalizeTypeName(parameterType, parameter.getExtraDimensions(), parameter.isVarargs());
            method.addParameter(parameter.getName().getIdentifier(), typeName);
        }
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
