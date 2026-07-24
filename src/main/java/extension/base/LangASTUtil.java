package extension.base;

import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.builder.go.GoASTBuilder;
import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.go.GoLexer;
import extension.base.lang.go.GoParser;
import extension.base.lang.python.PythonLexer;
import extension.base.lang.python.PythonParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Utility class to create Language AST parsers
 * based on file extensions and supported programming languages
 */
public class LangASTUtil {


    public static LangASTNode getLangAST(LangSupportedEnum language, String content) throws IOException {

        if (language == null) {
            throw new UnsupportedOperationException("Language not supported for file");
        }

        return switch (language) {
            case PYTHON -> getCustomPythonAST(new StringReader(content));
            case CSHARP -> getCustomCSharpAST(new StringReader(content));
            case GO -> getCustomGoAST(new StringReader(content));
            default -> throw new UnsupportedOperationException("Parser not implemented for language: " + language);
        };

    }

    public static LangASTNode getCustomPythonAST(Reader r) throws IOException {

        // Parse the Python code
        CharStream input = CharStreams.fromReader(r);
        PythonLexer lexer = new PythonLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PythonParser parser = new PythonParser(tokens);

        // Get the parse tree
        PythonParser.File_inputContext parseTree = parser.file_input();

    //    System.out.println(parseTree.getText() + "\n");

        // Build custom AST
        PyASTBuilder astBuilder = new PyASTBuilder();

        return astBuilder.build(parseTree);
    }


    public static LangASTNode getCustomCSharpAST(Reader r) throws IOException {

        // Parse the C# code
        CharStream input = CharStreams.fromReader(r);
        extension.base.lang.csharp.CSharpLexer lexer = new extension.base.lang.csharp.CSharpLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CSharpParser parser = new CSharpParser(tokens);

        // Get the parse tree
        CSharpParser.Compilation_unitContext parseTree = parser.compilation_unit();

        // Build custom AST
        CSharpASTBuilder astBuilder = new CSharpASTBuilder();

        return astBuilder.build(parseTree);
    }


    public static LangASTNode getCustomGoAST(Reader r) throws IOException {

        // Parse the Go code
        CharStream input = CharStreams.fromReader(r);
        GoLexer lexer = new GoLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GoParser parser = new GoParser(tokens);

        // Get the parse tree
        GoParser.SourceFileContext parseTree = parser.sourceFile();

        // Build custom AST
        GoASTBuilder astBuilder = new GoASTBuilder();

        return astBuilder.build(parseTree);
    }

}
