package extension.ast.builder.python;

import extension.ast.node.OperatorEnum;
import extension.base.lang.python.Python3Parser;

public class PyASTBuilderUtil {

    public static String extractOperator(Python3Parser.ExprContext ctx) {

        if (ctx.getChildCount() == 3) {
            return ctx.getChild(1).getText();
        }

        if (ctx.getChildCount() > 3) {
            for (int i = 0; i < ctx.getChildCount(); i++) {
                String text = ctx.getChild(i).getText();
                if (isOperator(text)) {
                    return text;
                }
            }

            StringBuilder operators = new StringBuilder();
            for (int i = 0; i < ctx.getChildCount(); i++) {
                String text = ctx.getChild(i).getText();
                if (isOperator(text)) {
                    if (!operators.isEmpty()) {
                        operators.append(",");
                    }
                    operators.append(text);
                }
            }

            if (!operators.isEmpty()) {
                return operators.toString();
            }
        }

        //System.out.println("No operator found, returning null");
        return null;
    }

    // Helper method to identify operators
    private static boolean isOperator(String text) {
        return OperatorEnum.isOperator(text);
    }

    public static boolean isListLiteral(Python3Parser.AtomContext ctx) {
        return ctx.OPEN_BRACK() != null && ctx.CLOSE_BRACK() != null;
    }

    public static boolean isStringLiteral(Python3Parser.AtomContext ctx) {
        if (ctx == null) {
            return false;
        }

        if (ctx.STRING() != null && !ctx.STRING().isEmpty()) {
            return true;
        }

        String text = ctx.getText();
        if (text == null || text.isEmpty()) {
            return false;
        }

        // f-strings and complex strings
        if (text.length() >= 2) {
            String lower = text.toLowerCase();
            // Check for any string prefix followed by quotes
            if ((lower.startsWith("f") || lower.startsWith("r") || lower.startsWith("b") ||
                    lower.startsWith("u") || lower.startsWith("fr") || lower.startsWith("rf")) &&
                    (text.contains("\"") || text.contains("'"))) {
                return true;
            }

            // Check for regular strings
            return text.startsWith("\"") || text.startsWith("'") ||
                    text.startsWith("\"\"\"") || text.startsWith("'''");
        }

        return false;
    }

    public static String removeQuotes(String text) {
        String unquotedValue = null;
        if (text.startsWith("\"\"\"") && text.endsWith("\"\"\"")) {
            // Triple quotes
            unquotedValue = text.substring(3, text.length() - 3);
        } else if (text.startsWith("'''") && text.endsWith("'''")) {
            // Triple single quotes
            unquotedValue = text.substring(3, text.length() - 3);
        } else if (text.startsWith("r\"") && text.endsWith("\"")) {
            // Raw string literal
            unquotedValue = text.substring(2, text.length() - 1);
        } else if ((text.startsWith("\"") && text.endsWith("\"")) ||
                (text.startsWith("'") && text.endsWith("'"))) {
            // Regular quotes or single quotes
            unquotedValue = text.substring(1, text.length() - 1);
        }
        return unquotedValue;
    }

}
