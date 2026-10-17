package extension.ast.node;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import extension.base.Utf16CharStream;
import extension.base.lang.python.PythonLexer;
import extension.base.lang.python.PythonParser;

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

    //the NEWLINE, INDENT and DEDENT tokens of the Python lexer are layout, i.e., a DEDENT ends right before the next statement,
    //so the range of a rule ending with a block would include the following blank lines and comments
    private static boolean isLayout(Token token) {
        if (!(token.getTokenSource() instanceof PythonLexer))
            return false;
        int type = token.getType();
        return type == PythonLexer.NEWLINE || type == PythonLexer.INDENT || type == PythonLexer.DEDENT;
    }

    private static Token start(ParserRuleContext ctx) {
        if (ctx == null || ctx.getStart() == null || !isLayout(ctx.getStart()))
            return ctx == null ? null : ctx.getStart();
        Token first = boundaryToken(ctx, true);
        return first != null ? first : ctx.getStart();
    }

    private static Token stop(ParserRuleContext ctx) {
        //the module ends at the end of the file, including its trailing comments
        if (ctx == null || ctx.getStop() == null || !isLayout(ctx.getStop()) || ctx instanceof PythonParser.File_inputContext)
            return ctx == null ? null : ctx.getStop();
        Token last = boundaryToken(ctx, false);
        if (last == null)
            return ctx.getStop();
        if (ctx.getStop().getType() == PythonLexer.DEDENT) {
            Token comment = lastTrailingComment(ctx, last);
            if (comment != null)
                return comment;
        }
        return last;
    }

    //the comments before a dedent belong to the block when they are indented like its statements, as in the tree-sitter (and AST diff) tree,
    //so a rule ending with a block also ends with them, up to the first comment indented less than the block
    private static Token lastTrailingComment(ParserRuleContext ctx, Token last) {
        ParserRuleContext block = trailingBlock(ctx);
        CharStream stream = last.getInputStream();
        Token firstStatement = block == null ? null : boundaryToken(block, true);
        if (firstStatement == null || stream == null)
            return null;
        int blockIndent = indentation(stream, firstStatement.getStartIndex());
        int line = last.getLine();
        int commentStart = -1, commentEnd = -1, commentLine = -1;
        int i = last.getStopIndex() + 1;
        int to = ctx.getStop().getStopIndex();
        while (i <= to) {
            char c = charAt(stream, i);
            if (c == '#') {
                if (indentation(stream, i) < blockIndent)
                    break;
                commentStart = i;
                while (i <= to && charAt(stream, i) != '\n' && charAt(stream, i) != '\r')
                    i++;
                commentEnd = i - 1;
                commentLine = line;
            }
            else if (c == '\n' || (c == '\r' && (i == to || charAt(stream, i + 1) != '\n'))) {
                line++;
                i++;
            }
            else if (Character.isWhitespace(c)) {
                i++;
            }
            else {
                break;
            }
        }
        if (commentStart < 0)
            return null;
        CommonToken comment = new CommonToken(new Pair<>(last.getTokenSource(), stream), PythonLexer.COMMENT, Token.HIDDEN_CHANNEL, commentStart, commentEnd);
        comment.setLine(commentLine);
        comment.setText(stream.getText(Interval.of(commentStart, commentEnd)));
        return comment;
    }

    //the outermost block ending the rule, i.e., the body of a function or the else block of an if statement
    private static ParserRuleContext trailingBlock(ParseTree tree) {
        while (tree != null && !(tree instanceof PythonParser.BlockContext)) {
            ParseTree next = null;
            for (int i = tree.getChildCount() - 1; i >= 0 && next == null; i--) {
                ParseTree child = tree.getChild(i);
                if (!(child instanceof TerminalNode terminal && isLayout(terminal.getSymbol())))
                    next = child;
            }
            tree = next instanceof TerminalNode ? null : next;
        }
        return (ParserRuleContext) tree;
    }

    private static char charAt(CharStream stream, int index) {
        return stream.getText(Interval.of(index, index)).charAt(0);
    }

    //the indentation of the line of the offset, with tabs as 8 spaces like the tree-sitter scanner and PythonTreeGenerator
    private static int indentation(CharStream stream, int offset) {
        int lineStart = offset;
        while (lineStart > 0 && charAt(stream, lineStart - 1) != '\n' && charAt(stream, lineStart - 1) != '\r')
            lineStart--;
        int indent = 0;
        for (int i = lineStart; i < offset; i++) {
            char character = charAt(stream, i);
            if (character == '\t')
                indent += 8;
            else if (character == '\f')
                indent = 0;
            else if (character == ' ')
                indent++;
            else
                break;
        }
        return indent;
    }

    //the first or last token of the tree that is not layout
    private static Token boundaryToken(ParseTree tree, boolean first) {
        if (tree instanceof TerminalNode terminal)
            return isLayout(terminal.getSymbol()) ? null : terminal.getSymbol();
        int count = tree.getChildCount();
        for (int i = 0; i < count; i++) {
            Token token = boundaryToken(tree.getChild(first ? i : count - 1 - i), first);
            if (token != null)
                return token;
        }
        return null;
    }

    public static int getStartLine(ParserRuleContext ctx) {
        Token start = start(ctx);
        return (start != null) ? start.getLine() : 1;  // Default to line 1
    }

    public static int getEndLine(ParserRuleContext ctx) {
        Token stop = stop(ctx);
        if (stop != null) {
            return stop.getLine();
        }
        // Fallback to start line
        return getStartLine(ctx);
    }

    public static int getStartOffset(ParserRuleContext ctx) {
        Token start = start(ctx);
        return (start != null) ? start.getStartIndex() : 0;  // Default to 0
    }

    public static int getEndOffset(ParserRuleContext ctx) {
        Token stop = stop(ctx);
        if (stop != null) {
            return stop.getStopIndex();  // +1 is not needed
        }
        // Fallback to start offset
        return getStartOffset(ctx);
    }

    public static int getStartColumn(ParserRuleContext ctx) {
        Token start = start(ctx);
        return (start != null) ? getColumn(start) : 0;  // Default to 0
    }

    //the lexer counts the columns in code points, while the UTF-16 stream gives the column in UTF-16 offsets, like the offsets
    private static int getColumn(Token token) {
        if (token.getInputStream() instanceof Utf16CharStream stream && token.getStartIndex() >= 0)
            return stream.column(token.getStartIndex());
        return token.getCharPositionInLine();
    }

    public static int getEndColumn(ParserRuleContext ctx) {
        Token stop = stop(ctx);
        if (stop != null) {
            return getColumn(stop) + stop.getText().length();  // Add token length
        }
        // Fallback to start column
        return getStartColumn(ctx);
    }
}