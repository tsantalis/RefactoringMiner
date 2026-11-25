package gr.uom.java.xmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.dom.parser.IScannerExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.ISourceCodeParser;
import org.eclipse.cdt.core.dom.parser.cpp.GPPScannerExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.cpp.ICPPParserExtensionConfiguration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.EndOfFileException;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IParserSettings;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.core.parser.ParserMode;
import org.eclipse.cdt.internal.core.dom.parser.BacktrackException;
import org.eclipse.cdt.internal.core.dom.parser.DeclarationOptions;
import org.eclipse.cdt.internal.core.dom.parser.cpp.GNUCPPSourceParser;

//the GNU C++ language with the C++20 features of CDT, i.e., concepts and operator<=>, which CDT enables only when their feature-test macros are defined,
//with C++20 designated initializers in return statements and braced initializers, i.e., return {.x = 1, .y = 2}; and P p{.x = 1, .y = 2};
//and with the constraints of concepts and requires clauses as the standard defines them, i.e., concept C = sizeof(T) > 1; and requires C<T> int f();
public class CppLanguage extends GPPLanguage {
	private static final CppLanguage DEFAULT = new CppLanguage();
	private static final IScannerExtensionConfiguration CPP20_SCANNER_CONFIGURATION = new GPPScannerExtensionConfiguration() {
		{
			addMacro("__cpp_concepts", "201907L");
			addMacro("__cpp_impl_three_way_comparison", "201907L");
		}
	};

	public static CppLanguage getDefault() {
		return DEFAULT;
	}

	@Override
	protected IScannerExtensionConfiguration getScannerExtensionConfiguration(IScannerInfo info) {
		return CPP20_SCANNER_CONFIGURATION;
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
				expr = bracedInitList(true, designatedInitializerList());
			} else if (lt1 != IToken.tSEMI) {
				expr = expression();
			}
			final int endOffset = consumeOrEOC(IToken.tSEMI).getEndOffset();
			return setRange(getNodeFactory().newReturnStatement(expr), offset, endOffset);
		}

		//the same as GNUCPPSourceParser.optionalInitializer, except that the braced initializer allows designators, i.e., P p{.x = 1};
		@Override
		protected IASTInitializer optionalInitializer(IASTDeclarator declarator, DeclarationOptions options) throws EndOfFileException, BacktrackException {
			if (options.fAllowBracedInitializer && LTcatchEOF(1) == IToken.tLBRACE && designatedInitializerList()) {
				return (IASTInitializer) bracedInitList(false, true);
			}
			return super.optionalInitializer(declarator, options);
		}

		//a C++20 braced initializer list is designated when its first clause is, since its clauses are either all designated or none,
		//and its designators are only fields, i.e., {.x = 1}, while the GNU array designators of CDT would take the lambda in {[a](int b) { ... }} for one
		private boolean designatedInitializerList() {
			return LTcatchEOF(1) == IToken.tLBRACE && LTcatchEOF(2) == IToken.tDOT;
		}

		//the constraint of a concept is a logical-or-expression, i.e., concept C = sizeof(T) > 1;, while CDT allows only the primary expressions of requires clauses
		@Override
		protected IASTExpression constraintExpression() throws EndOfFileException, BacktrackException {
			if (LTcatchEOF(1) == IToken.t_requires) {
				return super.constraintExpression();
			}
			return constantExpression();
		}

		//a requires clause is a conjunction or disjunction of primary expressions, i.e., C<T> in requires C<T> int f();,
		//which CDT parses as an expression continuing into the declaration, i.e., C < T > int(...)
		@Override
		protected IASTExpression requiresClause() throws EndOfFileException, BacktrackException {
			if (LTcatchEOF(1) != IToken.t_requires) {
				return null;
			}
			consume();
			IASTExpression disjunction = constraintConjunction();
			while (LT(1) == IToken.tOR) {
				consume();
				IASTExpression conjunction = constraintConjunction();
				disjunction = buildBinaryExpression(IASTBinaryExpression.op_logicalOr, disjunction, conjunction, calculateEndOffset(conjunction));
			}
			return disjunction;
		}

		private IASTExpression constraintConjunction() throws EndOfFileException, BacktrackException {
			IASTExpression conjunction = primaryExpression(CastExprCtx.eNotInBExpr, null);
			while (LT(1) == IToken.tAND) {
				consume();
				IASTExpression primary = primaryExpression(CastExprCtx.eNotInBExpr, null);
				conjunction = buildBinaryExpression(IASTBinaryExpression.op_logicalAnd, conjunction, primary, calculateEndOffset(primary));
			}
			return conjunction;
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
