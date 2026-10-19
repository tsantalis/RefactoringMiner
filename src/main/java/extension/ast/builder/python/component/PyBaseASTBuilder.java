package extension.ast.builder.python.component;

import java.util.ArrayList;
import java.util.List;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.python.PythonParser;

public class PyBaseASTBuilder {

    protected final PyASTBuilder mainBuilder;

    public PyBaseASTBuilder(PyASTBuilder mainBuilder) {
        this.mainBuilder = mainBuilder;
    }

    //the simple statements separated by ; in a line are statements of the enclosing body, as in the AST, i.e., a = 1; b = 2
    protected List<LangASTNode> statements(PythonParser.StatementContext ctx) {
        List<LangASTNode> statements = new ArrayList<>();
        if (ctx.simple_stmts() != null && ctx.simple_stmts().simple_stmt().size() > 1) {
            for (PythonParser.Simple_stmtContext simpleCtx : ctx.simple_stmts().simple_stmt()) {
                LangASTNode statement = mainBuilder.visit(simpleCtx);
                if (statement != null) {
                    statements.add(statement);
                }
            }
        }
        else {
            LangASTNode statement = mainBuilder.visit(ctx);
            if (statement != null) {
                statements.add(statement);
            }
        }
        return statements;
    }

}
