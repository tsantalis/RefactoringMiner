package gr.uom.java.xmi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

public class ASTReader3 {
	private final static Logger logger = Logger.getLogger(ASTReader3.class.getName());
	private UMLModelSet umlModelSet;

	public ASTReader3(File srcFolder) throws Exception {
		this.umlModelSet = new UMLModelSet();
		
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		parser.setCompilerOptions(options);

		Collection<File> files = FileUtils.listFiles(
				srcFolder, 
				new RegexFileFilter("^(.java)"), 
				DirectoryFileFilter.DIRECTORY
				);

		String[] bindingKeys = new String[files.size()];

		parser.setEnvironment(null, new String[]{srcFolder.getPath()}, null, true);
		parser.setResolveBindings(true); // we need bindings later on
		parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);

		String[] canonicalPaths = new String[files.size()];
		int i = 0;
		for (File javaFile : files) {
			canonicalPaths[i] = javaFile.getPath();
			try {
				bindingKeys[i] = createBindingKeyFromClassFile(javaFile);
			} catch (IOException e) {
				logger.log(Level.SEVERE,null,e);
			}
			i++;
		}

		ASTFileReader myASTFileReader = new ASTFileReader(umlModelSet, true, srcFolder);
		try {
			parser.createASTs(canonicalPaths, null, bindingKeys, myASTFileReader, null);
		} catch (Exception e) {
			logger.log(Level.SEVERE,null,e);
		}
	}

	public UMLModelSet getUmlModelSet() {
		return umlModelSet;
	}

	private static String createBindingKeyFromClassFile(File file) throws IOException {
		String filePath = file.getPath();
		String classString = readFileToString(file);
		String fullyQualifiedClassName = "";
		try {
			String packageName = "";
			int packageDeclarationStart = classString.indexOf("package");
			if (packageDeclarationStart != -1) {
				int packageDeclarationEnd = classString.indexOf(";", packageDeclarationStart);
				String packageDeclarationLine = classString.substring(packageDeclarationStart, packageDeclarationEnd);
				packageName = packageDeclarationLine.substring(packageDeclarationLine.lastIndexOf("package") + 7, packageDeclarationLine.length());
			} else {
				packageName = "";
			}
			String className = filePath.substring(filePath.lastIndexOf(File.separator), filePath.indexOf(".java"));
			fullyQualifiedClassName = packageName + "." + className + ";";
			fullyQualifiedClassName = fullyQualifiedClassName.replace(".", File.separator);
		} catch (Exception e) {
			logger.log(Level.SEVERE,"error with class {0}",classString);
		}
		return fullyQualifiedClassName;
	}

	private static String readFileToString(File file) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

}
