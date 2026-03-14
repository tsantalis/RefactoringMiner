package extension.ast.builder.python;

import extension.ast.node.OperatorEnum;
import extension.base.lang.python.PythonParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

public class PyASTBuilderUtil {

    public static String extractOperator(ParserRuleContext ctx) {
        if (ctx instanceof PythonParser.Compare_op_bitwise_or_pairContext pair) {
            return extractComparisonOperator(pair);
        }

        if (ctx.getChildCount() >= 2) {
            for (int i = 0; i < ctx.getChildCount(); i++) {
                String text = ctx.getChild(i).getText();
                if (OperatorEnum.isOperator(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String extractComparisonOperator(PythonParser.Compare_op_bitwise_or_pairContext pair) {
        if (pair.eq_bitwise_or() != null) return OperatorEnum.EQUAL.getSymbol();
        else if (pair.noteq_bitwise_or() != null) return OperatorEnum.NOT_EQUAL.getSymbol();
        else if (pair.lte_bitwise_or() != null) return OperatorEnum.LESS_EQUAL.getSymbol();
        else if (pair.lt_bitwise_or() != null) return OperatorEnum.LESS_THAN.getSymbol();
        else if (pair.gte_bitwise_or() != null) return OperatorEnum.GREATER_EQUAL.getSymbol();
        else if (pair.gt_bitwise_or() != null) return OperatorEnum.GREATER_THAN.getSymbol();
        else if (pair.notin_bitwise_or() != null) return OperatorEnum.NOT_IN.getSymbol();
        else if (pair.in_bitwise_or() != null) return OperatorEnum.IN.getSymbol();
        else if (pair.isnot_bitwise_or() != null) return OperatorEnum.IS_NOT.getSymbol();
        else if (pair.is_bitwise_or() != null) return OperatorEnum.IS.getSymbol();

        return null;
    }

    public static boolean isStringLiteral(PythonParser.AtomContext ctx) {
        if (ctx == null || ctx.strings() == null) {
            return false;
        }

        // If any child is an fstring or tstring, its not a literal
        for (ParseTree child : ctx.strings().children) {
            if (child instanceof PythonParser.FstringContext || child instanceof PythonParser.TstringContext) {
                return false;
            }
        }

        return true;
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
