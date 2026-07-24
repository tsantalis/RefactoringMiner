package extension.ast.builder.go.component;

import extension.ast.builder.go.GoASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.base.lang.go.GoParser;

public class GoStatementASTBuilder extends GoBaseASTBuilder {

    public GoStatementASTBuilder(GoASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    // Return an empty placeholder block so callers have a non-null body to attach.
    // TODO implement GoVisitor for AST traversal
    public LangASTNode visitBlock(GoParser.BlockContext ctx) {
        return LangASTNodeFactory.createBlock(ctx, null);
    }

}
