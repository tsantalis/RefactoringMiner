package extension.ast.builder.go.component;

import extension.ast.builder.go.GoASTBuilder;

public class GoBaseASTBuilder {

    protected final GoASTBuilder mainBuilder;

    public GoBaseASTBuilder(GoASTBuilder mainBuilder) {
        this.mainBuilder = mainBuilder;
    }

}
