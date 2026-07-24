package extension.ast.builder.go.component;

import extension.ast.builder.go.GoASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.TypeObjectEnum;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.statement.LangBlock;
import extension.base.lang.go.GoParser;
import gr.uom.java.xmi.Visibility;

import java.util.ArrayList;
import java.util.List;

public class GoDeclarationASTBuilder extends GoBaseASTBuilder {

    public GoDeclarationASTBuilder(GoASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitFunctionDecl(GoParser.FunctionDeclContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        GoParser.SignatureContext signature = ctx.signature();
        List<LangSingleVariableDeclaration> parameters = buildParameters(signature.parameters());
        LangBlock body = ctx.block() != null ? (LangBlock) mainBuilder.visit(ctx.block()) : null;

        LangMethodDeclaration method = LangASTNodeFactory.createMethodDeclaration(name, ctx, parameters, body);
        applyReturnTypes(method, signature.result());
        applyVisibility(method, name);
        return method;
    }

    public LangASTNode visitMethodDecl(GoParser.MethodDeclContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        GoParser.SignatureContext signature = ctx.signature();
        List<LangSingleVariableDeclaration> parameters = buildParameters(signature.parameters());
        LangBlock body = ctx.block() != null ? (LangBlock) mainBuilder.visit(ctx.block()) : null;

        LangMethodDeclaration method = LangASTNodeFactory.createMethodDeclaration(name, ctx, parameters, body);
        applyReturnTypes(method, signature.result());
        applyVisibility(method, name);
        method.setReceiverType(receiverTypeText(ctx.receiver()));
        return method;
    }

    // For grouped type declarations (`type ( A struct {...}; B interface {...} )`), one entry per
    // typeSpec. Doesn't fit the single-node ANTLR visitor dispatch, so it's a plain helper method
    // called directly by GoCompilationUnitASTBuilder rather than any visit override.
    public List<LangTypeDeclaration> buildTypeDeclarations(GoParser.TypeDeclContext ctx) {
        List<LangTypeDeclaration> result = new ArrayList<>();
        for (GoParser.TypeSpecContext spec : ctx.typeSpec()) {
            GoParser.TypeDefContext typeDef = spec.typeDef();
            if (typeDef == null) {
                continue; // alias decl (`type X = Y`), not a class-like declaration
            }
            String name = typeDef.IDENTIFIER().getText();
            GoParser.TypeLitContext typeLit = findTypeLit(typeDef.type_());
            if (typeLit == null) {
                continue; // e.g. `type MyInt int`
                // TODO model as a UML-like type
            }
            if (typeLit.structType() != null) {
                result.add(buildStructType(name, typeDef, typeLit.structType()));
            } else if (typeLit.interfaceType() != null) {
                result.add(buildInterfaceType(name, typeDef, typeLit.interfaceType()));
            }
        }
        return result;
    }

    private GoParser.TypeLitContext findTypeLit(GoParser.Type_Context ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.typeLit() != null) {
            return ctx.typeLit();
        }
        return findTypeLit(ctx.type_());
    }

    private LangTypeDeclaration buildStructType(String name, GoParser.TypeDefContext ctx, GoParser.StructTypeContext structType) {
        LangTypeDeclaration type = LangASTNodeFactory.createTypeDeclaration(name, ctx);
        type.setInterface(false);
        type.setVisibility(isExported(name) ? Visibility.PUBLIC : Visibility.PRIVATE);
        for (GoParser.FieldDeclContext fieldDecl : structType.fieldDecl()) {
            if (fieldDecl.embeddedField() != null) {
                String embeddedTypeText = fieldDecl.embeddedField().getText();
                LangSingleVariableDeclaration field = LangASTNodeFactory.createSingleVariableDeclaration(embeddedTypeText, null, fieldDecl);
                field.setTypeAnnotationText(embeddedTypeText);
                field.setTypeAnnotation(TypeObjectEnum.OBJECT);
                field.setAttribute(true);
                field.setHasTypeAnnotation(true);
                type.addField(field);
                continue;
            }
            String typeText = fieldDecl.type_().getText();
            TypeObjectEnum mapped = TypeObjectEnum.fromType(typeText);
            for (var identifier : fieldDecl.identifierList().IDENTIFIER()) {
                LangSingleVariableDeclaration field = LangASTNodeFactory.createSingleVariableDeclaration(identifier.getText(), null, fieldDecl);
                field.setTypeAnnotationText(typeText);
                field.setTypeAnnotation(mapped != null ? mapped : TypeObjectEnum.OBJECT);
                field.setAttribute(true);
                field.setHasTypeAnnotation(true);
                type.addField(field);
            }
        }
        return type;
    }

    private LangTypeDeclaration buildInterfaceType(String name, GoParser.TypeDefContext ctx, GoParser.InterfaceTypeContext interfaceType) {
        LangTypeDeclaration type = LangASTNodeFactory.createTypeDeclaration(name, ctx);
        type.setInterface(true);
        type.setAbstract(true);
        type.setVisibility(isExported(name) ? Visibility.PUBLIC : Visibility.PRIVATE);
        for (GoParser.MethodSpecContext methodSpec : interfaceType.methodSpec()) {
            String methodName = methodSpec.IDENTIFIER().getText();
            List<LangSingleVariableDeclaration> parameters = buildParameters(methodSpec.parameters());
            LangMethodDeclaration method = LangASTNodeFactory.createMethodDeclaration(methodName, methodSpec, parameters, null);
            applyReturnTypes(method, methodSpec.result());
            method.setAbstract(true);
            method.setVisibility(isExported(methodName) ? Visibility.PUBLIC : Visibility.PRIVATE);
            type.addMethod(method);
        }
        return type;
    }

    private String receiverTypeText(GoParser.ReceiverContext receiver) {
        List<GoParser.ParameterDeclContext> decls = receiver.parameters().parameterDecl();
        if (decls.isEmpty()) {
            return null;
        }
        return decls.get(0).type_().getText();
    }

    private void applyReturnTypes(LangMethodDeclaration method, GoParser.ResultContext result) {
        if (result == null) {
            method.addReturnTypeAnnotation(TypeObjectEnum.VOID.name());
            return;
        }
        if (result.type_() != null) {
            method.addReturnTypeAnnotation(result.type_().getText());
            return;
        }
        List<GoParser.ParameterDeclContext> decls = result.parameters().parameterDecl();
        if (decls.isEmpty()) {
            method.addReturnTypeAnnotation(TypeObjectEnum.VOID.name());
            return;
        }
        for (GoParser.ParameterDeclContext decl : decls) {
            String typeText = decl.type_().getText();
            int count = decl.identifierList() != null ? decl.identifierList().IDENTIFIER().size() : 1;
            for (int i = 0; i < count; i++) {
                method.addReturnTypeAnnotation(typeText);
            }
        }
    }

    private void applyVisibility(LangMethodDeclaration method, String name) {
        method.setVisibility(isExported(name) ? Visibility.PUBLIC : Visibility.PRIVATE);
    }

    private static boolean isExported(String name) {
        return name != null && !name.isEmpty() && Character.isUpperCase(name.charAt(0));
    }

    private List<LangSingleVariableDeclaration> buildParameters(GoParser.ParametersContext parametersCtx) {
        List<LangSingleVariableDeclaration> result = new ArrayList<>();
        int syntheticIndex = 0;
        for (GoParser.ParameterDeclContext decl : parametersCtx.parameterDecl()) {
            String typeText = decl.type_().getText();
            boolean isVarArgs = decl.ELLIPSIS() != null;
            List<String> names = new ArrayList<>();
            if (decl.identifierList() != null) {
                for (var identifier : decl.identifierList().IDENTIFIER()) {
                    names.add(identifier.getText());
                }
            } else {
                names.add("arg" + syntheticIndex++);
            }
            TypeObjectEnum mapped = TypeObjectEnum.fromType(typeText);
            for (String name : names) {
                LangSingleVariableDeclaration param = LangASTNodeFactory.createSingleVariableDeclaration(name, null, decl);
                param.setParameter(true);
                param.setTypeAnnotationText(isVarArgs ? "..." + typeText : typeText);
                param.setTypeAnnotation(mapped != null ? mapped : TypeObjectEnum.OBJECT);
                param.setVarArgs(isVarArgs);
                result.add(param);
            }
        }
        return result;
    }

}
