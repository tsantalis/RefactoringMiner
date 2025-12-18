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
				processJavaFileContents(fileContents, astDiff);
			}
		} else {
			// Only Java-Kotlin files
			this.umlModel = new UMLModel(repositoryDirectories);
			processJavaFileContents(fileContents, astDiff);
		}
	}

	public UMLModel getUmlModel() {
		return this.umlModel;
	}

	private void processJavaFileContents(Map<String, String> javaFileContents, boolean astDiff) {
		// create a single ASTParser instance for all Java files (performance)
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		// create a single environment instance for all Kotlin files (performance)
		KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
				Disposer.newDisposable(),
				new CompilerConfiguration(),
				EnvironmentConfigFiles.JVM_CONFIG_FILES
				);
		PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(environment.getProject());
		for(String filePath : javaFileContents.keySet()) {
			String javaFileContent = javaFileContents.get(filePath);
			if(PathFileUtils.isJavaFile(filePath)) {
				JavaFileProcessor processor = new JavaFileProcessor(umlModel);
				processor.processJavaFile(filePath, javaFileContent, astDiff, parser);
			}
			else if(PathFileUtils.isKotlinFile(filePath)) {
				KotlinFileProcessor processor = new KotlinFileProcessor(umlModel);
				processor.processKotlinFile(filePath, javaFileContent, astDiff, factory);
			}
		}
	}

}
