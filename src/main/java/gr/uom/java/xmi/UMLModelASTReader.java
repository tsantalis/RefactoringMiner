package gr.uom.java.xmi;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl;
import org.jetbrains.kotlin.config.CompilerConfiguration;

import extension.umladapter.UMLModelAdapter;
import org.refactoringminer.util.PathFileUtils;

import com.caoccao.javet.swc4j.Swc4j;

public class UMLModelASTReader {
	
	private UMLModel umlModel;

	public UMLModelASTReader(Map<String, String> fileContents, Set<String> repositoryDirectories, boolean astDiff) {
		boolean hasLangSupportedFiles = fileContents.keySet().stream()
				.anyMatch(PathFileUtils::isLangSupportedFile);

		if (hasLangSupportedFiles) {
			try {
				this.umlModel = new UMLModelAdapter(fileContents, astDiff).getUMLModel();
			} catch (IOException e) {
				System.err.println("Error processing language-supported files: " + e.getMessage());
				// Fall back to Java-Kotlin processing
				this.umlModel = new UMLModel(repositoryDirectories);
				processFileContents(fileContents, astDiff);
			}
		} else {
			// Only Java-Kotlin files
			this.umlModel = new UMLModel(repositoryDirectories);
			processFileContents(fileContents, astDiff);
		}
	}

	public UMLModel getUmlModel() {
		return this.umlModel;
	}

	private void processFileContents(Map<String, String> fileContents, boolean astDiff) {
		// create a single ASTParser instance for all Java files (performance)
		boolean hasJavaFiles = fileContents.keySet().stream()
				.anyMatch(PathFileUtils::isJavaFile);
		ASTParser parser = hasJavaFiles ? ASTParser.newParser(AST.getJLSLatest()) : null;
		// create a single environment instance for all Kotlin files (performance)
		boolean hasKotlinFiles = fileContents.keySet().stream()
				.anyMatch(PathFileUtils::isKotlinFile);
		PsiFileFactoryImpl factory = null;
		if(hasKotlinFiles) {
			KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
					Disposer.newDisposable(),
					new CompilerConfiguration(),
					EnvironmentConfigFiles.JVM_CONFIG_FILES
					);
			factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(environment.getProject());
		}
		// create a single instance, as it calls Swc4jLibLoader().load()
		boolean hasTSFiles = fileContents.keySet().stream()
				.anyMatch(PathFileUtils::isTypeScriptFile);
		Swc4j swc4j = hasTSFiles ? new Swc4j() : null;
		for(String filePath : fileContents.keySet()) {
			String fileContent = fileContents.get(filePath);
			if(PathFileUtils.isJavaFile(filePath)) {
				JavaFileProcessor processor = new JavaFileProcessor(umlModel);
				processor.processJavaFile(filePath, fileContent, astDiff, parser);
			}
			else if(PathFileUtils.isKotlinFile(filePath)) {
				KotlinFileProcessor processor = new KotlinFileProcessor(umlModel);
				processor.processKotlinFile(filePath, fileContent, astDiff, factory);
			}
			else if(PathFileUtils.isTypeScriptFile(filePath)) {
				TypeScriptFileProcessor processor = new TypeScriptFileProcessor(umlModel);
				processor.processTypeScriptFile(filePath, fileContent, astDiff, swc4j);
			}
		}
	}

}
