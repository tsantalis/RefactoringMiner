package gr.uom.java.xmi;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.*;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiTreeUtil;
import gr.uom.java.xmi.decomposition.PsiUtils;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class UMLModelASTReader {
	private static final PsiFileFactory factory =
			PsiFileFactory.getInstance(ProjectManager.getInstance().getDefaultProject());
	private static final String FREE_MARKER_GENERATED = "generated using freemarker";
	private UMLModel umlModel;

	public UMLModelASTReader(Map<String, String> javaFileContents, Set<String> repositoryDirectories) {
		this.umlModel = new UMLModel(repositoryDirectories);
		processJavaFileContents(javaFileContents);
	}

	private void processJavaFileContents(Map<String, String> javaFileContents) {
		for(String filePath : javaFileContents.keySet()) {
			String javaFileContent = javaFileContents.get(filePath);
			if(javaFileContent.contains(FREE_MARKER_GENERATED) &&
					!javaFileContent.contains("private static final String FREE_MARKER_GENERATED = \"generated using freemarker\";")) {
				continue;
			}
			try {
				PsiFile psiFile = factory.createFileFromText(JavaLanguage.INSTANCE, javaFileContent);
				processCompilationUnit(filePath, psiFile, javaFileContent);
			}
			catch(Exception e) {
				//e.printStackTrace();
			}
		}
	}

	public UMLModel getUmlModel() {
		return this.umlModel;
	}

	protected void processCompilationUnit(String sourceFilePath, PsiFile compilationUnit, String javaFileContent) {
		List<UMLComment> comments = extractInternalComments(compilationUnit, sourceFilePath);
		PsiPackageStatement packageDeclaration = PsiTreeUtil.findChildOfType(compilationUnit, PsiPackageStatement.class);
		String packageName = "";
		UMLJavadoc packageDoc = null;
		if(packageDeclaration != null) {
			packageName = packageDeclaration.getPackageName();
			//packageDoc = generateJavadoc(compilationUnit, sourceFilePath, packageDeclaration.getJavadoc());
		}

		PsiImportList imports = PsiUtils.findFirstChildOfType(compilationUnit, PsiImportList.class);
		List<String> importedTypes = new ArrayList<>();
		for(PsiImportStatementBase importDeclaration : imports.getAllImportStatements()) {
			importedTypes.add(importDeclaration.getImportReference().getText());
		}

		PsiElement[] topLevelTypeDeclarations = compilationUnit.getChildren();
        for(PsiElement abstractTypeDeclaration : topLevelTypeDeclarations) {
			if(abstractTypeDeclaration instanceof PsiClass) {
				PsiClass topLevelTypeDeclaration = (PsiClass) abstractTypeDeclaration;
				if(topLevelTypeDeclaration.isEnum()) {
					processEnumDeclaration(compilationUnit, topLevelTypeDeclaration, packageName, sourceFilePath, importedTypes, packageDoc, comments);
				}
				else if(topLevelTypeDeclaration.isAnnotationType()) {
					//
				}
				else {
					processTypeDeclaration(compilationUnit, topLevelTypeDeclaration, packageName, sourceFilePath, importedTypes, packageDoc, comments);
				}
			}
        }
	}

	private List<UMLComment> extractInternalComments(PsiFile compilationUnit, String sourceFile) {
		Collection<PsiComment> psiComments = PsiTreeUtil.findChildrenOfType(compilationUnit, PsiComment.class);
		List<UMLComment> comments = new ArrayList<>();
		for(PsiComment comment : psiComments) {
			LocationInfo locationInfo = null;
			if(comment.getTokenType() == JavaTokenType.END_OF_LINE_COMMENT) {
				locationInfo = generateLocationInfo(compilationUnit, sourceFile, comment, CodeElementType.LINE_COMMENT);
			}
			else if(comment.getTokenType() == JavaTokenType.C_STYLE_COMMENT) {
				locationInfo = generateLocationInfo(compilationUnit, sourceFile, comment, CodeElementType.BLOCK_COMMENT);
			}
			if(locationInfo != null) {
				String text = Formatter.format(comment);
				UMLComment umlComment = new UMLComment(text, locationInfo);
				comments.add(umlComment);
			}
		}
		return comments;
	}

	private void distributeComments(List<UMLComment> compilationUnitComments, LocationInfo codeElementLocationInfo, List<UMLComment> codeElementComments) {
		ListIterator<UMLComment> listIterator = compilationUnitComments.listIterator(compilationUnitComments.size());
		while(listIterator.hasPrevious()) {
			UMLComment comment = listIterator.previous();
			LocationInfo commentLocationInfo = comment.getLocationInfo();
			if(codeElementLocationInfo.subsumes(commentLocationInfo) ||
					codeElementLocationInfo.sameLine(commentLocationInfo) ||
					(codeElementLocationInfo.nextLine(commentLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
					(codeElementComments.size() > 0 && codeElementComments.get(0).getLocationInfo().nextLine(commentLocationInfo))) {
				codeElementComments.add(0, comment);
			}
		}
		compilationUnitComments.removeAll(codeElementComments);
	}

	private UMLJavadoc generateJavadoc(PsiFile cu, PsiJavaDocumentedElement bodyDeclaration, String sourceFile) {
		PsiDocComment javaDoc = bodyDeclaration.getDocComment();
		return generateJavadoc(cu, sourceFile, javaDoc);
	}

	private UMLJavadoc generateJavadoc(PsiFile cu, String sourceFile, PsiDocComment javaDoc) {
		UMLJavadoc doc = null;
		if(javaDoc != null) {
			LocationInfo locationInfo = generateLocationInfo(cu, sourceFile, javaDoc, CodeElementType.JAVADOC);
			doc = new UMLJavadoc(locationInfo);
			PsiDocTag[] tags = javaDoc.getTags();
			for(PsiDocTag tag : tags) {
				UMLTagElement tagElement = new UMLTagElement(tag.getName());
				PsiElement[] fragments = tag.getDataElements();
				for(PsiElement docElement : fragments) {
					tagElement.addFragment(Formatter.format(docElement));
				}
				doc.addTag(tagElement);
			}
		}
		return doc;
	}

	private List<UMLType> getUMLTypesOfReferenceList(PsiFile cu, String sourceFile, PsiReferenceList referenceList) {
		if (referenceList == null) {
			return Collections.emptyList();
		}
		PsiJavaCodeReferenceElement[] referenceElements = referenceList.getReferenceElements();
		List<UMLType> types = new ArrayList<>(referenceElements.length);
		for (PsiJavaCodeReferenceElement referenceElement : referenceElements) {
			types.add(UMLTypePsiParser.extractTypeObject(cu, sourceFile, referenceElement));
		}
		return types;
	}

	private UMLTypeParameter processTypeParameter(PsiFile cu, String sourceFile, PsiTypeParameter typeParameter) {
		UMLTypeParameter umlTypeParameter = new UMLTypeParameter(typeParameter.getName());
		List<UMLType> extendsList = getUMLTypesOfReferenceList(cu, sourceFile, typeParameter.getExtendsList());
		extendsList.forEach(umlTypeParameter::addTypeBound);
		PsiAnnotation[] typeParameterAnnotations = typeParameter.getAnnotations();
		for (PsiAnnotation psiAnnotation : typeParameterAnnotations) {
			umlTypeParameter.addAnnotation(new UMLAnnotation(cu, sourceFile, psiAnnotation));
		}
		return umlTypeParameter;
	}

	private void processEnumDeclaration(PsiFile cu, PsiClass enumDeclaration, String packageName, String sourceFile,
			List<String> importedTypes, UMLJavadoc packageDoc, List<UMLComment> comments) {
		UMLJavadoc javadoc = generateJavadoc(cu, enumDeclaration, sourceFile);
		if(javadoc != null && javadoc.containsIgnoreCase(FREE_MARKER_GENERATED)) {
			return;
		}
		String className = enumDeclaration.getName();
		LocationInfo locationInfo = generateLocationInfo(cu, sourceFile, enumDeclaration, CodeElementType.TYPE_DECLARATION);
		boolean isPackageMemberTypeDeclaration = enumDeclaration.getParent() instanceof PsiFile;
		UMLClass umlClass = new UMLClass(packageName, className, locationInfo, isPackageMemberTypeDeclaration, importedTypes);
		umlClass.setJavadoc(javadoc);
		if(isPackageMemberTypeDeclaration) {
			umlClass.setPackageDeclarationJavadoc(packageDoc);
			for(UMLComment comment : comments) {
				if(comment.getLocationInfo().getStartLine() == 1) {
					umlClass.getPackageDeclarationComments().add(comment);
				}
			}
		}
		umlClass.setEnum(true);

		for (UMLType umlType : getUMLTypesOfReferenceList(cu, sourceFile, enumDeclaration.getImplementsList())) {
			UMLRealization umlRealization = new UMLRealization(umlClass, umlType.getClassType());
			umlClass.addImplementedInterface(umlType);
			getUmlModel().addRealization(umlRealization);
		}
    	
    	//List<EnumConstantDeclaration> enumConstantDeclarations = enumDeclaration.enumConstants();
    	//for(EnumConstantDeclaration enumConstantDeclaration : enumConstantDeclarations) {
		//	processEnumConstantDeclaration(cu, enumConstantDeclaration, sourceFile, umlClass, comments);
		//}
		
		processModifiers(cu, sourceFile, enumDeclaration, umlClass);
		
		processBodyDeclarations(cu, enumDeclaration, packageName, sourceFile, importedTypes, umlClass, packageDoc, comments);
		
		processAnonymousClassDeclarations(cu, enumDeclaration, packageName, sourceFile, className, umlClass);
		
		this.getUmlModel().addClass(umlClass);
		distributeComments(comments, locationInfo, umlClass.getComments());
	}

	private void processBodyDeclarations(PsiFile cu, PsiClass abstractTypeDeclaration, String packageName,
			String sourceFile, List<String> importedTypes, UMLClass umlClass, UMLJavadoc packageDoc, List<UMLComment> comments) {
		for (PsiElement psiElement : abstractTypeDeclaration.getChildren()) {
			if (psiElement instanceof PsiEnumConstant) {
				processEnumConstantDeclaration(cu, (PsiEnumConstant) psiElement, sourceFile, umlClass, comments);
			}
			else if (psiElement instanceof PsiField) {
				PsiField psiField = (PsiField) psiElement;
				List<UMLAttribute> attributes = processFieldDeclaration(cu, psiField, umlClass.isInterface(), sourceFile, comments);
	    		for(UMLAttribute attribute : attributes) {
	    			attribute.setClassName(umlClass.getName());
	    			umlClass.addAttribute(attribute);
	    		}
			}
			else if (psiElement instanceof PsiMethod) {
				PsiMethod psiMethod = (PsiMethod) psiElement;
				UMLOperation operation = processMethodDeclaration(cu, psiMethod, packageName, umlClass.isInterface(), sourceFile, comments);
	    		operation.setClassName(umlClass.getName());
	    		umlClass.addOperation(operation);
			}
			else if (psiElement instanceof PsiClass) {
				PsiClass psiInnerClass = (PsiClass) psiElement;
				if (psiInnerClass.isEnum()) {
					processEnumDeclaration(cu, psiInnerClass, umlClass.getName(), sourceFile, importedTypes, packageDoc, comments);
				}
				else if(psiInnerClass.isAnnotationType()) {
					//
				}
				else {
					processTypeDeclaration(cu, psiInnerClass, umlClass.getName(), sourceFile, importedTypes, packageDoc, comments);
				}
			}
		}
	}

	private void processTypeDeclaration(PsiFile cu, PsiClass typeDeclaration, String packageName, String sourceFile,
			List<String> importedTypes, UMLJavadoc packageDoc, List<UMLComment> comments) {
		UMLJavadoc javadoc = generateJavadoc(cu, typeDeclaration, sourceFile);
		if(javadoc != null && javadoc.containsIgnoreCase(FREE_MARKER_GENERATED)) {
			return;
		}
		String className = typeDeclaration.getName();
		LocationInfo locationInfo = generateLocationInfo(cu, sourceFile, typeDeclaration, CodeElementType.TYPE_DECLARATION);
		boolean isPackageMemberTypeDeclaration = typeDeclaration.getParent() instanceof PsiFile;
		UMLClass umlClass = new UMLClass(packageName, className, locationInfo, isPackageMemberTypeDeclaration, importedTypes);
		umlClass.setJavadoc(javadoc);
		if(isPackageMemberTypeDeclaration) {
			umlClass.setPackageDeclarationJavadoc(packageDoc);
			for(UMLComment comment : comments) {
				if(comment.getLocationInfo().getStartLine() == 1) {
					umlClass.getPackageDeclarationComments().add(comment);
				}
			}
		}

		if (typeDeclaration.isInterface()) {
			umlClass.setInterface(true);
			for (UMLType umlType : getUMLTypesOfReferenceList(cu, sourceFile, typeDeclaration.getExtendsList())) {
				UMLRealization umlRealization = new UMLRealization(umlClass, umlType.getClassType());
				umlClass.addImplementedInterface(umlType);
				getUmlModel().addRealization(umlRealization);
			}
		} else {
			List<UMLType> extendsList = getUMLTypesOfReferenceList(cu, sourceFile, typeDeclaration.getExtendsList());
			if (extendsList.size() == 1) {
				UMLType umlType = extendsList.get(0);
				UMLGeneralization umlGeneralization = new UMLGeneralization(umlClass, umlType.getClassType());
				umlClass.setSuperclass(umlType);
				getUmlModel().addGeneralization(umlGeneralization);
			}

			for (UMLType umlType : getUMLTypesOfReferenceList(cu, sourceFile, typeDeclaration.getImplementsList())) {
				UMLRealization umlRealization = new UMLRealization(umlClass, umlType.getClassType());
				umlClass.addImplementedInterface(umlType);
				getUmlModel().addRealization(umlRealization);
			}
		}
    	
    	processModifiers(cu, sourceFile, typeDeclaration, umlClass);

		PsiTypeParameter[] typeParameters = typeDeclaration.getTypeParameters();
		for(PsiTypeParameter typeParameter : typeParameters) {
			umlClass.addTypeParameter(processTypeParameter(cu, sourceFile, typeParameter));
    	}
    	/*
    	PsiField[] fieldDeclarations = typeDeclaration.getFields();
    	for(PsiField fieldDeclaration : fieldDeclarations) {
    		List<UMLAttribute> attributes = processFieldDeclaration(cu, fieldDeclaration, umlClass.isInterface(), sourceFile, comments);
    		for(UMLAttribute attribute : attributes) {
    			attribute.setClassName(umlClass.getName());
    			umlClass.addAttribute(attribute);
    		}
    	}
    	
    	PsiMethod[] methodDeclarations = typeDeclaration.getMethods();
    	for(PsiMethod methodDeclaration : methodDeclarations) {
    		UMLOperation operation = processMethodDeclaration(cu, methodDeclaration, packageName, umlClass.isInterface(), sourceFile, comments);
    		operation.setClassName(umlClass.getName());
    		umlClass.addOperation(operation);
    	}
    	*/
    	processAnonymousClassDeclarations(cu, typeDeclaration, packageName, sourceFile, className, umlClass);
    	
    	this.getUmlModel().addClass(umlClass);

		processBodyDeclarations(cu, typeDeclaration, packageName, sourceFile, importedTypes, umlClass, packageDoc, comments);
		distributeComments(comments, locationInfo, umlClass.getComments());
	}

	private void processAnonymousClassDeclarations(PsiFile cu, PsiClass typeDeclaration,
			String packageName, String sourceFile, String className, UMLClass umlClass) {
		AnonymousClassDeclarationVisitor visitor = new AnonymousClassDeclarationVisitor();
    	typeDeclaration.accept(visitor);
    	Set<PsiAnonymousClass> anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
    	
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    	for(PsiAnonymousClass anonymous : anonymousClassDeclarations) {
    		insertNode(anonymous, root);
    	}
    	
    	List<UMLAnonymousClass> createdAnonymousClasses = new ArrayList<>();
    	Enumeration enumeration = root.postorderEnumeration();
    	while(enumeration.hasMoreElements()) {
    		DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumeration.nextElement();
    		if(node.getUserObject() != null) {
				PsiAnonymousClass anonymous = (PsiAnonymousClass)node.getUserObject();
    			boolean operationFound = false;
    			UMLOperation matchingOperation = null;
    			UMLAttribute matchingAttribute = null;
    			List<UMLComment> comments = null;
				for(UMLOperation operation : umlClass.getOperations()) {
    				if(operation.getLocationInfo().getStartOffset() <= anonymous.getTextRange().getStartOffset() &&
    						operation.getLocationInfo().getEndOffset() >= anonymous.getTextRange().getEndOffset()) {
    					comments  = operation.getComments();
    					operationFound = true;
    					matchingOperation = operation;
    					break;
    				}
    			}
    			if(!operationFound) {
	    			for(UMLAttribute attribute : umlClass.getAttributes()) {
	    				if(attribute.getLocationInfo().getStartOffset() <= anonymous.getTextRange().getStartOffset() &&
	    						attribute.getLocationInfo().getEndOffset() >= anonymous.getTextRange().getEndOffset()) {
	    					comments = attribute.getComments();
	    					matchingAttribute = attribute;
	    					break;
	    				}
	    			}
    			}
    			if(matchingOperation != null || matchingAttribute != null) {
	    			String anonymousBinaryName = getAnonymousBinaryName(node);
	    			String anonymousCodePath = getAnonymousCodePath(anonymous);
	    			UMLAnonymousClass anonymousClass = processAnonymousClassDeclaration(cu, anonymous, packageName + "." + className, anonymousBinaryName, anonymousCodePath, sourceFile, comments);
	    			umlClass.addAnonymousClass(anonymousClass);
	    			if(matchingOperation != null) {
	    				matchingOperation.addAnonymousClass(anonymousClass);
	    			}
	    			if(matchingAttribute != null) {
	    				matchingAttribute.addAnonymousClass(anonymousClass);
	    			}
	    			for(UMLOperation operation : anonymousClass.getOperations()) {
	    				for(UMLAnonymousClass createdAnonymousClass : createdAnonymousClasses) {
	    					if(operation.getLocationInfo().subsumes(createdAnonymousClass.getLocationInfo())) {
	    						operation.addAnonymousClass(createdAnonymousClass);
	    					}
	    				}
	    			}
	    			for(UMLAttribute attribute : anonymousClass.getAttributes()) {
	    				for(UMLAnonymousClass createdAnonymousClass : createdAnonymousClasses) {
	    					if(attribute.getLocationInfo().subsumes(createdAnonymousClass.getLocationInfo())) {
	    						attribute.addAnonymousClass(createdAnonymousClass);
	    					}
	    				}
	    			}
	    			createdAnonymousClasses.add(anonymousClass);
    			}
    		}
    	}
	}

	private void processModifiers(PsiFile cu, String sourceFile, PsiClass typeDeclaration, UMLClass umlClass) {
		PsiModifierList modifiers = typeDeclaration.getModifierList();
		if(modifiers != null) {
			if (modifiers.hasExplicitModifier(PsiModifier.ABSTRACT))
				umlClass.setAbstract(true);
			if (modifiers.hasExplicitModifier(PsiModifier.STATIC))
				umlClass.setStatic(true);
			if (modifiers.hasExplicitModifier(PsiModifier.FINAL))
				umlClass.setFinal(true);

			if (modifiers.hasExplicitModifier(PsiModifier.PUBLIC))
				umlClass.setVisibility("public");
			else if (modifiers.hasExplicitModifier(PsiModifier.PROTECTED))
				umlClass.setVisibility("protected");
			else if (modifiers.hasExplicitModifier(PsiModifier.PRIVATE))
				umlClass.setVisibility("private");
			else
				umlClass.setVisibility("package");

			for (PsiAnnotation annotation : modifiers.getAnnotations()) {
				umlClass.addAnnotation(new UMLAnnotation(cu, sourceFile, annotation));
			}
		}
	}

	private UMLOperation processMethodDeclaration(PsiFile cu, PsiMethod methodDeclaration, String packageName, boolean isInterfaceMethod, String sourceFile, List<UMLComment> comments) {
		UMLJavadoc javadoc = generateJavadoc(cu, methodDeclaration, sourceFile);
		String methodName = methodDeclaration.getName();
		LocationInfo locationInfo = generateLocationInfo(cu, sourceFile, methodDeclaration, CodeElementType.METHOD_DECLARATION);
		UMLOperation umlOperation = new UMLOperation(methodName, locationInfo);
		umlOperation.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, umlOperation.getComments());
		
		if(methodDeclaration.isConstructor())
			umlOperation.setConstructor(true);

		PsiModifierList modifiers = methodDeclaration.getModifierList();
		if(modifiers != null) {
			if (modifiers.hasModifierProperty(PsiModifier.PUBLIC))
				umlOperation.setVisibility("public");
			else if (modifiers.hasExplicitModifier(PsiModifier.PROTECTED))
				umlOperation.setVisibility("protected");
			else if (modifiers.hasExplicitModifier(PsiModifier.PRIVATE))
				umlOperation.setVisibility("private");
			else if (isInterfaceMethod)
				umlOperation.setVisibility("public");
			else
				umlOperation.setVisibility("package");

			if (modifiers.hasExplicitModifier(PsiModifier.ABSTRACT))
				umlOperation.setAbstract(true);

			if (modifiers.hasExplicitModifier(PsiModifier.FINAL))
				umlOperation.setFinal(true);

			if (modifiers.hasExplicitModifier(PsiModifier.STATIC))
				umlOperation.setStatic(true);

			if (modifiers.hasExplicitModifier(PsiModifier.SYNCHRONIZED))
				umlOperation.setSynchronized(true);

			for (PsiAnnotation annotation : modifiers.getAnnotations()) {
				umlOperation.addAnnotation(new UMLAnnotation(cu, sourceFile, annotation));
			}
		}

		PsiTypeParameter[] typeParameters = methodDeclaration.getTypeParameters();
		for(PsiTypeParameter typeParameter : typeParameters) {
			umlOperation.addTypeParameter(processTypeParameter(cu, sourceFile, typeParameter));
		}

		if(!methodDeclaration.isConstructor()) {
			UMLType type = UMLTypePsiParser.extractTypeObject(cu, sourceFile, methodDeclaration.getReturnTypeElement(), methodDeclaration.getReturnType());
			UMLParameter returnParameter = new UMLParameter("return", type, "return", false);
			umlOperation.addParameter(returnParameter);
		}

		PsiParameter[] parameters = methodDeclaration.getParameterList().getParameters();
		for(PsiParameter parameter : parameters) {
			String parameterName = parameter.getName();
			UMLType type = UMLTypePsiParser.extractTypeObject(cu, sourceFile, parameter.getTypeElement(), parameter.getType());
			UMLParameter umlParameter = new UMLParameter(parameterName, type, "in", parameter.isVarArgs());
			VariableDeclaration variableDeclaration = new VariableDeclaration(cu, sourceFile, parameter, CodeElementType.SINGLE_VARIABLE_DECLARATION, parameter.isVarArgs());
			variableDeclaration.setParameter(true);
			umlParameter.setVariableDeclaration(variableDeclaration);
			umlOperation.addParameter(umlParameter);
		}

		for (UMLType umlType : getUMLTypesOfReferenceList(cu, sourceFile, methodDeclaration.getThrowsList())) {
			umlOperation.addThrownExceptionType(umlType);
		}
		
		PsiCodeBlock block = methodDeclaration.getBody();
		if(block != null) {
			OperationBody body = new OperationBody(cu, sourceFile, block, umlOperation.getParameterDeclarationList());
			umlOperation.setBody(body);
			if(block.isEmpty()) {
				umlOperation.setEmptyBody(true);
			}
		}
		else {
			umlOperation.setBody(null);
		}
		
		return umlOperation;
	}

	private void processEnumConstantDeclaration(PsiFile cu, PsiEnumConstant enumConstantDeclaration, String sourceFile, UMLClass umlClass, List<UMLComment> comments) {
		UMLJavadoc javadoc = generateJavadoc(cu, enumConstantDeclaration, sourceFile);
		LocationInfo locationInfo = generateLocationInfo(cu, sourceFile, enumConstantDeclaration, CodeElementType.ENUM_CONSTANT_DECLARATION);
		UMLEnumConstant enumConstant = new UMLEnumConstant(enumConstantDeclaration.getName(), UMLType.extractTypeObject(umlClass.getName()), locationInfo);
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, sourceFile, enumConstantDeclaration);
		enumConstant.setVariableDeclaration(variableDeclaration);
		enumConstant.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, enumConstant.getComments());
		enumConstant.setFinal(true);
		enumConstant.setStatic(true);
		enumConstant.setVisibility("public");
		PsiExpressionList argumentList = enumConstantDeclaration.getArgumentList();
		if (argumentList != null) {
			for (PsiExpression argument : argumentList.getExpressions()) {
				enumConstant.addArgument(Formatter.format(argument));
			}
		}
		enumConstant.setClassName(umlClass.getName());
		umlClass.addEnumConstant(enumConstant);
	}

	private List<UMLAttribute> processFieldDeclaration(PsiFile cu, PsiField fieldDeclaration, boolean isInterfaceField, String sourceFile, List<UMLComment> comments) {
		UMLJavadoc javadoc = generateJavadoc(cu, fieldDeclaration, sourceFile);
		List<UMLAttribute> attributes = new ArrayList<>();
		UMLType type = UMLTypePsiParser.extractTypeObject(cu, sourceFile, fieldDeclaration.getTypeElement(), fieldDeclaration.getType());
		String fieldName = fieldDeclaration.getName();
		LocationInfo locationInfo = generateLocationInfo(cu, sourceFile, fieldDeclaration, CodeElementType.FIELD_DECLARATION);
		UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo);
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, sourceFile, fieldDeclaration);
		variableDeclaration.setAttribute(true);
		umlAttribute.setVariableDeclaration(variableDeclaration);
		umlAttribute.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, umlAttribute.getComments());

		PsiModifierList modifiers = fieldDeclaration.getModifierList();
		if(modifiers != null) {
			if (modifiers.hasModifierProperty(PsiModifier.PUBLIC))
				umlAttribute.setVisibility("public");
			else if (modifiers.hasExplicitModifier(PsiModifier.PROTECTED))
				umlAttribute.setVisibility("protected");
			else if (modifiers.hasExplicitModifier(PsiModifier.PRIVATE))
				umlAttribute.setVisibility("private");
			else if (isInterfaceField)
				umlAttribute.setVisibility("public");
			else
				umlAttribute.setVisibility("package");

			if (modifiers.hasExplicitModifier(PsiModifier.FINAL))
				umlAttribute.setFinal(true);

			if (modifiers.hasExplicitModifier(PsiModifier.STATIC))
				umlAttribute.setStatic(true);

			if (modifiers.hasExplicitModifier(PsiModifier.VOLATILE))
				umlAttribute.setVolatile(true);

			if (modifiers.hasExplicitModifier(PsiModifier.TRANSIENT))
				umlAttribute.setTransient(true);
		}
		attributes.add(umlAttribute);
		return attributes;
	}
	
	private UMLAnonymousClass processAnonymousClassDeclaration(PsiFile cu, PsiAnonymousClass anonymous, String packageName, String binaryName, String codePath, String sourceFile, List<UMLComment> comments) {
		LocationInfo locationInfo = generateLocationInfo(cu, sourceFile, anonymous, CodeElementType.ANONYMOUS_CLASS_DECLARATION);
		UMLAnonymousClass anonymousClass = new UMLAnonymousClass(packageName, binaryName, codePath, locationInfo);

		for (PsiField fieldDeclaration : anonymous.getFields()) {
				List<UMLAttribute> attributes = processFieldDeclaration(cu, fieldDeclaration, false, sourceFile, comments);
	    		for(UMLAttribute attribute : attributes) {
	    			attribute.setClassName(anonymousClass.getCodePath());
	    			anonymousClass.addAttribute(attribute);
	    		}
			}
		for (PsiMethod methodDeclaration : anonymous.getMethods()) {
			UMLOperation operation = processMethodDeclaration(cu, methodDeclaration, packageName, false, sourceFile, comments);
			operation.setClassName(anonymousClass.getCodePath());
			operation.setDeclaredInAnonymousClass(true);
			anonymousClass.addOperation(operation);
		}
		distributeComments(comments, locationInfo, anonymousClass.getComments());
		return anonymousClass;
	}
	
	private void insertNode(PsiAnonymousClass childAnonymous, DefaultMutableTreeNode root) {
		Enumeration enumeration = root.postorderEnumeration();
		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childAnonymous);
		
		DefaultMutableTreeNode parentNode = root;
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			PsiAnonymousClass currentAnonymous = (PsiAnonymousClass)currentNode.getUserObject();
			if(currentAnonymous != null && isParent(childAnonymous, currentAnonymous)) {
				parentNode = currentNode;
				break;
			}
		}
		parentNode.add(childNode);
	}

	private String getAnonymousCodePath(PsiAnonymousClass anonymous) {
		String name = "";
		PsiElement parent = anonymous.getParent();
		while(parent != null) {
			if(parent instanceof PsiMethod) {
				String methodName = ((PsiMethod)parent).getName();
				if(name.isEmpty()) {
					name = methodName;
				}
				else {
					name = methodName + "." + name;
				}
			}
			else if(parent instanceof PsiField) {
				String fieldName = ((PsiField)parent).getName();
				if(name.isEmpty()) {
					name = fieldName;
				}
				else {
					name = fieldName + "." + name;
				}
			}
			else if(parent instanceof PsiLocalVariable) {
				String fieldName = ((PsiLocalVariable)parent).getName();
				if(name.isEmpty()) {
					name = fieldName;
				}
				else {
					name = fieldName + "." + name;
				}
			}
			else if(parent instanceof PsiMethodCallExpression) {
				PsiIdentifier identifier =
						PsiUtils.findFirstChildOfType(
								((PsiMethodCallExpression) parent).getMethodExpression(),
								PsiIdentifier.class
						);
				String invocationName = Formatter.format(identifier);
				if(name.isEmpty()) {
					name = invocationName;
				}
				else {
					name = invocationName + "." + name;
				}
			}
			else if(parent instanceof PsiNewExpression) {
				String invocationName = ((PsiNewExpression)parent).getType().getPresentableText();
				if(name.isEmpty()) {
					name = "new " + invocationName;
				}
				else {
					name = "new " + invocationName + "." + name;
				}
			}
			parent = parent.getParent();
		}
		return name;
	}

	private String getAnonymousBinaryName(DefaultMutableTreeNode node) {
		StringBuilder name = new StringBuilder();
		TreeNode[] path = node.getPath();
		for(int i=0; i<path.length; i++) {
			DefaultMutableTreeNode tmp = (DefaultMutableTreeNode)path[i];
			if(tmp.getUserObject() != null) {
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode)tmp.getParent();
				int index = parent.getIndex(tmp);
				name.append(index+1);
				if(i < path.length-1)
					name.append(".");
			}
		}
		return name.toString();
	}
	
	private boolean isParent(PsiElement child, PsiElement parent) {
		PsiElement current = child;
		while(current.getParent() != null) {
			if(current.getParent().equals(parent))
				return true;
			current = current.getParent();
		}
		return false;
	}

	private LocationInfo generateLocationInfo(PsiFile cu, String sourceFile, PsiElement node, CodeElementType codeElementType) {
		return new LocationInfo(cu, sourceFile, node, codeElementType);
	}
}
