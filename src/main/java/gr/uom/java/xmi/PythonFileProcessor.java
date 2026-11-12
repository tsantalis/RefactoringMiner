package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
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
		ParseResult result = parse(fileContent);
		if(!result.errors.isEmpty()) {
			result = repairMissingCommas(filePath, fileContent, result);
		}
		CommonTokenStream tokens = result.tokens;
		PythonParser.File_inputContext parseTree = result.parseTree;
		LangASTNode ast = new PyASTBuilder().build(parseTree);
		if (astDiff) {
			TreeContext treeContext = PythonTreeGenerator.generate(result.content, tokens, parseTree);
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

	private record ParseResult(String content, CommonTokenStream tokens, PythonParser.File_inputContext parseTree, List<Token> errors) {}

	private static ParseResult parse(String content) {
		List<Token> errors = new ArrayList<Token>();
		BaseErrorListener listener = new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
				if(offendingSymbol instanceof Token token) {
					errors.add(token);
				}
			}
		};
		PythonLexer lexer = new PythonLexer(new Utf16CharStream(content));
		lexer.removeErrorListeners();
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		PythonParser parser = new PythonParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		PythonParser.File_inputContext parseTree = parser.file_input();
		return new ParseResult(content, tokens, parseTree, errors);
	}

	//a missing comma between two items in brackets, i.e., w_ppl=1. w_d=1 in a parameter list, or "name": "tsf" followed by "rho": ... in a dictionary,
	//is inserted in place of the whitespace before the second item, so that the offsets, lines and columns of the file do not change,
	//as long as each insertion reduces the syntax errors
	private static ParseResult repairMissingCommas(String filePath, String fileContent, ParseResult result) {
		for(int i=0; i<20 && !result.errors.isEmpty(); i++) {
			int position = missingCommaPosition(result);
			if(position == -1) {
				break;
			}
			String content = result.content.substring(0, position) + "," + result.content.substring(position + 1);
			ParseResult repaired = parse(content);
			if(repaired.errors.size() >= result.errors.size()) {
				break;
			}
			int line = (int)result.content.substring(0, position).chars().filter(c -> c == '\n').count() + 1;
			System.err.println("Repaired a missing comma at line " + line + " of " + filePath);
			result = repaired;
		}
		for(Token error : result.errors) {
			System.err.println("line " + error.getLine() + ":" + error.getCharPositionInLine() + " syntax error at '" + error.getText() + "' in " + filePath);
		}
		return result;
	}

	private static int missingCommaPosition(ParseResult result) {
		List<Token> tokens = new ArrayList<Token>();
		for(Token token : result.tokens.getTokens()) {
			if(token.getChannel() == Token.DEFAULT_CHANNEL && token.getType() != Token.EOF) {
				tokens.add(token);
			}
		}
		int[] depth = new int[tokens.size()];
		int current = 0;
		for(int i=0; i<tokens.size(); i++) {
			depth[i] = current;
			int type = tokens.get(i).getType();
			if(type == PythonLexer.LPAR || type == PythonLexer.LSQB || type == PythonLexer.LBRACE) {
				current++;
			}
			else if((type == PythonLexer.RPAR || type == PythonLexer.RSQB || type == PythonLexer.RBRACE) && current > 0) {
				current--;
			}
		}
		Token error = result.errors.get(0);
		int errorIndex = -1;
		for(int i=0; i<tokens.size(); i++) {
			if(tokens.get(i).getTokenIndex() <= error.getTokenIndex()) {
				errorIndex = i;
			}
		}
		//the nearest pair of items before the first syntax error
		for(int i=errorIndex; i>0; i--) {
			Token previous = tokens.get(i-1);
			Token next = tokens.get(i);
			//adjacent strings are concatenated, unless the concatenation causes the error, i.e., "name": "tsf" followed by "rho": 1.
			if(depth[i] > 0 && itemEnd(previous) && itemStart(next) && next.getStartIndex() > previous.getStopIndex() + 1) {
				char before = result.content.charAt(next.getStartIndex() - 1);
				if(before == ' ' || before == '\t') {
					return next.getStartIndex() - 1;
				}
			}
		}
		return -1;
	}

	private static boolean itemEnd(Token token) {
		int type = token.getType();
		return type == PythonLexer.NAME || type == PythonLexer.NUMBER || type == PythonLexer.STRING || type == PythonLexer.FSTRING_END ||
				type == PythonLexer.TRUE || type == PythonLexer.FALSE || type == PythonLexer.NONE ||
				type == PythonLexer.RPAR || type == PythonLexer.RSQB || type == PythonLexer.RBRACE;
	}

	private static boolean itemStart(Token token) {
		int type = token.getType();
		return type == PythonLexer.NAME || type == PythonLexer.NUMBER || type == PythonLexer.STRING || type == PythonLexer.FSTRING_START ||
				type == PythonLexer.TRUE || type == PythonLexer.FALSE || type == PythonLexer.NONE ||
				type == PythonLexer.LSQB || type == PythonLexer.LBRACE;
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
