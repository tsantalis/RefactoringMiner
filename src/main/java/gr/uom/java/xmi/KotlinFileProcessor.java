package gr.uom.java.xmi;

import static org.jetbrains.kotlin.lexer.KtTokens.INTERNAL_KEYWORD;
import static org.jetbrains.kotlin.lexer.KtTokens.PRIVATE_KEYWORD;
import static org.jetbrains.kotlin.lexer.KtTokens.PROTECTED_KEYWORD;
import static org.jetbrains.kotlin.lexer.KtTokens.PUBLIC_KEYWORD;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtPackageDirective;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtTypeReference;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class KotlinFileProcessor {
	private UMLModel umlModel;

	public KotlinFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	public void processKotlinFile(String filePath, String fileContent, boolean astDiff, PsiFileFactoryImpl factory) {
		PsiFile psiFile = factory.createFileFromText(filePath, KotlinLanguage.INSTANCE, fileContent);
		KtFile ktFile = (KtFile)psiFile;
		
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
					// TODO process class declaration
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
			
			for(KtProperty property : topLevelProperties) {
				UMLAttribute attribute = processFieldDeclaration(ktFile, property, sourceFolder, filePath, fileContent, locationInfo);
				attribute.setClassName(moduleClass.getName());
				moduleClass.addAttribute(attribute);
			}
			for(KtNamedFunction function : topLevelFunctions) {
				// TODO process function declaration
			}
			umlModel.addClass(moduleClass);
		}
	}

	private UMLAttribute processFieldDeclaration(KtFile ktFile, KtProperty property, String sourceFolder, String filePath, String fileContent, LocationInfo parentLocationInfo) {
		KtTypeReference type = property.getTypeReference();
		UMLType typeObject = UMLType.extractTypeObject(ktFile, sourceFolder, filePath, fileContent, type, 0);
		LocationInfo locationInfo = generateLocationInfo(ktFile, sourceFolder, filePath, property, CodeElementType.FIELD_DECLARATION);
		UMLAttribute umlAttribute = new UMLAttribute(property.getName(), typeObject, locationInfo);
		VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, sourceFolder, filePath, property, umlAttribute, new LinkedHashMap<>(), fileContent, parentLocationInfo);
		variableDeclaration.setAttribute(true);
		umlAttribute.setVariableDeclaration(variableDeclaration);
		
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
