package gr.uom.java.xmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.dom.parser.ISourceCodeParser;
import org.eclipse.cdt.core.dom.parser.cpp.ICPPParserExtensionConfiguration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.EndOfFileException;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IParserSettings;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.core.parser.ParserMode;
import org.eclipse.cdt.internal.core.dom.parser.BacktrackException;
import org.eclipse.cdt.internal.core.dom.parser.cpp.GNUCPPSourceParser;

//the GNU C++ language with C++20 designated initializers in return statements, i.e., return {.x = 1, .y = 2};
public class CppLanguage extends GPPLanguage {
	private static final CppLanguage DEFAULT = new CppLanguage();

	public static CppLanguage getDefault() {
		return DEFAULT;
	}

	@Override
	protected ISourceCodeParser createParser(IScanner scanner, ParserMode parserMode, IParserLogService logService, IIndex index) {
		return new Parser(scanner, parserMode, logService, getParserExtensionConfiguration(), index);
	}

	@Override
	protected ISourceCodeParser createParser(IScanner scanner, ParserMode parserMode, IParserLogService logService, IIndex index, int options, IParserSettings settings) {
		Parser parser = new Parser(scanner, parserMode, logService, getParserExtensionConfiguration(), index);
		if(settings != null) {
			int maximumTrivialExpressions = settings.getMaximumTrivialExpressionsInAggregateInitializers();
			if(maximumTrivialExpressions >= 0 && (options & OPTION_SKIP_TRIVIAL_EXPRESSIONS_IN_AGGREGATE_INITIALIZERS) != 0) {
				parser.setMaximumTrivialExpressionsInAggregateInitializers(maximumTrivialExpressions);
			}
		}
		return parser;
	}

	private static class Parser extends GNUCPPSourceParser {
		private static final Method BRACED_INIT_LIST;
		static {
			try {
				BRACED_INIT_LIST = GNUCPPSourceParser.class.getDeclaredMethod("bracedInitList", boolean.class, boolean.class);
				BRACED_INIT_LIST.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
		}

		private Parser(IScanner scanner, ParserMode parserMode, IParserLogService logService, ICPPParserExtensionConfiguration config, IIndex index) {
			super(scanner, parserMode, logService, config, index);
		}

		//the same as GNUCPPSourceParser.parseReturnStatement, except that the braced initializer list allows designators
		@Override
		protected IASTStatement parseReturnStatement() throws EndOfFileException, BacktrackException {
			final int offset = consume(IToken.t_return).getOffset();
			IASTInitializerClause expr = null;
			final int lt1 = LT(1);
			if (lt1 == IToken.tLBRACE) {
				expr = bracedInitList(true, true);
			} else if (lt1 != IToken.tSEMI) {
				expr = expression();
			}
			final int endOffset = consumeOrEOC(IToken.tSEMI).getEndOffset();
			return setRange(getNodeFactory().newReturnStatement(expr), offset, endOffset);
		}

		private IASTInitializerClause bracedInitList(boolean allowSkipping, boolean allowDesignators) throws EndOfFileException, BacktrackException {
			try {
				return (IASTInitializerClause) BRACED_INIT_LIST.invoke(this, allowSkipping, allowDesignators);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof EndOfFileException eof)
					throw eof;
				if (e.getCause() instanceof BacktrackException backtrack)
					throw backtrack;
				throw new IllegalStateException(e.getCause());
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
