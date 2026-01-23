package gr.uom.java.xmi;

import static org.jetbrains.kotlin.lexer.KtTokens.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
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
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFileAnnotationList;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtInitializerList;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtPackageDirective;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtPropertyAccessor;
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;
import org.jetbrains.kotlin.psi.KtTypeAlias;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.KtValueArgumentList;
import org.jetbrains.kotlin.psi.KtWhenExpression;

import com.github.gumtreediff.gen.treesitterng.KotlinTreeSitterNgTreeGenerator;
import com.github.gumtreediff.tree.TreeContext;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
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

	private static void distributeComments(List<UMLComment> compilationUnitComments, LocationInfo codeElementLocationInfo, List<UMLComment> codeElementComments) {
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

	private static UMLJavadoc generateDocComment(KtFile cu, String sourceFolder, String filePath, String fileContent, KDoc javaDoc) {
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
		List<KtTypeAlias> topLevelTypeAliasList = new ArrayList<>();
		List<KtAnnotationEntry> topLevelAnnotations = new ArrayList<>();
		for (PsiElement psiElement : ktFile.getChildren()) {
			if (psiElement instanceof KtObjectDeclaration objectDeclaration) {
				UMLClass companionObject = processObjectDeclaration(ktFile, objectDeclaration, umlPackage, packageName, sourceFolder, filePath, fileContent, importedTypes, comments, Collections.emptyList(), umlModel);
				umlModel.addClass(companionObject);
			}
			else if (psiElement instanceof KtFileAnnotationList annotationList) {
				for (KtAnnotationEntry entry : annotationList.getAnnotationEntries()) {
					topLevelAnnotations.add(entry);
				}
			}
			else if (psiElement instanceof KtClass ktClass) {
				UMLClass umlClass = processClassDeclaration(ktFile, ktClass, umlPackage, packageName, sourceFolder, filePath, fileContent, importedTypes, comments, umlModel);
				umlModel.addClass(umlClass);
			}
			else if (psiElement instanceof KtNamedFunction function) {
				topLevelFunctions.add(function);
			}
			else if (psiElement instanceof KtProperty property) {
				topLevelProperties.add(property);
			}
			else if (psiElement instanceof KtTypeAlias typeAlias) {
				topLevelTypeAliasList.add(typeAlias);
			}
		}
		if (topLevelFunctions.size() > 0 || topLevelProperties.size() > 0 || topLevelTypeAliasList.size() > 0 || topLevelAnnotations.size() > 0) {
			LocationInfo locationInfo = new LocationInfo(ktFile, sourceFolder, filePath, ktFile, CodeElementType.TYPE_DECLARATION);
			String baseFileName = ktFile.getName();
			if (baseFileName.endsWith(".kt")) {
				baseFileName = baseFileName.substring(0, baseFileName.length() - 3);
			}
			if (baseFileName.contains("/")) {
				baseFileName = baseFileName.substring(baseFileName.lastIndexOf("/")+1, baseFileName.length());
			}
			UMLClass moduleClass = new UMLClass(packageName, baseFileName + ".module", locationInfo, true, importedTypes);
			moduleClass.setModule(true);
			moduleClass.setPackageDeclaration(umlPackage);
			moduleClass.setVisibility(Visibility.PUBLIC);
			
			for(KtAnnotationEntry annotationEntry : topLevelAnnotations) {
				moduleClass.addAnnotation(new UMLAnnotation(ktFile, sourceFolder, filePath, annotationEntry, fileContent));
			}
			for(KtProperty property : topLevelProperties) {
				UMLAttribute attribute = processFieldDeclaration(ktFile, property, sourceFolder, filePath, fileContent, comments, locationInfo, moduleClass.getName());
				moduleClass.addAttribute(attribute);
			}
			for(KtNamedFunction function : topLevelFunctions) {
				UMLOperation operation = processFunctionDeclaration(ktFile, function, sourceFolder, filePath, fileContent, moduleClass.getAttributes(), comments, moduleClass.getName());
				moduleClass.addOperation(operation);
			}
			for(KtTypeAlias typeAlias : topLevelTypeAliasList) {
				KtTypeReference typeReference = typeAlias.getTypeReference();
				LocationInfo typeAliasLocationInfo = new LocationInfo(ktFile, sourceFolder, filePath, typeAlias, CodeElementType.TYPE_ALIAS);
				UMLType rightType = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, typeReference, 0);
				UMLTypeAlias umlTypeAlias = new UMLTypeAlias(typeAlias.getName(), rightType, typeAliasLocationInfo);
				for(KtTypeParameter typeParameter : typeAlias.getTypeParameters()) {
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
					umlTypeAlias.addTypeParameter(umlTypeParameter);
				}
				moduleClass.addTypeAlias(umlTypeAlias);
			}
			distributeComments(comments, locationInfo, moduleClass.getComments());
			umlModel.addClass(moduleClass);
		}
	}

	private static UMLClass processObjectDeclaration(KtFile ktFile, KtObjectDeclaration ktClass, UMLPackage umlPackage, String packageName, String sourceFolder, String filePath, String fileContent, List<UMLImport> importedTypes, List<UMLComment> comments, List<UMLAttribute> attributes, UMLModel umlModel) {
		LocationInfo locationInfo = new LocationInfo(ktFile, sourceFolder, filePath, ktClass, CodeElementType.OBJECT_DECLARATION);
		String name = ktClass.getName() != null ? ktClass.getName() : "Companion";
		UMLClass umlClass = new UMLClass(packageName, name, locationInfo, ktClass.isTopLevel(), importedTypes);
		umlClass.setObject(true);
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, ktClass.getDocComment());
		umlClass.setJavadoc(javadoc);
		LocationInfo lastImportLocationInfo = importedTypes.size() > 0 ? importedTypes.get(importedTypes.size()-1).getLocationInfo() : null;
		if(ktFile.getName().endsWith(ktClass.getName() + ".kt")) {
			umlClass.setPackageDeclaration(umlPackage);
			List<KtDeclaration> declarations = ktFile.getDeclarations().stream()
					.filter(type -> type instanceof KtClass || type instanceof KtObjectDeclaration)
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
		
		KtModifierList modifierList = ktClass.getModifierList();
		processClassModifiers(ktFile, sourceFolder, filePath, fileContent, umlClass, modifierList);
		List<KtTypeParameter> typeParameters = ktClass.getTypeParameters();
		processTypeParameters(ktFile, sourceFolder, filePath, fileContent, umlClass, typeParameters);
		Map<String, Set<VariableDeclaration>> activeVariableDeclarations = new LinkedHashMap<>();
		for(UMLAttribute attribute : attributes) {
			VariableDeclaration variableDeclaration = attribute.getVariableDeclaration();
			if(activeVariableDeclarations.containsKey(variableDeclaration.getVariableName())) {
				activeVariableDeclarations.get(variableDeclaration.getVariableName()).add(variableDeclaration);
			}
			else {
				Set<VariableDeclaration> set = new HashSet<VariableDeclaration>();
				set.add(variableDeclaration);
				activeVariableDeclarations.put(variableDeclaration.getVariableName(), set);
			}
		}
		List<KtSuperTypeListEntry> superTypeListEntries = ktClass.getSuperTypeListEntries();
		processSuperTypeListEntries(ktFile, sourceFolder, filePath, fileContent, umlClass, activeVariableDeclarations, superTypeListEntries, umlModel);
		KtClassBody classBody = ktClass.getBody();
		processClassBody(ktFile, sourceFolder, filePath, fileContent, importedTypes, comments, umlClass, activeVariableDeclarations, classBody, umlModel);
		distributeComments(comments, locationInfo, umlClass.getComments());
		return umlClass;
	}

	private static UMLClass processClassDeclaration(KtFile ktFile, KtClass ktClass, UMLPackage umlPackage, String packageName, String sourceFolder, String filePath, String fileContent, List<UMLImport> importedTypes, List<UMLComment> comments, UMLModel umlModel) {
		LocationInfo locationInfo = new LocationInfo(ktFile, sourceFolder, filePath, ktClass, CodeElementType.TYPE_DECLARATION);
		UMLClass umlClass = new UMLClass(packageName, ktClass.getName(), locationInfo, ktClass.isTopLevel(), importedTypes);
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, ktClass.getDocComment());
		umlClass.setJavadoc(javadoc);
		if(ktClass.isInterface()) {
			umlClass.setInterface(true);
		}
		if(ktClass.isEnum()) {
			umlClass.setEnum(true);
		}
		if(ktClass.isAnnotation()) {
			umlClass.setAnnotation(true);
		}
		if(ktClass.isData()) {
			umlClass.setData(true);
		}
		if(ktClass.isSealed()) {
			umlClass.setSealed(true);
		}
		if(ktClass.isLocal()) {
			umlClass.setLocal(true);
		}
		if(ktClass.isInner()) {
			umlClass.setTopLevel(false);
		}
		else {
			umlClass.setTopLevel(true);
		}
		LocationInfo lastImportLocationInfo = importedTypes.size() > 0 ? importedTypes.get(importedTypes.size()-1).getLocationInfo() : null;
		if(ktFile.getName().endsWith(ktClass.getName() + ".kt")) {
			umlClass.setPackageDeclaration(umlPackage);
			List<KtDeclaration> declarations = ktFile.getDeclarations().stream()
					.filter(type -> type instanceof KtClass || type instanceof KtObjectDeclaration)
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
		
		KtModifierList modifierList = ktClass.getModifierList();
		processClassModifiers(ktFile, sourceFolder, filePath, fileContent, umlClass, modifierList);
		List<KtTypeParameter> typeParameters = ktClass.getTypeParameters();
		processTypeParameters(ktFile, sourceFolder, filePath, fileContent, umlClass, typeParameters);
		Map<String, Set<VariableDeclaration>> activeVariableDeclarations = new LinkedHashMap<>();
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
				if(activeVariableDeclarations.containsKey(variableDeclaration.getVariableName())) {
					activeVariableDeclarations.get(variableDeclaration.getVariableName()).add(variableDeclaration);
				}
				else {
					Set<VariableDeclaration> set = new HashSet<VariableDeclaration>();
					set.add(variableDeclaration);
					activeVariableDeclarations.put(variableDeclaration.getVariableName(), set);
				}
				if(parameter.hasValOrVar()) {
					variableDeclaration.setAttribute(true);
					UMLAttribute umlAttribute = new UMLAttribute(parameterName, type, variableDeclaration.getLocationInfo(), umlClass.getName());
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
				primaryConstructor.addParameter(umlParameter);
			}
			umlClass.setPrimaryConstructor(primaryConstructor);
		}
		List<KtSuperTypeListEntry> superTypeListEntries = ktClass.getSuperTypeListEntries();
		processSuperTypeListEntries(ktFile, sourceFolder, filePath, fileContent, umlClass, activeVariableDeclarations, superTypeListEntries, umlModel);
		KtClassBody classBody = ktClass.getBody();
		processClassBody(ktFile, sourceFolder, filePath, fileContent, importedTypes, comments, umlClass, activeVariableDeclarations, classBody, umlModel);
		distributeComments(comments, locationInfo, umlClass.getComments());
		return umlClass;
	}

	private static void processSuperTypeListEntries(KtFile ktFile, String sourceFolder, String filePath, String fileContent,
			UMLClass umlClass, Map<String, Set<VariableDeclaration>> activeVariableDeclarations,
			List<KtSuperTypeListEntry> superTypeListEntries, UMLModel umlModel) {
		int index = 0;
		for (KtSuperTypeListEntry superTypeListEntry : superTypeListEntries) {
			UMLType umlType = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent,
					superTypeListEntry.getTypeReference(), 0);
			if(index == 0) {
				UMLGeneralization umlGeneralization = new UMLGeneralization(umlClass, umlType.getClassType());
				umlClass.setSuperclass(umlType);
				if(umlModel != null)
					umlModel.addGeneralization(umlGeneralization);
			}
			else {
				UMLRealization umlRealization = new UMLRealization(umlClass, umlType.getClassType());
				umlClass.addImplementedInterface(umlType);
				if(umlModel != null)
					umlModel.addRealization(umlRealization);
			}
			AbstractExpression callEntry = new AbstractExpression(ktFile, sourceFolder, filePath, superTypeListEntry, CodeElementType.SUPER_TYPE_CALL_ENTRY,
					umlClass.getPrimaryConstructor().isPresent() ? umlClass.getPrimaryConstructor().get() : null, activeVariableDeclarations, fileContent);
			addStatementInVariableScopes(activeVariableDeclarations, callEntry);
			umlClass.addSuperTypeCallEntry(callEntry);
			index++;
		}
	}

	private static void addStatementInVariableScopes(Map<String, Set<VariableDeclaration>> activeVariableDeclarations, AbstractExpression statement) {
		for(String variableName : activeVariableDeclarations.keySet()) {
			Set<VariableDeclaration> variableDeclarations = activeVariableDeclarations.get(variableName);
			for(VariableDeclaration variableDeclaration : variableDeclarations) {
				boolean localVariableWithSameName = false;
				if(variableDeclaration.isAttribute() && variableDeclarations.size() > 1) {
					localVariableWithSameName = true;
				}
				variableDeclaration.addStatementInScope(statement, localVariableWithSameName);
				for(LambdaExpressionObject lambda : statement.getLambdas()) {
					OperationBody lambdaBody = lambda.getBody();
					if(lambdaBody != null) {
						CompositeStatementObject composite = lambdaBody.getCompositeStatement();
						for(AbstractStatement lambdaStatement : composite.getInnerNodes()) {
							variableDeclaration.addStatementInScope(lambdaStatement, localVariableWithSameName);
						}
						for(AbstractCodeFragment lambdaStatement : composite.getLeaves()) {
							variableDeclaration.addStatementInScope(lambdaStatement, localVariableWithSameName);
						}
					}
					AbstractExpression lambdaExpression = lambda.getExpression();
					if(lambdaExpression != null) {
						variableDeclaration.addStatementInScope(lambdaExpression, localVariableWithSameName);
					}
				}
			}
		}
	}

	public static void processClassBody(KtFile ktFile, String sourceFolder, String filePath, String fileContent,
			List<UMLImport> importedTypes, List<UMLComment> comments, UMLAbstractClass umlClass,
			Map<String, Set<VariableDeclaration>> activeVariableDeclarations, KtClassBody classBody, UMLModel umlModel) {
		if(classBody != null) {
			for(KtProperty property : classBody.getProperties()) {
				UMLAttribute attribute = processFieldDeclaration(ktFile, property, sourceFolder, filePath, fileContent, comments, umlClass.getLocationInfo(), umlClass.getName());
				umlClass.addAttribute(attribute);
			}
			for(KtAnonymousInitializer initializer : classBody.getAnonymousInitializers()) {
				UMLInitializer umlInitializer = processInitializer(ktFile, initializer, sourceFolder, filePath, fileContent, umlClass.getAttributes(), comments, umlClass.getNonQualifiedName(), umlClass.getName());
				umlClass.addInitializer(umlInitializer);
			}
			for(KtSecondaryConstructor constructor : classBody.getSecondaryConstructors$psi_api()) {
				LocationInfo constructorLocationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, constructor, CodeElementType.METHOD_DECLARATION);
				UMLOperation umlConstructor = new UMLOperation(umlClass.getNonQualifiedName(), constructorLocationInfo, umlClass.getName());
				umlConstructor.setConstructor(true);
				umlConstructor.setVisibility(Visibility.PUBLIC);
				UMLJavadoc constructorJavadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, constructor.getDocComment());
				umlConstructor.setJavadoc(constructorJavadoc);
				distributeComments(comments, constructorLocationInfo, umlConstructor.getComments());
				int startSignatureOffset = constructorLocationInfo.getStartOffset();
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
							new VariableDeclaration(ktFile, sourceFolder, filePath, parameter, umlConstructor, activeVariableDeclarations, fileContent, umlConstructor.getLocationInfo());
					if(parameter.hasValOrVar()) {
						variableDeclaration.setAttribute(true);
						UMLAttribute umlAttribute = new UMLAttribute(parameterName, type, variableDeclaration.getLocationInfo(), umlClass.getName());
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
				int endSignatureOffset = constructor.getBodyBlockExpression() != null ?
						umlConstructor.getBody().getCompositeStatement().getLocationInfo().getStartOffset() + 1 :
							constructor.getTextRange().getEndOffset();
				String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
				umlConstructor.setActualSignature(text);
				umlClass.addOperation(umlConstructor);
			}
			for(KtEnumEntry entry : classBody.getEnumEntries()) {
				UMLJavadoc entryJavadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, entry.getDocComment());
				LocationInfo entryLocationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, entry, CodeElementType.ENUM_CONSTANT_DECLARATION);
				UMLEnumConstant enumConstant = new UMLEnumConstant(entry.getName(), UMLType.extractTypeObject(umlClass.getName()), entryLocationInfo, umlClass.getName());
				VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, sourceFolder, filePath, entry, activeVariableDeclarations, fileContent, umlClass.getLocationInfo());
				enumConstant.setVariableDeclaration(variableDeclaration);
				enumConstant.setJavadoc(entryJavadoc);
				distributeComments(comments, entryLocationInfo, enumConstant.getComments());
				enumConstant.setFinal(true);
				enumConstant.setStatic(true);
				enumConstant.setVisibility(Visibility.PUBLIC);
				KtInitializerList initializerList = entry.getInitializerList();
				if(initializerList != null) {
					for(KtSuperTypeListEntry argument : initializerList.getInitializers()) {
						for(PsiElement element : argument.getChildren()) {
							if(element instanceof KtValueArgumentList argumentList) {
								for(KtValueArgument valueArgument : argumentList.getArguments()) {
									enumConstant.addArgument(valueArgument.getText());
								}
							}
							
						}
						
					}
				}
				if(entry.getBody() != null) {
					LocationInfo anonymousLocationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, entry.getBody(), CodeElementType.ANONYMOUS_CLASS_DECLARATION);
					UMLAnonymousClass anonymousClass =  new UMLAnonymousClass(umlClass.getName(), entry.getName(), entry.getName(), anonymousLocationInfo, importedTypes);
					processClassBody(ktFile, sourceFolder, filePath, fileContent, importedTypes, comments, anonymousClass, activeVariableDeclarations, entry.getBody(), umlModel);
					enumConstant.addAnonymousClass(anonymousClass);
					anonymousClass.addParentContainer(enumConstant);
				}
				umlClass.addEnumConstant(enumConstant);
			}
			for(KtNamedFunction function : classBody.getFunctions()) {
				UMLOperation operation = processFunctionDeclaration(ktFile, function, sourceFolder, filePath, fileContent, umlClass.getAttributes(), comments, umlClass.getName());
				umlClass.addOperation(operation);
			}
			for(KtObjectDeclaration companion : classBody.getAllCompanionObjects()) {
				UMLClass companionObject = processObjectDeclaration(ktFile, companion, null, umlClass.getName(), sourceFolder, filePath, fileContent, importedTypes, comments, umlClass.getAttributes(), umlModel);
				if(umlModel != null)
					umlModel.addClass(companionObject);
			}
			for(KtDeclaration declaration : classBody.getDeclarations()) {
				if(declaration instanceof KtClass ktClass) {
					boolean enumConstant = ktClass.getParent() instanceof KtClassBody && ktClass.getParent().getParent() instanceof KtClass parentClass && parentClass.isEnum() && ktClass.getClassKeyword() == null;
					if(!enumConstant) {
						UMLClass nestedClass = processClassDeclaration(ktFile, ktClass, null, umlClass.getName(), sourceFolder, filePath, fileContent, importedTypes, comments, umlModel);
						if(umlModel != null)
							umlModel.addClass(nestedClass);
					}
				}
				else if(declaration instanceof KtObjectDeclaration objectDeclaration) {
					UMLClass nestedClass = processObjectDeclaration(ktFile, objectDeclaration, null, umlClass.getName(), sourceFolder, filePath, fileContent, importedTypes, comments, umlClass.getAttributes(), umlModel);
					if(umlModel != null && !umlModel.getClassList().contains(nestedClass)) {
						umlModel.addClass(nestedClass);
					}
				}
			}
		}
	}

	private static void processTypeParameters(KtFile ktFile, String sourceFolder, String filePath, String fileContent,
			UMLClass umlClass, List<KtTypeParameter> typeParameters) {
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
	}

	private static void processClassModifiers(KtFile ktFile, String sourceFolder, String filePath, String fileContent,
			UMLClass umlClass, KtModifierList modifierList) {
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
			if (modifierList.hasModifier(FUN_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(FUN_KEYWORD));
				umlClass.addModifier(modifier);
				umlClass.setFunctionalInterface(true);
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
	}

	private static UMLInitializer processInitializer(KtFile ktFile, KtAnonymousInitializer initializer, String sourceFolder, String filePath, String fileContent, List<UMLAttribute> attributes, List<UMLComment> comments, String name, String className) {
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, initializer, CodeElementType.INITIALIZER);
		UMLInitializer umlInitializer = new UMLInitializer(name, locationInfo, className);
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

	private static UMLOperation processFunctionDeclaration(KtFile ktFile, KtNamedFunction function, String sourceFolder, String filePath, String fileContent, List<UMLAttribute> attributes, List<UMLComment> comments, String className) {
		String methodName = function.getName();
		UMLType receiver = null;
		if(function.getReceiverTypeReference() != null) {
			receiver = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, function.getReceiverTypeReference(), 0);
		}
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, function, CodeElementType.METHOD_DECLARATION);
		UMLOperation umlOperation = null;
		if(receiver != null) {
			umlOperation = new UMLOperation(receiver.toQualifiedString() + "." + methodName, locationInfo, className);
			umlOperation.setReceiver(receiver);
		}
		else {
			umlOperation = new UMLOperation(methodName, locationInfo, className);
		}
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, function.getDocComment());
		umlOperation.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, umlOperation.getComments());
		
		KtModifierList modifierList = function.getModifierList();
		int startSignatureOffset = processFunctionModifiers(ktFile, sourceFolder, filePath, fileContent, umlOperation, modifierList);
		List<KtTypeParameter> typeParameters = function.getTypeParameters();
		for (KtTypeParameter typeParameter : typeParameters) {
			LocationInfo typeParameterLocation = generateLocationInfo(ktFile, sourceFolder, filePath, typeParameter, CodeElementType.TYPE_PARAMETER);
			if(startSignatureOffset == -1) {
				startSignatureOffset = typeParameterLocation.getStartOffset();
			}
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
			if(startSignatureOffset == -1) {
				startSignatureOffset = type.getLocationInfo().getStartOffset();
			}
			umlOperation.setReceiverTypeReference(type);
		}
		if(startSignatureOffset == -1) {
			startSignatureOffset = function.getNameIdentifier().getTextRange().getStartOffset();
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
			if(functionInitializer instanceof KtWhenExpression whenExpression) {
				OperationBody operationBody = new OperationBody(ktFile, sourceFolder, filePath, whenExpression, umlOperation, attributes, fileContent);
				umlOperation.setBody(operationBody);
			}
			else if(functionInitializer instanceof KtIfExpression ifExpression) {
				OperationBody operationBody = new OperationBody(ktFile, sourceFolder, filePath, ifExpression, umlOperation, attributes, fileContent);
				umlOperation.setBody(operationBody);
			}
			else {
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
		}
		if (function.getBodyBlockExpression() != null) {
			OperationBody operationBody = new OperationBody(ktFile, sourceFolder, filePath, function.getBodyBlockExpression(), umlOperation, attributes, fileContent);
			umlOperation.setBody(operationBody);
		}
		int endSignatureOffset = function.getBodyBlockExpression() != null ?
				umlOperation.getBody().getCompositeStatement().getLocationInfo().getStartOffset() + 1 :
					function.getTextRange().getEndOffset();
		String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
		umlOperation.setActualSignature(text);
		return umlOperation;
	}

	private static int processFunctionModifiers(KtFile ktFile, String sourceFolder, String filePath, String fileContent,
			UMLOperation umlOperation, KtModifierList modifierList) {
		// default visibility in Kotlin is public
		umlOperation.setVisibility(Visibility.PUBLIC);
		int startSignatureOffset = -1;
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
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(PROTECTED_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PROTECTED_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setVisibility(Visibility.PROTECTED);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(PRIVATE_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(PRIVATE_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setVisibility(Visibility.PRIVATE);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(INTERNAL_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(INTERNAL_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setVisibility(Visibility.INTERNAL);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(OPEN_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(OPEN_KEYWORD));
				umlOperation.addModifier(modifier);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(FINAL_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(FINAL_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setFinal(true);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(OVERRIDE_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(OVERRIDE_KEYWORD));
				umlOperation.addModifier(modifier);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(INLINE_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(INLINE_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setInline(true);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
			if (modifierList.hasModifier(ABSTRACT_KEYWORD)) {
				UMLModifier modifier = new UMLModifier(ktFile, sourceFolder, filePath, modifierList.getModifier(ABSTRACT_KEYWORD));
				umlOperation.addModifier(modifier);
				umlOperation.setAbstract(true);
				if(startSignatureOffset == -1) {
					startSignatureOffset = modifier.getLocationInfo().getStartOffset();
				}
			}
		}
		return startSignatureOffset;
	}

	private static UMLAttribute processFieldDeclaration(KtFile ktFile, KtProperty property, String sourceFolder, String filePath, String fileContent, List<UMLComment> comments, LocationInfo parentLocationInfo, String className) {
		KtTypeReference type = property.getTypeReference();
		UMLType typeObject = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, type, 0);
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, property, CodeElementType.FIELD_DECLARATION);
		UMLAttribute umlAttribute = new UMLAttribute(property.getName(), typeObject, locationInfo, className);
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
		// by ...
		KtExpression delegateExpression = property.getDelegateExpression();
		if (delegateExpression != null) {
			
		}
		KtPropertyAccessor getter = property.getGetter();
		if (getter != null) {
			UMLOperation operation = processPropertyAccessor(ktFile, getter, sourceFolder, filePath, fileContent, Collections.emptyList(), comments, className);
			operation.setProperyAccessor(umlAttribute);
			umlAttribute.setCustomGetter(operation);
		}
		KtPropertyAccessor setter = property.getSetter();
		if (setter != null) {
			UMLOperation operation = processPropertyAccessor(ktFile, setter, sourceFolder, filePath, fileContent, Collections.emptyList(), comments, className);
			operation.setProperyAccessor(umlAttribute);
			umlAttribute.setCustomSetter(operation);
		}
		return umlAttribute;
	}

	private static UMLOperation processPropertyAccessor(KtFile ktFile, KtPropertyAccessor function, String sourceFolder, String filePath, String fileContent, List<UMLAttribute> attributes, List<UMLComment> comments, String className) {
		String methodName = "";
		if(function.isGetter())
			methodName = "get";
		else if(function.isSetter())
			methodName = "set";
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, function, CodeElementType.METHOD_DECLARATION);
		UMLOperation umlOperation = new UMLOperation(methodName, locationInfo, className);
		UMLJavadoc javadoc = generateDocComment(ktFile, sourceFolder, filePath, fileContent, function.getDocComment());
		umlOperation.setJavadoc(javadoc);
		distributeComments(comments, locationInfo, umlOperation.getComments());
		int startSignatureOffset = locationInfo.getStartOffset();
		KtModifierList modifierList = function.getModifierList();
		processFunctionModifiers(ktFile, sourceFolder, filePath, fileContent, umlOperation, modifierList);
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
		int endSignatureOffset = function.getBodyBlockExpression() != null ?
				umlOperation.getBody().getCompositeStatement().getLocationInfo().getStartOffset() + 1 :
					function.getTextRange().getEndOffset();
		String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
		umlOperation.setActualSignature(text);
		return umlOperation;
	}

	private static LocationInfo generateLocationInfo(KtFile ktFile,
			String sourceFolder,
			String sourceFilePath,
			KtElement node,
			CodeElementType codeElementType) {
		return new LocationInfo(ktFile, sourceFolder, sourceFilePath, node, codeElementType);
	}
}
