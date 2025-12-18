package extension.ast.stringifier;

import extension.ast.visitor.LangASTVisitor;

public interface LangASTFlattener extends LangASTVisitor {
    String getResult();
}
