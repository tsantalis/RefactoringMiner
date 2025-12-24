package gr.uom.java.xmi;

import static org.jetbrains.kotlin.lexer.KtTokens.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtAnonymousInitializer;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtPackageDirective;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.KtTypeReference;

import com.github.gumtreediff.gen.treesitterng.KotlinTreeSitterNgTreeGenerator;
import com.github.gumtreediff.tree.TreeContext;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class KotlinFileProcessor {
	private static final Pattern LEAD_WHITE_SPACE_JAVADOC = Pattern.compile("^\s+\\*", Pattern.MULTILINE);
	private UMLModel umlModel;

	public KotlinFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	private List<UMLComment> extractInternalComments(KtFile cu, String sourceFolder, String sourceFile, String javaFileContent) {
		List<PsiComment> astComments = new ArrayList<>();
		cu.accept(new PsiRecursiveElementVisitor() {
			@Override
			public void visitElement(PsiElement element) {
				super.visitElement(element);
				if (element instanceof PsiComment comment) {
					astComments.add(comment);
				}
			}
		});
		List<UMLComment> comments = new ArrayList<UMLComment>();
		for(PsiComment comment : astComments) {
			LocationInfo locationInfo = null;
			if(comment.getText().startsWith("//")) {
				locationInfo = new LocationInfo(cu, sourceFolder, sourceFile, comment, CodeElementType.LINE_COMMENT);
			}
			else if(comment.getText().startsWith("/*")) {
				locationInfo = new LocationInfo(cu, sourceFolder, sourceFile, comment, CodeElementType.BLOCK_COMMENT);
			}
			if(locationInfo != null) {
				int start = locationInfo.getStartOffset();
				int end = locationInfo.getEndOffset();
				String text = javaFileContent.substring(start, end);
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
					(commentLocationInfo.startsAtTheEndLineOf(codeElementLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
					(codeElementLocationInfo.nextLine(commentLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
					(codeElementComments.size() > 0 && codeElementComments.get(0).getLocationInfo().nextLine(commentLocationInfo))) {
				codeElementComments.add(0, comment);
			}
			if(commentLocationInfo.nextLine(codeElementLocationInfo) || commentLocationInfo.rightAfterNextLine(codeElementLocationInfo)) {
				comment.addPreviousLocation(codeElementLocationInfo);
			}
		}
		compilationUnitComments.removeAll(codeElementComments);
	}

	private UMLJavadoc generateDocComment(KtFile cu, String sourceFolder, String filePath, String fileContent, KDoc javaDoc) {
		UMLJavadoc doc = null;
		if (javaDoc != null) {
			LocationInfo locationInfo = new LocationInfo(cu, sourceFolder, filePath, javaDoc, CodeElementType.JAVADOC);
			int start = locationInfo.getStartOffset();
			int end = locationInfo.getEndOffset();
			String text = fileContent.substring(start, end);
			Matcher matcher = LEAD_WHITE_SPACE_JAVADOC.matcher(text); 
			StringBuilder sb = new StringBuilder(); 
			while (matcher.find()) { 
				matcher.appendReplacement(sb, " \\*"); 
			} 
			matcher.appendTail(sb); 
			String trimLeadWhiteSpace = sb.toString();
			doc = new UMLJavadoc(trimLeadWhiteSpace, locationInfo);
			//KDocSection tag = javaDoc.getDefaultSection();
			//LocationInfo tagLocationInfo = new LocationInfo(cu, sourceFolder, filePath, tag, CodeElementType.TAG_ELEMENT);
			//UMLTagElement tagElement = new UMLTagElement(tag.getName(), tagLocationInfo);
			// TODO process contents
			//doc.addTag(tagElement);
		}
		return doc;
	}

	public void processKotlinFile(String filePath, String fileContent, boolean astDiff, PsiFileFactoryImpl factory) {
		PsiFile psiFile = factory.createFileFromText(filePath, KotlinLanguage.INSTANCE, fileContent);
		KtFile ktFile = (KtFile)psiFile;
		if (astDiff) {
			ByteArrayInputStream is = new ByteArrayInputStream(fileContent.getBytes());
			try {
				TreeContext treeContext = new KotlinTreeSitterNgTreeGenerator().generateFrom().stream(is);
				this.umlModel.getTreeContextMap().put(filePath, treeContext);
			}
			catch(Exception e) {

			}
		}
		
		String packageName = "";
		String sourceFolder = "";
		UMLPackage umlPackage = null;
		KtPackageDirective packageDirective = ktFile.getPackageDirective();
		if(packageDirective != null) {
			packageName = packageDirective.getQualifiedName();
			int index = filePath.indexOf(packageName.replace('.', '/'));
			if(index != -1) {
				sourceFolder = filePath.substring(0, index);
			}
			LocationInfo packageLocationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, packageDirective, CodeElementType.PACKAGE_DECLARATION);
			umlPackage = new UMLPackage(packageName, packageLocationInfo);
		}
		
		List<UMLImport> importedTypes = new ArrayList<UMLImport>();
		KtImportList importList = ktFile.getImportList();
		if (importList != null) {
			for (KtImportDirective importDeclaration : importList.getImports()) {
				FqName fqName = importDeclaration.getImportedFqName();
				String importName = fqName == null ? null : fqName.asString();
				if (importName != null) {
					LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, importDeclaration, CodeElementType.IMPORT_DECLARATION);
					// kotlin does not have static keyword for method imports, you can import directly a method
					UMLImport imported = new UMLImport(importName, importDeclaration.isAllUnder(), false, locationInfo);
					importedTypes.add(imported);
				}
			}
		}
		List<UMLComment> comments = extractInternalComments(ktFile, sourceFolder, filePath, fileContent);
		this.umlModel.getCommentMap().put(filePath, comments);
		List<KtNamedFunction> topLevelFunctions = new ArrayList<>();
		List<KtProperty> topLevelProperties = new ArrayList<>();
		for (PsiElement psiElement : ktFile.getChildren()) {
			if (psiElement instanceof KtObjectDeclaration objectDeclaration) {
				// TODO process object declaration
			}
			else if (psiElement instanceof KtClass ktClass) {
				if (ktClass.isEnum()) {
					// TODO process enum declaration
				} else {
					UMLClass umlClass = processClassDeclaration(ktFile, ktClass, umlPackage, packageName, sourceFolder, filePath, fileContent, importedTypes, comments);
					umlModel.addClass(umlClass);
				}
			}
			else if (psiElement instanceof KtNamedFunction function) {
				topLevelFunctions.add(function);
			}
			else if (psiElement instanceof KtProperty property) {
				topLevelProperties.add(property);
			}
		}
		if (topLevelFunctions.size() > 0 || topLevelProperties.size() > 0) {
			LocationInfo locationInfo = new LocationInfo(ktFile, sourceFolder, filePath, ktFile, CodeElementType.TYPE_DECLARATION);
			String baseFileName = ktFile.getName();
			if (baseFileName.endsWith(".kt")) {
				baseFileName = baseFileName.substring(0, baseFileName.length() - 3);
			}
			String moduleName = packageName + baseFileName;
			UMLClass moduleClass = new UMLClass(moduleName, "module", locationInfo, true, importedTypes);
			moduleClass.setModule(true);
			moduleClass.setPackageDeclaration(umlPackage);
			moduleClass.setVisibility(Visibility.PUBLIC);
			
			for(KtProperty property : topLevelProperties) {
				UMLAttribute attribute = processFieldDeclaration(ktFile, property, sourceFolder, filePath, fileContent, comments, locationInfo);
				attribute.setClassName(moduleClass.getName());
				moduleClass.addAttribute(attribute);
			}
			for(KtNamedFunction function : topLevelFunctions) {
				UMLOperation operation = processFunctionDeclaration(ktFile, function, sourceFolder, filePath, fileContent, moduleClass.getAttributes(), comments);
				operation.setClassName(moduleClass.getName());
				moduleClass.addOperation(operation);
			}
			distributeComments(comments, locationInfo, moduleClass.getComments());
			umlModel.addClass(moduleClass);
		}
	}

	private UMLClass processClassDeclaration(KtFile ktFile, KtClass ktClass, UMLPackage umlPackage, String packageName, String sourceFolder, String filePath, String fileContent, List<UMLImport> importedTypes, List<UMLComment> comments) {
		LocationInfo locationInfo = new LocationInfo(ktFile, sourceFolder, filePath, ktClass, CodeElementType.TYPE_DECLARATION);
		UMLClass umlClass = new UMLClass(packageName, ktClass.getName(), locationInfo, true, importedTypes);
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, ktClass.getDocComment());
		umlClass.setJavadoc(javadoc);
		LocationInfo lastImportLocationInfo = importedTypes.size() > 0 ? importedTypes.get(importedTypes.size()-1).getLocationInfo() : null;
		if(ktFile.getName().endsWith(ktClass.getName() + ".kt")) {
			umlClass.setPackageDeclaration(umlPackage);
			List<KtDeclaration> declarations = ktFile.getDeclarations().stream()
					.filter(type -> type instanceof KtClass)
					.collect(Collectors.toList());
			boolean isFirstType = declarations.get(0).equals(ktClass);
			boolean isLastType = declarations.get(declarations.size()-1).equals(ktClass);
			for(UMLComment comment : comments) {
				if(umlPackage != null && umlPackage.getLocationInfo().before(comment.getLocationInfo()) && isFirstType && comment.getLocationInfo().before(locationInfo)) {
					if(lastImportLocationInfo != null && lastImportLocationInfo.before(comment.getLocationInfo()) && !lastImportLocationInfo.sameLine(comment.getLocationInfo()))
						umlClass.getComments().add(comment);
				}
				if(comment.getLocationInfo().before(locationInfo) && !locationInfo.nextLine(comment.getLocationInfo())) {
					umlClass.getPackageDeclarationComments().add(comment);
				}
				else if(isLastType && locationInfo.getEndLine() < comment.getLocationInfo().getStartLine()) {
					umlClass.getPackageDeclarationComments().add(comment);
				}
			}
			comments.removeAll(umlClass.getPackageDeclarationComments());
			comments.removeAll(umlClass.getComments());
		}
		
		umlClass.setVisibility(Visibility.PUBLIC);
		KtModifierList modifierList = ktClass.getModifierList();
		// default visibility in Kotlin is public
		umlClass.setVisibility(Visibility.PUBLIC);
		if(modifierList != null) {
			for (PsiElement modifier : modifierList.getChildren()) {
				if (modifier instanceof KtAnnotationEntry annotationEntry) {
					umlClass.addAnnotation(new UMLAnnotation(ktFile, sourceFolder, filePath, annotationEntry, fileContent));
				}
			}
			if (modifierList.hasModifier(PUBLIC_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PUBLIC_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setVisibility(Visibility.PUBLIC);
			}
			if (modifierList.hasModifier(PROTECTED_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PROTECTED_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setVisibility(Visibility.PROTECTED);
			}
			if (modifierList.hasModifier(PRIVATE_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PRIVATE_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setVisibility(Visibility.PRIVATE);
			}
			if (modifierList.hasModifier(INTERNAL_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(INTERNAL_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setVisibility(Visibility.INTERNAL);
			}
			if (modifierList.hasModifier(OPEN_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(OPEN_KEYWORD));
				umlClass.addModifier(modifier);
			}
			if (modifierList.hasModifier(FINAL_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(FINAL_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setFinal(true);
			}
			if (modifierList.hasModifier(ABSTRACT_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(ABSTRACT_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setAbstract(true);
			}
			if (modifierList.hasModifier(SEALED_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(SEALED_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setSealed(true);
			}
			if (modifierList.hasModifier(DATA_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(DATA_KEYWORD));
				umlClass.addModifier(modifier);
			}
			if (modifierList.hasModifier(INNER_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(INNER_KEYWORD));
				umlClass.addModifier(modifier);
			}
			if (modifierList.hasModifier(COMPANION_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(COMPANION_KEYWORD));
				umlClass.addModifier(modifier);
			}
		}
		List<KtTypeParameter> typeParameters = ktClass.getTypeParameters();
		for (KtTypeParameter typeParameter : typeParameters) {
			LocationInfo typeParameterLocation = generateLocationInfo(ktFile, sourceFolder, filePath, typeParameter, CodeElementType.TYPE_PARAMETER);
			UMLTypeParameter umlTypeParameter = new UMLTypeParameter(typeParameter.getName(), typeParameterLocation);
			KtTypeReference typeBounds = typeParameter.getExtendsBound();
			if (typeBounds != null) {
				umlTypeParameter.addTypeBound(
						UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, typeBounds, 0));
			}
			KtModifierList typeParameterModifiers = typeParameter.getModifierList();
			if (typeParameterModifiers != null) {
				for (PsiElement modifier : typeParameterModifiers.getChildren()) {
					if (modifier instanceof KtAnnotationEntry annotationEntry) {
						umlTypeParameter.addAnnotation(new UMLAnnotation(ktFile, sourceFolder, filePath, annotationEntry, fileContent));
					}
				}
			}
			umlClass.addTypeParameter(umlTypeParameter);
		}
		if(ktClass.getPrimaryConstructor() != null) {
			LocationInfo primaryConstructorLocation = generateLocationInfo(ktFile, sourceFolder, filePath, ktClass.getPrimaryConstructor(), CodeElementType.PRIMARY_CONSTRUCTOR);
			PrimaryConstructor primaryConstructor = new PrimaryConstructor(primaryConstructorLocation, ktClass.getName(), umlClass.getName());
			for(KtParameter parameter : ktClass.getPrimaryConstructorParameters()) {
				KtTypeReference typeReference = parameter.getTypeReference();
				String parameterName = parameter.getName();
				UMLType type = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, typeReference, 0);
				if (parameter.isVarArg()) {
					type.setVarargs();
				}
				UMLParameter umlParameter = new UMLParameter(parameterName, type, "in", parameter.isVarArg());
				VariableDeclaration variableDeclaration =
						new VariableDeclaration(ktFile, sourceFolder, filePath, parameter, primaryConstructor, new LinkedHashMap<>(), fileContent, umlClass.getLocationInfo());
				if(parameter.hasValOrVar()) {
					variableDeclaration.setAttribute(true);
					UMLAttribute umlAttribute = new UMLAttribute(parameterName, type, variableDeclaration.getLocationInfo());
					KtModifierList parameterModifierList = parameter.getModifierList();
					if(parameterModifierList != null) {
						if (parameterModifierList.hasModifier(PUBLIC_KEYWORD)) {
							umlAttribute.setVisibility(Visibility.PUBLIC);
						} else if (parameterModifierList.hasModifier(PROTECTED_KEYWORD)) {
							umlAttribute.setVisibility(Visibility.PROTECTED);
						} else if (parameterModifierList.hasModifier(PRIVATE_KEYWORD)) {
							umlAttribute.setVisibility(Visibility.PRIVATE);
						} else if (parameterModifierList.hasModifier(INTERNAL_KEYWORD)) {
							umlAttribute.setVisibility(Visibility.INTERNAL);
						}
					}
					umlAttribute.setVariableDeclaration(variableDeclaration);
					umlClass.addAttribute(umlAttribute);
				}
				else {
					variableDeclaration.setParameter(true);
				}
				umlParameter.setVariableDeclaration(variableDeclaration);
				primaryConstructor.addParameter(umlParameter);
			}
			umlClass.setPrimaryConstructorParameter(primaryConstructor);
		}
		KtClassBody classBody = ktClass.getBody();
		if(classBody != null) {
			for(KtProperty property : classBody.getProperties()) {
				UMLAttribute attribute = processFieldDeclaration(ktFile, property, sourceFolder, filePath, fileContent, comments, locationInfo);
				attribute.setClassName(umlClass.getName());
				umlClass.addAttribute(attribute);
			}
			for(KtAnonymousInitializer initializer : classBody.getAnonymousInitializers()) {
				UMLInitializer umlInitializer = processInitializer(ktFile, initializer, sourceFolder, filePath, fileContent, umlClass.getAttributes(), comments, umlClass.getNonQualifiedName());
				umlInitializer.setClassName(umlClass.getName());
				umlClass.addInitializer(umlInitializer);
			}
			for(KtSecondaryConstructor constructor : classBody.getSecondaryConstructors$psi_api()) {
				LocationInfo constructorLocationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, constructor, CodeElementType.METHOD_DECLARATION);
				UMLOperation umlConstructor = new UMLOperation(ktClass.getName(), constructorLocationInfo);
				umlConstructor.setConstructor(true);
				umlConstructor.setVisibility(Visibility.PUBLIC);
				UMLJavadoc constructorJavadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, constructor.getDocComment());
				umlConstructor.setJavadoc(constructorJavadoc);
				distributeComments(comments, locationInfo, umlConstructor.getComments());
				List<KtParameter> parameters = constructor.getValueParameters();
				for (KtParameter parameter : parameters) {
					KtTypeReference typeReference = parameter.getTypeReference();
					String parameterName = parameter.getName();
					UMLType type = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, typeReference, 0);
					if (parameter.isVarArg()) {
						type.setVarargs();
					}
					UMLParameter umlParameter = new UMLParameter(parameterName, type, "in", parameter.isVarArg());
					VariableDeclaration variableDeclaration =
							new VariableDeclaration(ktFile, sourceFolder, filePath, parameter, umlConstructor, new LinkedHashMap<>(), fileContent, umlConstructor.getLocationInfo());
					if(parameter.hasValOrVar()) {
						variableDeclaration.setAttribute(true);
						UMLAttribute umlAttribute = new UMLAttribute(parameterName, type, variableDeclaration.getLocationInfo());
						umlAttribute.setVisibility(Visibility.PUBLIC);
						KtModifierList parameterModifierList = parameter.getModifierList();
						if(parameterModifierList != null) {
							if (parameterModifierList.hasModifier(PUBLIC_KEYWORD)) {
								umlAttribute.setVisibility(Visibility.PUBLIC);
							} else if (parameterModifierList.hasModifier(PROTECTED_KEYWORD)) {
								umlAttribute.setVisibility(Visibility.PROTECTED);
							} else if (parameterModifierList.hasModifier(PRIVATE_KEYWORD)) {
								umlAttribute.setVisibility(Visibility.PRIVATE);
							} else if (parameterModifierList.hasModifier(INTERNAL_KEYWORD)) {
								umlAttribute.setVisibility(Visibility.INTERNAL);
							}
						}
						umlAttribute.setVariableDeclaration(variableDeclaration);
						umlClass.addAttribute(umlAttribute);
					}
					else {
						variableDeclaration.setParameter(true);
					}
					umlParameter.setVariableDeclaration(variableDeclaration);
					umlConstructor.addParameter(umlParameter);
				}
				if (constructor.getBodyBlockExpression() != null) {
					OperationBody operationBody = new OperationBody(ktFile, sourceFolder, filePath, constructor.getBodyBlockExpression(), umlConstructor, umlClass.getAttributes(), fileContent);
					umlConstructor.setBody(operationBody);
				}
				umlConstructor.setClassName(umlClass.getName());
				umlClass.addOperation(umlConstructor);
			}
			for(KtNamedFunction function : classBody.getFunctions()) {
				UMLOperation operation = processFunctionDeclaration(ktFile, function, sourceFolder, filePath, fileContent, umlClass.getAttributes(), comments);
				operation.setClassName(umlClass.getName());
				umlClass.addOperation(operation);
			}
		}
		distributeComments(comments, locationInfo, umlClass.getComments());
		return umlClass;
	}

	private UMLInitializer processInitializer(KtFile ktFile, KtAnonymousInitializer initializer, String sourceFolder, String filePath, String fileContent, List<UMLAttribute> attributes, List<UMLComment> comments, String name) {
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, initializer, CodeElementType.INITIALIZER);
		UMLInitializer umlInitializer = new UMLInitializer(name, locationInfo);
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, initializer.getDocComment());
		umlInitializer.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, umlInitializer.getComments());
		if (initializer.getBody() != null) {
			if(initializer.getBody() instanceof KtBlockExpression block) {
				OperationBody operationBody = new OperationBody(ktFile, sourceFolder, filePath, block, umlInitializer, attributes, fileContent);
				umlInitializer.setBody(operationBody);
			}
		}
		return umlInitializer;
	}

	private UMLOperation processFunctionDeclaration(KtFile ktFile, KtNamedFunction function, String sourceFolder, String filePath, String fileContent, List<UMLAttribute> attributes, List<UMLComment> comments) {
		String methodName = function.getName();
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, function, CodeElementType.METHOD_DECLARATION);
		UMLOperation umlOperation = new UMLOperation(methodName, locationInfo);
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, function.getDocComment());
		umlOperation.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, umlOperation.getComments());
		
		KtModifierList modifierList = function.getModifierList();
		// default visibility in Kotlin is public
		umlOperation.setVisibility(Visibility.PUBLIC);
		if(modifierList != null) {
			for (PsiElement modifier : modifierList.getChildren()) {
				if (modifier instanceof KtAnnotationEntry annotationEntry) {
					umlOperation.addAnnotation(new UMLAnnotation(ktFile, sourceFolder, filePath, annotationEntry, fileContent));
				}
			}
			if (modifierList.hasModifier(PUBLIC_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PUBLIC_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setVisibility(Visibility.PUBLIC);
			}
			if (modifierList.hasModifier(PROTECTED_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PROTECTED_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setVisibility(Visibility.PROTECTED);
			}
			if (modifierList.hasModifier(PRIVATE_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PRIVATE_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setVisibility(Visibility.PRIVATE);
			}
			if (modifierList.hasModifier(INTERNAL_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(INTERNAL_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setVisibility(Visibility.INTERNAL);
			}
			if (modifierList.hasModifier(OPEN_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(OPEN_KEYWORD));
				umlOperation.addModifier(modifier);
			}
			if (modifierList.hasModifier(OVERRIDE_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(OVERRIDE_KEYWORD));
				umlOperation.addModifier(modifier);
			}
			if (modifierList.hasModifier(INLINE_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(INLINE_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setInline(true);
			}
			if (modifierList.hasModifier(ABSTRACT_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(ABSTRACT_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setAbstract(true);
			}
		}
		List<KtTypeParameter> typeParameters = function.getTypeParameters();
		for (KtTypeParameter typeParameter : typeParameters) {
			LocationInfo typeParameterLocation = generateLocationInfo(ktFile, sourceFolder, filePath, typeParameter, CodeElementType.TYPE_PARAMETER);
			UMLTypeParameter umlTypeParameter = new UMLTypeParameter(typeParameter.getName(), typeParameterLocation);
			KtTypeReference typeBounds = typeParameter.getExtendsBound();
			if (typeBounds != null) {
				umlTypeParameter.addTypeBound(
						UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, typeBounds, 0));
			}
			KtModifierList typeParameterModifiers = typeParameter.getModifierList();
			if (typeParameterModifiers != null) {
				for (PsiElement modifier : typeParameterModifiers.getChildren()) {
					if (modifier instanceof KtAnnotationEntry annotationEntry) {
						umlTypeParameter.addAnnotation(new UMLAnnotation(ktFile, sourceFolder, filePath, annotationEntry, fileContent));
					}
				}
			}
			umlOperation.addTypeParameter(umlTypeParameter);
		}
		if (function.getReceiverTypeReference() != null) {
			UMLType type = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, function.getReceiverTypeReference(), 0);
			umlOperation.setReceiverTypeReference(type);
		}
		if (function.hasDeclaredReturnType()) {
			KtTypeReference returnTypeReference = function.getTypeReference();
			if (returnTypeReference != null) {
				UMLType typeObject = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, returnTypeReference, 0);
				UMLParameter returnParameter = new UMLParameter("return", typeObject, "return", false);
				umlOperation.addParameter(returnParameter);
			}
		}
		List<KtParameter> parameters = function.getValueParameters();
		for (KtParameter parameter : parameters) {
			KtTypeReference typeReference = parameter.getTypeReference();
			String parameterName = parameter.getName();
			UMLType type = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, typeReference, 0);
			if (parameter.isVarArg()) {
				type.setVarargs();
			}
			UMLParameter umlParameter = new UMLParameter(parameterName, type, "in", parameter.isVarArg());
			VariableDeclaration variableDeclaration =
					new VariableDeclaration(ktFile, sourceFolder, filePath, parameter, umlOperation, new LinkedHashMap<>(), fileContent, umlOperation.getLocationInfo());
			variableDeclaration.setParameter(true);
			umlParameter.setVariableDeclaration(variableDeclaration);
			umlOperation.addParameter(umlParameter);
		}
		KtExpression functionInitializer = function.getInitializer();
		if (functionInitializer != null) {
			Map<String, Set<VariableDeclaration>> activeVariableDeclarations = new LinkedHashMap<>();
			for(VariableDeclaration v : umlOperation.getParameterDeclarationList()) {
				if(activeVariableDeclarations.containsKey(v.getVariableName())) {
					activeVariableDeclarations.get(v.getVariableName()).add(v);
				}
				else {
					Set<VariableDeclaration> set = new HashSet<VariableDeclaration>();
					set.add(v);
					activeVariableDeclarations.put(v.getVariableName(), set);
				}
			}
			AbstractExpression defaultExpression = new AbstractExpression(ktFile, sourceFolder, filePath, functionInitializer, CodeElementType.FUNCTION_INITIALIZER_EXPRESSION, umlOperation, activeVariableDeclarations, fileContent);
			umlOperation.setDefaultExpression(defaultExpression);
		}
		if (function.getBodyBlockExpression() != null) {
			OperationBody operationBody = new OperationBody(ktFile, sourceFolder, filePath, function.getBodyBlockExpression(), umlOperation, attributes, fileContent);
			umlOperation.setBody(operationBody);
		}
		return umlOperation;
	}

	private UMLAttribute processFieldDeclaration(KtFile ktFile, KtProperty property, String sourceFolder, String filePath, String fileContent, List<UMLComment> comments, LocationInfo parentLocationInfo) {
		KtTypeReference type = property.getTypeReference();
		UMLType typeObject = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, type, 0);
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, property, CodeElementType.FIELD_DECLARATION);
		UMLAttribute umlAttribute = new UMLAttribute(property.getName(), typeObject, locationInfo);
		VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, sourceFolder, filePath, property, umlAttribute, new LinkedHashMap<>(), fileContent, parentLocationInfo);
		variableDeclaration.setAttribute(true);
		umlAttribute.setVariableDeclaration(variableDeclaration);
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, property.getDocComment());
		umlAttribute.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, umlAttribute.getComments());
		
		KtModifierList modifierList = property.getModifierList();
		// default visibility in Kotlin is public
		umlAttribute.setVisibility(Visibility.PUBLIC);
		if(modifierList != null) {
			if (modifierList.hasModifier(PUBLIC_KEYWORD)) {
				umlAttribute.setVisibility(Visibility.PUBLIC);
			} else if (modifierList.hasModifier(PROTECTED_KEYWORD)) {
				umlAttribute.setVisibility(Visibility.PROTECTED);
			} else if (modifierList.hasModifier(PRIVATE_KEYWORD)) {
				umlAttribute.setVisibility(Visibility.PRIVATE);
			} else if (modifierList.hasModifier(INTERNAL_KEYWORD)) {
				umlAttribute.setVisibility(Visibility.INTERNAL);
			}
		}
		return umlAttribute;
	}

	private LocationInfo generateLocationInfo(KtFile ktFile,
			String sourceFolder,
			String sourceFilePath,
			KtElement node,
			CodeElementType codeElementType) {
		return new LocationInfo(ktFile, sourceFolder, sourceFilePath, node, codeElementType);
	}
}
