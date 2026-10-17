package extension.ast.node;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import extension.base.Utf16CharStream;

public class PositionUtils {

    public static PositionInfo getPositionInfo(ParserRuleContext ctx) {
        if (ctx == null) {
            return new PositionInfo(-1, -1, -1, -1, -1, -1);
        }

        // Validate that we have valid tokens
        int startOffset = getStartOffset(ctx);
        int endOffset = getEndOffset(ctx);

        // Fix invalid ranges
        if (startOffset < 0) startOffset = 0;
        if (endOffset < startOffset) endOffset = startOffset;

        return new PositionInfo(
                getStartLine(ctx),
                getEndLine(ctx),
                startOffset,
                endOffset,
                getStartColumn(ctx),
                getEndColumn(ctx)
        );
    }

    public static int getStartLine(ParserRuleContext ctx) {
        Token start = (ctx == null) ? null : ctx.getStart();
        return (start != null) ? start.getLine() : 1;  // Default to line 1
    }

    public static int getEndLine(ParserRuleContext ctx) {
        Token stop = (ctx == null) ? null : ctx.getStop();
        if (stop != null) {
            return stop.getLine();
        }
        // Fallback to start line
        return getStartLine(ctx);
    }

    public static int getStartOffset(ParserRuleContext ctx) {
        Token start = (ctx == null) ? null : ctx.getStart();
        return (start != null) ? start.getStartIndex() : 0;  // Default to 0
    }

    public static int getEndOffset(ParserRuleContext ctx) {
        Token stop = (ctx == null) ? null : ctx.getStop();
        if (stop != null) {
            return stop.getStopIndex();  // +1 is not needed
        }
        // Fallback to start offset
        return getStartOffset(ctx);
    }

    public static int getStartColumn(ParserRuleContext ctx) {
        Token start = (ctx == null) ? null : ctx.getStart();
        return (start != null) ? getColumn(start) : 0;  // Default to 0
    }

    //the lexer counts the columns in code points, while the UTF-16 stream gives the column in UTF-16 offsets, like the offsets
    private static int getColumn(Token token) {
        if (token.getInputStream() instanceof Utf16CharStream stream && token.getStartIndex() >= 0)
            return stream.column(token.getStartIndex());
        return token.getCharPositionInLine();
    }

    public static int getEndColumn(ParserRuleContext ctx) {
        Token stop = (ctx == null) ? null : ctx.getStop();
        if (stop != null) {
            return getColumn(stop) + stop.getText().length();  // Add token length
        }
        // Fallback to start column
        return getStartColumn(ctx);
    }
}