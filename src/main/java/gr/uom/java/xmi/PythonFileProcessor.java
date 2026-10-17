package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CommonTokenStream;
import org.refactoringminer.astDiff.utils.PythonTreeGenerator;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.Utf16CharStream;
import extension.base.lang.python.PythonLexer;
import extension.base.lang.python.PythonParser;
import extension.umladapter.UMLAdapterUtil;
import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class PythonFileProcessor {
	private UMLModel umlModel;

	public PythonFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	public void processPythonFile(String filePath, String fileContent, boolean astDiff) {
		//a single parse for the model and the AST diff tree
		CommonTokenStream tokens = new CommonTokenStream(new PythonLexer(new Utf16CharStream(fileContent)));
		PythonParser.File_inputContext parseTree = new PythonParser(tokens).file_input();
		LangASTNode ast = new PyASTBuilder().build(parseTree);
		if (astDiff) {
			TreeContext treeContext = PythonTreeGenerator.generate(fileContent, tokens, parseTree);
			List<Tree> trees = TreeUtilFunctions.findChildrenByTypeRecursively(treeContext.getRoot(), "comment");
			List<UMLComment> comments = new ArrayList<UMLComment>();
			for(Tree t : trees) {
				String sourceFolder = UMLAdapterUtil.extractSourceFolder(filePath);
				LocationInfo location = new LocationInfo(sourceFolder, filePath, t, CodeElementType.LINE_COMMENT, fileContent);
				UMLComment comment = new UMLComment(t.getLabel(), location);
				comments.add(comment);
			}
			this.umlModel.getCommentMap().put(filePath, comments);
			this.umlModel.getTreeContextMap().put(filePath, treeContext);
		}
		UMLModelAdapter.extractUMLEntities(ast, umlModel, filePath, fileContent);
	}

	//the operations declared in a code snippet, or null if the snippet is not valid Python code
	public static List<UMLOperation> parseOperations(String filePath, String code) {
		PythonLexer lexer = new PythonLexer(new Utf16CharStream(code));
		lexer.removeErrorListeners();
		PythonParser parser = new PythonParser(new CommonTokenStream(lexer));
		parser.removeErrorListeners();
		PythonParser.File_inputContext parseTree = parser.file_input();
		if(parser.getNumberOfSyntaxErrors() > 0) {
			return null;
		}
		UMLModel model = new UMLModel(Set.of());
		UMLModelAdapter.extractUMLEntities(new PyASTBuilder().build(parseTree), model, filePath, code);
		List<UMLOperation> operations = new ArrayList<UMLOperation>();
		for(UMLClass umlClass : model.getClassList()) {
			operations.addAll(umlClass.getOperations());
		}
		return operations;
	}
}
