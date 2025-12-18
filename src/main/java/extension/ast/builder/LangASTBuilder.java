package extension.ast.builder;

import extension.ast.node.LangASTNode;

/**
 * A generic interface for building a custom AST from a language-specific parse tree.
 * @param <ParseTreeRoot> The type of the parse tree root node.
 */
public interface LangASTBuilder<ParseTreeRoot> {
    LangASTNode build(ParseTreeRoot ctx);
}

