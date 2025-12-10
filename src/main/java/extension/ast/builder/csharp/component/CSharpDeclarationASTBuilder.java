package extension.ast.builder.csharp.component;

import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.TypeObjectEnum;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.statement.LangBlock;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.csharp.CSharpParserBaseVisitor;
import gr.uom.java.xmi.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic C# declaration-related AST building (classes, methods).
 */
public class CSharpDeclarationASTBuilder extends CSharpParserBaseVisitor<LangASTNode> {

    protected final CSharpASTBuilder mainBuilder;

    public CSharpDeclarationASTBuilder(CSharpASTBuilder mainBuilder) {
        this.mainBuilder = mainBuilder;
    }

    @Override
    public LangASTNode visitClass_definition(CSharpParser.Class_definitionContext ctx) {
        String name = ctx.identifier() != null ? ctx.identifier().getText() : "";
        LangTypeDeclaration type = LangASTNodeFactory.createTypeDeclaration(name, ctx);


        type.setVisibility(Visibility.PUBLIC);
        type.setAbstract(false);
        type.setFinal(false);
        type.setEnum(false);
        type.setStatic(false);
        CSharpParser.Class_bodyContext body = ctx.class_body();
        if (body != null) {
            CSharpParser.Class_member_declarationsContext members = body.class_member_declarations();
            if (members != null) {
                for (CSharpParser.Class_member_declarationContext m : members.class_member_declaration()) {
                    LangASTNode memberNode = mainBuilder.visit(m);
                    if (memberNode instanceof LangMethodDeclaration) {
                        type.addMethod((LangMethodDeclaration) memberNode);
                    } else if (memberNode != null) {
                        type.addChild(memberNode);
                    }
                }
            }
        }
        return type;
    }

    @Override
    public LangASTNode visitClass_member_declaration(CSharpParser.Class_member_declarationContext ctx) {
        // Extract attributes attached to this member
        List<LangAnnotation> annotations = extractAttributes(ctx.attributes());

        // Visit the actual member declaration
        LangASTNode result = null;
        if (ctx.common_member_declaration() != null) {
            result = mainBuilder.visit(ctx.common_member_declaration());
        } else if (ctx.destructor_definition() != null) {
            result = mainBuilder.visit(ctx.destructor_definition());
        }

        // Attach annotations like LangAnnotation to supported nodes
        if (result instanceof LangMethodDeclaration method) {
            if (!annotations.isEmpty()) {
                method.setLangAnnotations(annotations);
            }
            // Extract and set visibility from the full member declaration (includes modifiers)
            Visibility vis = parseVisibilityFromText(ctx.getText());
            method.setVisibility(vis);
        } else if (result instanceof LangTypeDeclaration) {
            LangTypeDeclaration type = (LangTypeDeclaration) result;
            if (!annotations.isEmpty()) {
                type.setLangAnnotations(annotations);
            }
        } else if (result != null) {
            for (LangAnnotation ann : annotations) {
                result.addChild(ann);
            }
        }
        return result;
    }

    @Override
    public LangASTNode visitCommon_member_declaration(CSharpParser.Common_member_declarationContext ctx) {
        // Handle methods so we can capture the return type which is outside method_declaration
        if (ctx.typed_member_declaration() != null) {
            CSharpParser.Typed_member_declarationContext t = ctx.typed_member_declaration();
            // Type appears before method_declaration
            String returnTypeText = t.type_() != null ? t.type_().getText() : null;
            LangASTNode inner;
            if (t.method_declaration() != null) {
                inner = mainBuilder.visit(t.method_declaration());
                if (inner instanceof LangMethodDeclaration && returnTypeText != null && !returnTypeText.isEmpty()) {
                    ((LangMethodDeclaration) inner).setReturnTypeAnnotation(returnTypeText);
                }
                return inner;
            }
            return super.visitCommon_member_declaration(ctx);
        }
        if (ctx.VOID() != null && ctx.method_declaration() != null) {
            LangASTNode methodNode = mainBuilder.visit(ctx.method_declaration());
            if (methodNode instanceof LangMethodDeclaration) {
                ((LangMethodDeclaration) methodNode).setReturnTypeAnnotation(TypeObjectEnum.VOID.name());
            }
            return methodNode;
        }
        return super.visitCommon_member_declaration(ctx);
    }

    @Override
    public LangASTNode visitMethod_declaration(CSharpParser.Method_declarationContext ctx) {
        String name = ctx.method_member_name() != null ? ctx.method_member_name().getText() : "";
        LangBlock bodyNode = null;
        if (ctx.method_body() != null && ctx.method_body().block() != null) {
            LangASTNode b = mainBuilder.visit(ctx.method_body().block());
            if (b instanceof LangBlock) {
                bodyNode = (LangBlock) b;
            }
        }
        // Parameters (fixed only, ignore params/array)
        List<LangSingleVariableDeclaration> params = new ArrayList<>();
        CSharpParser.Formal_parameter_listContext fpl = ctx.formal_parameter_list();
        if (fpl != null && fpl.fixed_parameters() != null) {
            for (CSharpParser.Fixed_parameterContext fp : fpl.fixed_parameters().fixed_parameter()) {
                CSharpParser.Arg_declarationContext arg = fp.arg_declaration();
                if (arg != null && arg.identifier() != null) {
                    String parameterName = arg.identifier().getText();
                    // TODO Extract default parameter value in C#
                    LangSingleVariableDeclaration param = LangASTNodeFactory.createSingleVariableDeclaration(parameterName, null, arg);
                    param.setParameter(true);
                    param.setTypeAnnotation(extractParameterType(fp));
                    params.add(param);
                }
            }
        }
        LangMethodDeclaration method = LangASTNodeFactory.createMethodDeclaration(name, ctx, params, bodyNode);

        method.setReturnTypeAnnotation(extractReturnType(ctx));

        method.setAbstract(extractIsAbstract(ctx));
        method.setConstructor(false);
        method.setStatic(extractIsStatic(ctx));
        method.setFinal(extractIsFinal(ctx));
        method.setNative(false);
        method.setSynchronized(false);


        return method;
    }

    // Helper methods

    private List<LangAnnotation> extractAttributes(CSharpParser.AttributesContext attrsCtx) {
        List<LangAnnotation> result = new ArrayList<>();
        if (attrsCtx == null) return result;
        for (CSharpParser.Attribute_sectionContext section : attrsCtx.attribute_section()) {
            if (section == null || section.attribute_list() == null) continue;
            for (CSharpParser.AttributeContext attr : section.attribute_list().attribute()) {
                LangAnnotation ann = buildAnnotationFromAttribute(attr);
                if (ann != null) result.add(ann);
            }
        }
        return result;
    }

    private LangAnnotation buildAnnotationFromAttribute(CSharpParser.AttributeContext attr) {
        if (attr == null || attr.namespace_or_type_name() == null) return null;
        String rawName = attr.namespace_or_type_name().getText();
        LangSimpleName nameNode = LangASTNodeFactory.createSimpleName(rawName, attr);
        LangAnnotation ann = LangASTNodeFactory.createAnnotation(attr, nameNode);
        // Arguments
        List<CSharpParser.Attribute_argumentContext> args = attr.attribute_argument();
        if (args != null) {
            for (CSharpParser.Attribute_argumentContext argCtx : args) {
                LangASTNode valueNode = null;
                if (argCtx.expression() != null) {
                    valueNode = mainBuilder.visit(argCtx.expression());
                }
                if (argCtx.identifier() != null) {
                    String key = argCtx.identifier().getText();
                    if (valueNode != null) ann.addMemberValuePair(key, valueNode);
                } else {
                    if (valueNode != null) ann.addArgument(valueNode);
                }
            }
        }
        return ann;
    }

    private TypeObjectEnum extractParameterType(CSharpParser.Fixed_parameterContext fp) {
        if (fp.arg_declaration() != null && fp.arg_declaration().type_() != null) {
            String typeText = fp.arg_declaration().type_().getText();
            if (!typeText.isEmpty()) {
                TypeObjectEnum typeEnum = TypeObjectEnum.fromType(typeText);
                return typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT;
            }
        }
        return TypeObjectEnum.OBJECT;
    }

    private String extractReturnType(CSharpParser.Method_declarationContext ctx) {

        for (int i = 0; i < ctx.getChildCount(); i++) {
            String childText = ctx.getChild(i).getText();
            if (isValidReturnType(childText)) {
                return childText;
            }
        }

        return null;
    }

    private boolean isValidReturnType(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.equals("void") ||
                lowerText.equals("int") ||
                lowerText.equals("string") ||
                lowerText.equals("bool") ||
                lowerText.equals("double") ||
                lowerText.equals("float") ||
                lowerText.equals("long") ||
                Character.isUpperCase(text.charAt(0));
    }

    private Visibility parseVisibilityFromText(String memberText) {
        if (memberText == null) return Visibility.PRIVATE;
        String t = memberText.toLowerCase();
        if (t.contains("private protected")) {
            return Visibility.PROTECTED;
        }
        if (t.contains("protected internal")) {
            return Visibility.PROTECTED;
        }
        if (t.contains("public")) {
            return Visibility.PUBLIC;
        }
        if (t.contains("private")) {
            return Visibility.PRIVATE;
        }
        if (t.contains("protected")) {
            return Visibility.PROTECTED;
        }
        if (t.contains("internal")) {
            return Visibility.PACKAGE; // maps to package/internal visibility
        }
        // Default for class members in C# is private
        return Visibility.PRIVATE;
    }


    private boolean extractIsAbstract(CSharpParser.Method_declarationContext ctx) {
        String contextText = ctx.getText().toLowerCase();
        return contextText.contains("abstract");
    }

    private boolean extractIsStatic(CSharpParser.Method_declarationContext ctx) {
        String contextText = ctx.getText().toLowerCase();
        return contextText.contains("static");
    }

    private boolean extractIsFinal(CSharpParser.Method_declarationContext ctx) {
        String contextText = ctx.getText().toLowerCase();
        return contextText.contains("sealed");
    }

}
