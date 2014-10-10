package ca.ualberta.cs.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.*;

import org.codehaus.plexus.util.FileUtils;

public class SourceCompiler {
	private File directory;
	private ArrayList<File> files = new ArrayList<File>();
	private String classpath = "";
	private ArrayList<String> errors = new ArrayList<String>();

	public SourceCompiler() {
		//Empty;
	}

	public ArrayList<String> getErrors() {
		return errors;
	}

	public void compile(File directory) throws IOException {
		this.directory = directory;

		File sourcePath = new File(directory.getAbsolutePath() + File.separator + "src");
		if(sourcePath.exists())
			files = (ArrayList<File>) FileUtils.getFiles(sourcePath, "**/*.java", null);
		else
			files = (ArrayList<File>) FileUtils.getFiles(directory, "**/*.java", null);

		DiagnosticListener listener = new DiagnosticListener() {
			public void report(Diagnostic diagnostic) {
				String message = diagnostic.getMessage(null);
				//System.err.println("message: " + diagnostic.getMessage(null));
				//System.err.println("line number: " + diagnostic.getLineNumber());
				//System.err.println(diagnostic.getSource());
				if(!message.startsWith("Note:") && !message.endsWith("is never thrown in body of corresponding try statement") &&
						!message.contains("warning: non-varargs call of varargs method with inexact argument type for last parameter") &&
						!message.endsWith("for a non-varargs call and to suppress this warning") &&
						!message.endsWith("for a varargs call") &&
						!message.contains("warning: as of release 1.4, 'assert' is a keyword, and may not be used as an identifier") &&
						!message.contains("(use -source 1.4 or higher to use 'assert' as a keyword)"))
					errors.add(message);
			}
		};

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

		List<String> optionList = new ArrayList<String>();
		File bin = new File(directory.getAbsolutePath() + "/bin");
		bin.mkdir();
		//optionList.addAll(Arrays.asList("-source", "1.3"));
		optionList.addAll(Arrays.asList("-d", directory.getAbsolutePath() + File.separator + "bin" + File.separator));
		optionList.addAll(Arrays.asList("-classpath", getClasspath()));

		compiler.getTask(null, fileManager, listener, optionList, null, compilationUnits).call();

		fileManager.close();
	}

	private String getClasspath() {
		recursiveTraversal(directory);
		//recursiveTraversal(new File(Constants.getValue("ECLIPSE_PLUGINS")));
		if(classpath.equals(""))
			return classpath;
		else
			return classpath.substring(0, classpath.length() - 1);
	}

	private void recursiveTraversal(File fileObject) {		
		if (fileObject.isDirectory()) {
			File allFiles[] = fileObject.listFiles();
			for(File aFile : allFiles) {
				recursiveTraversal(aFile);
			}
		}
		else if (fileObject.isFile() && fileObject.getName().endsWith(".jar")) {
			//if(!fileObject.getName().endsWith("test.jar")) {
			files.add(fileObject);
			classpath += fileObject.getAbsolutePath() + File.pathSeparator;
			//}
		}		
	}

}
