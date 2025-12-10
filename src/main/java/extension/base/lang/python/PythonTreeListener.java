package extension.base.lang.python;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class PythonTreeListener extends Python3ParserBaseListener {
    private int indentLevel = 0;

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        // Print indentation based on the tree depth
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  ");
        }

        // Print the rule name instead of raw text for readability
        String ruleName = Python3Parser.ruleNames[ctx.getRuleIndex()];
        System.out.println(ruleName);

        // Increase indentation level for child nodes
        indentLevel++;
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        // Decrease indentation level after finishing this rule
        indentLevel--;
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        String tokenText = node.getText();

        // Print indentation based on the current depth
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  ");
        }

        // Print the token itself
        System.out.println(tokenText);
    }

}

