package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.util.Util;

import br.ufmg.dcc.labsoft.refdetector.model.SDAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.SDEntity;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDPackage;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;
import br.ufmg.dcc.labsoft.refdetector.model.SourceRepresentation;

public class BindingsRecoverySDBuilder {
	private static final String systemFileSeparator = Matcher.quoteReplacement(File.separator);
	
	private Map<SDEntity, List<String>> postProcessReferences;
	private Map<SDType, List<String>> postProcessSupertypes;
	private Map<String, List<SourceRepresentation>> postProcessClientCode;

	private void postProcessReferences(final SDModel.Snapshot model, Map<? extends SDEntity, List<String>> referencesMap) {
	    for (Map.Entry<? extends SDEntity, List<String>> entry : referencesMap.entrySet()) {
	        final SDEntity entity = entry.getKey();
	        List<String> references = entry.getValue();
	        for (String referencedKey : references) {
	            SDEntity referenced = model.find(SDEntity.class, referencedKey);
	            if (referenced != null) {
	                entity.addReference(referenced);
	            }
	        }
	    }
	}
	private void postProcessSupertypes(final SDModel.Snapshot model) {
		for (Map.Entry<SDType, List<String>> entry : postProcessSupertypes.entrySet()) {
			final SDType type = entry.getKey();
			List<String> supertypes = entry.getValue();
			for (String supertypeKey : supertypes) {
				SDType supertype = model.find(SDType.class, supertypeKey);
				if (supertype != null) {
					supertype.addSubtype(type);
				}
			}
		}
	}
	private void postProcessClientCode(final SDModel.Snapshot model) {
	    for (Map.Entry<String, List<SourceRepresentation>> entry : postProcessClientCode.entrySet()) {
	        final String entityId = entry.getKey();
	        SDAttribute entity = model.find(SDAttribute.class, entityId);
	        if (entity != null) {
	            List<SourceRepresentation> sourceSnippets = entry.getValue();
	            entity.setClientCode(entity.assignment().combine(sourceSnippets));
	        }
	    }
	}

	public void analyze(File rootFolder, List<String> javaFiles, final SDModel.Snapshot model) {
	    postProcessReferences = new HashMap<SDEntity, List<String>>();
		postProcessSupertypes = new HashMap<SDType, List<String>>();
		postProcessClientCode = new HashMap<String, List<SourceRepresentation>>();
		final String projectRoot = rootFolder.getPath();
		final String[] emptyArray = new String[0];
		
		String[] filesArray = new String[javaFiles.size()];
		for (int i = 0; i < filesArray.length; i++) {
			filesArray[i] = rootFolder + File.separator + javaFiles.get(i).replaceAll("/", systemFileSeparator);
		}
		final String[] sourceFolders = this.inferSourceFolders(filesArray);
		final ASTParser parser = buildAstParser(sourceFolders);

		FileASTRequestor fileASTRequestor = new FileASTRequestor() { 
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				String relativePath = sourceFilePath.substring(projectRoot.length() + 1).replaceAll(systemFileSeparator, "/");
//				IProblem[] problems = ast.getProblems();
//				if (problems.length > 0) {
//					System.out.println("problems");
//				}
				try {
					char[] charArray = Util.getFileCharContent(new File(sourceFilePath), null);
					processCompilationUnit(relativePath, charArray, ast, model);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
		parser.createASTs((String[]) filesArray, null, emptyArray, fileASTRequestor, null);
		
		postProcessReferences(model, postProcessReferences);
		postProcessReferences = null;
		postProcessSupertypes(model);
		postProcessSupertypes = null;
		postProcessClientCode(model);
		postProcessClientCode = null;
	}

	private static ASTParser buildAstParser(String[] sourceFolders) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setEnvironment(new String[0], sourceFolders, null, true);
		//parser.setEnvironment(new String[0], new String[]{"tmp\\refactoring-toy-example\\src"}, null, false);
		return parser;
	}

	protected void processCompilationUnit(String sourceFilePath, char[] fileContent, CompilationUnit compilationUnit, SDModel.Snapshot model) {
		PackageDeclaration packageDeclaration = compilationUnit.getPackage();
		String packageName = "";
		if (packageDeclaration != null) {
			packageName = packageDeclaration.getName().getFullyQualifiedName();
		}
		SDPackage sdPackage = model.getOrCreatePackage(packageName);
		
		BindingsRecoveryAstVisitor visitor = new BindingsRecoveryAstVisitor(model, sourceFilePath, fileContent, sdPackage, postProcessReferences, postProcessSupertypes, postProcessClientCode);
		compilationUnit.accept(visitor);
		
//		List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
//        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
//        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
//        		TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
//        		processTypeDeclaration(topLevelTypeDeclaration, packageName, sourceFilePath);
//        	}
//        }
	}

	private String[] inferSourceFolders(String[] filesArray) {
		Set<String> sourceFolders = new TreeSet<String>();
		nextFile: for (String file : filesArray) {
			for (String sourceFolder : sourceFolders) {
				if (file.startsWith(sourceFolder)) {
					continue nextFile;
				}
			}
			String otherSourceFolder = extractSourceFolderFromPath(file);
			if (otherSourceFolder != null) {
				sourceFolders.add(otherSourceFolder);
//				System.out.print("source folder: ");
//				System.out.println(otherSourceFolder);
			}
		}
		return sourceFolders.toArray(new String[sourceFolders.size()]);
	}

	private String extractSourceFolderFromPath(String sourceFilePath) {
		try (BufferedReader scanner = new BufferedReader(new FileReader(sourceFilePath))) {
			String lineFromFile;
			while ((lineFromFile = scanner.readLine()) != null) {
				if (lineFromFile.startsWith("package ")) { 
					// a match!
					//System.out.print("package declaration: ");
					String packageName = lineFromFile.substring(8, lineFromFile.indexOf(';'));
					//System.out.println(packageName);
					
					String packagePath = packageName.replace('.', File.separator.charAt(0));
					int indexOfPackagePath = sourceFilePath.lastIndexOf(packagePath + File.separator);
					if (indexOfPackagePath >= 0) {
						return sourceFilePath.substring(0, indexOfPackagePath - 1);
					}
					return null;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}
	
	private String getTypeName(Type type) {
		ITypeBinding binding = type.resolveBinding();
		if (binding != null) {
			return binding.getQualifiedName();
		}
		return type.toString();
	}
	
	/////////////////

	private void processTypeDeclaration(TypeDeclaration typeDeclaration, String packageName, String sourceFile) {
		String className = typeDeclaration.getName().getFullyQualifiedName();
//		System.out.println("Type: " + className);
//		UMLClass umlClass = new UMLClass(packageName, className, null, sourceFile, typeDeclaration.isPackageMemberTypeDeclaration());
//		//UMLClass bytecodeClass = bytecodeModel.getClass(umlClass.getName());
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
    	Type superclassType = typeDeclaration.getSuperclassType();
    	if (superclassType != null) {
    		String typeName = this.getTypeName(superclassType);
//    		System.out.println("Superclass: " + typeName);
    	}
//    	
//    	List<Type> superInterfaceTypes = typeDeclaration.superInterfaceTypes();
//    	for(Type interfaceType : superInterfaceTypes) {
//    		UMLRealization umlRealization = new UMLRealization(umlClass.getName(), this.getTypeName(interfaceType));
//    		/*UMLRealization bytecodeRealization = bytecodeModel.matchRealization(umlRealization);
//    		if(bytecodeRealization != null) {
//    			umlRealization.setSupplier(bytecodeRealization.getSupplier());
//    		}*/
//    		getUmlModel().addRealization(umlRealization);
//    	}
//    	
//    	FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
//    	for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
//    		List<UMLAttribute> attributes = processFieldDeclaration(fieldDeclaration/*, bytecodeClass*/);
//    		for(UMLAttribute attribute : attributes) {
//    			attribute.setClassName(umlClass.getName());
//    			umlClass.addAttribute(attribute);
//    		}
//    	}
//    	
//    	MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
//    	for(MethodDeclaration methodDeclaration : methodDeclarations) {
//    		UMLOperation operation = processMethodDeclaration(methodDeclaration, packageName, className/*, bytecodeClass*/);
//    		operation.setClassName(umlClass.getName());
//    		umlClass.addOperation(operation);
//    	}
//    	
//    	AnonymousClassDeclarationVisitor visitor = new AnonymousClassDeclarationVisitor();
//    	typeDeclaration.accept(visitor);
//    	Set<AnonymousClassDeclaration> anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
//    	
//    	DefaultMutableTreeNode root = new DefaultMutableTreeNode();
//    	for(AnonymousClassDeclaration anonymous : anonymousClassDeclarations) {
//    		insertNode(anonymous, root);
//    	}
//    	
//    	Enumeration<DefaultMutableTreeNode> enumeration = root.preorderEnumeration();
//    	while(enumeration.hasMoreElements()) {
//    		DefaultMutableTreeNode node = enumeration.nextElement();
//    		if(node.getUserObject() != null) {
//    			AnonymousClassDeclaration anonymous = (AnonymousClassDeclaration)node.getUserObject();
//    			String anonymousName = getAnonymousName(node);
//    			processAnonymousClassDeclaration(anonymous, packageName + "." + className, anonymousName, sourceFile);
//    		}
//    	}
//    	
//    	this.getUmlModel().addClass(umlClass);
//		
//		TypeDeclaration[] types = typeDeclaration.getTypes();
//		for(TypeDeclaration type : types) {
//			processTypeDeclaration(type, umlClass.getName(), sourceFile);
//		}
	}

//	private UMLOperation processMethodDeclaration(MethodDeclaration methodDeclaration, String packageName, String className/*, UMLClass bytecodeClass*/) {
//		String methodName = methodDeclaration.getName().getFullyQualifiedName();
//		final IMethodBinding binding = methodDeclaration.resolveBinding();
//		UMLOperation umlOperation;
//		if (binding == null) {
//			umlOperation = new UMLOperation(methodName, null);
//			//System.out.println(String.format("WARN null binding: %s", ASTUtils.getKey(packageName, className, methodDeclaration)));
//		} else {
//			umlOperation = new UMLOperation(methodName, ASTUtils.getKey(binding));
//		}
//		//umlOperation.setClassName(umlClass.getName());
//		if(methodDeclaration.isConstructor())
//			umlOperation.setConstructor(true);
//		
//		int methodModifiers = methodDeclaration.getModifiers();
//		if((methodModifiers & Modifier.PUBLIC) != 0)
//			umlOperation.setVisibility("public");
//		else if((methodModifiers & Modifier.PROTECTED) != 0)
//			umlOperation.setVisibility("protected");
//		else if((methodModifiers & Modifier.PRIVATE) != 0)
//			umlOperation.setVisibility("private");
//		else
//			umlOperation.setVisibility("package");
//		
//		if((methodModifiers & Modifier.ABSTRACT) != 0)
//			umlOperation.setAbstract(true);
//		
//		if((methodModifiers & Modifier.FINAL) != 0)
//			umlOperation.setFinal(true);
//		
//		if((methodModifiers & Modifier.STATIC) != 0)
//			umlOperation.setStatic(true);
//		
//		Block block = methodDeclaration.getBody();
//		if(block != null) {
//			OperationBody body = new OperationBody(block);
//			umlOperation.setBody(body);
//			if(block.statements().size() == 0) {
//				umlOperation.setEmptyBody(true);
//			}
//		}
//		else {
//			umlOperation.setBody(null);
//		}
//		
//		Type returnType = methodDeclaration.getReturnType2();
//		if(returnType != null) {
//			UMLType type = UMLType.extractTypeObject(getTypeName(returnType));
//			UMLParameter returnParameter = new UMLParameter("return", type, "return");
//			umlOperation.addParameter(returnParameter);
//		}
//		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
//		for(SingleVariableDeclaration parameter : parameters) {
//			Type parameterType = parameter.getType();
//			String parameterName = parameter.getName().getFullyQualifiedName();
//			UMLType type = UMLType.extractTypeObject(getTypeName(parameterType));
//			UMLParameter umlParameter = new UMLParameter(parameterName, type, "in");
//			umlOperation.addParameter(umlParameter);
//		}
//		
//		/*if(bytecodeClass != null) {
//			UMLOperation bytecodeOperation = bytecodeClass.matchOperation(umlOperation);
//			if(bytecodeOperation != null) {
//				int i = 0;
//				for(UMLParameter bytecodeParameter : bytecodeOperation.getParameters()) {
//					umlOperation.getParameters().get(i).setType(bytecodeParameter.getType());
//					i++;
//				}
//				umlOperation.setAccessedMembers(new LinkedHashSet<AccessedMember>(bytecodeOperation.getAccessedMembers()));
//			}
//		}*/
//		
//		return umlOperation;
//	}
//
//
//	private List<UMLAttribute> processFieldDeclaration(FieldDeclaration fieldDeclaration/*, UMLClass bytecodeClass*/) {
//		List<UMLAttribute> attributes = new ArrayList<UMLAttribute>();
//		Type fieldType = fieldDeclaration.getType();
//		UMLType type = UMLType.extractTypeObject(getTypeName(fieldType));
//		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
//		for(VariableDeclarationFragment fragment : fragments) {
//			String fieldName = fragment.getName().getFullyQualifiedName();
//			UMLAttribute umlAttribute = new UMLAttribute(fieldName, type);
//			//umlAttribute.setClassName(umlClass.getName());
//			
//			int fieldModifiers = fieldDeclaration.getModifiers();
//			if((fieldModifiers & Modifier.PUBLIC) != 0)
//				umlAttribute.setVisibility("public");
//			else if((fieldModifiers & Modifier.PROTECTED) != 0)
//				umlAttribute.setVisibility("protected");
//			else if((fieldModifiers & Modifier.PRIVATE) != 0)
//				umlAttribute.setVisibility("private");
//			else
//				umlAttribute.setVisibility("package");
//			
//			if((fieldModifiers & Modifier.FINAL) != 0)
//				umlAttribute.setFinal(true);
//			
//			if((fieldModifiers & Modifier.STATIC) != 0)
//				umlAttribute.setStatic(true);
//			
//			/*if(bytecodeClass != null) {
//				UMLAttribute bytecodeAttribute = bytecodeClass.matchAttribute(umlAttribute);
//				if(bytecodeAttribute != null)
//					umlAttribute.setType(bytecodeAttribute.getType());
//			}*/
//			
//			attributes.add(umlAttribute);
//		}
//		return attributes;
//	}
//	
//	private void processAnonymousClassDeclaration(AnonymousClassDeclaration anonymous, String packageName, String className, String sourceFile) {
//		List<BodyDeclaration> bodyDeclarations = anonymous.bodyDeclarations();
//		
//		UMLAnonymousClass anonymousClass = new UMLAnonymousClass(packageName, className, sourceFile);
//		//UMLClass bytecodeClass = bytecodeModel.getClass(anonymousClass.getName());
//		
//		for(BodyDeclaration bodyDeclaration : bodyDeclarations) {
//			if(bodyDeclaration instanceof FieldDeclaration) {
//				FieldDeclaration fieldDeclaration = (FieldDeclaration)bodyDeclaration;
//				List<UMLAttribute> attributes = processFieldDeclaration(fieldDeclaration/*, bytecodeClass*/);
//	    		for(UMLAttribute attribute : attributes) {
//	    			attribute.setClassName(anonymousClass.getName());
//	    			anonymousClass.addAttribute(attribute);
//	    		}
//			}
//			else if(bodyDeclaration instanceof MethodDeclaration) {
//				MethodDeclaration methodDeclaration = (MethodDeclaration)bodyDeclaration;
//				UMLOperation operation = processMethodDeclaration(methodDeclaration, packageName, className/*, bytecodeClass*/);
//				operation.setClassName(anonymousClass.getName());
//				anonymousClass.addOperation(operation);
//			}
//		}
//		
//		this.getUmlModel().addAnonymousClass(anonymousClass);
//	}
//	
//	private void insertNode(AnonymousClassDeclaration childAnonymous, DefaultMutableTreeNode root) {
//		Enumeration<DefaultMutableTreeNode> enumeration = root.postorderEnumeration();
//		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childAnonymous);
//		
//		DefaultMutableTreeNode parentNode = root;
//		while(enumeration.hasMoreElements()) {
//			DefaultMutableTreeNode currentNode = enumeration.nextElement();
//			AnonymousClassDeclaration currentAnonymous = (AnonymousClassDeclaration)currentNode.getUserObject();
//			if(currentAnonymous != null && isParent(childAnonymous, currentAnonymous)) {
//				parentNode = currentNode;
//				break;
//			}
//		}
//		parentNode.add(childNode);
//	}
//	
//	private String getAnonymousName(DefaultMutableTreeNode node) {
//		StringBuilder name = new StringBuilder();
//		TreeNode[] path = node.getPath();
//		for(int i=0; i<path.length; i++) {
//			DefaultMutableTreeNode tmp = (DefaultMutableTreeNode)path[i];
//			if(tmp.getUserObject() != null) {
//				DefaultMutableTreeNode parent = (DefaultMutableTreeNode)tmp.getParent();
//				int index = parent.getIndex(tmp);
//				name.append(index+1);
//				if(i < path.length-1)
//					name.append(".");
//			}
//		}
//		return name.toString();
//	}
//	
//	private boolean isParent(ASTNode child, ASTNode parent) {
//		ASTNode current = child;
//		while(current.getParent() != null) {
//			if(current.getParent().equals(parent))
//				return true;
//			current = current.getParent();
//		}
//		return false;
//	}
	
	
}
