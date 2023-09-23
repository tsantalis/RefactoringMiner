package org.voltdb.sqlparser.semantics.grammar;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.NotNull;
import org.voltdb.sqlparser.semantics.symtab.CatalogAdapter;
import org.voltdb.sqlparser.semantics.symtab.Neutrino;
import org.voltdb.sqlparser.semantics.symtab.Type;
import org.voltdb.sqlparser.syntax.grammar.ErrorMessage;
import org.voltdb.sqlparser.syntax.grammar.ICatalog;
import org.voltdb.sqlparser.syntax.grammar.IInsertStatement;
import org.voltdb.sqlparser.syntax.grammar.IOperator;
import org.voltdb.sqlparser.syntax.grammar.ISelectQuery;
import org.voltdb.sqlparser.syntax.grammar.SQLParserBaseListener;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser;
import org.voltdb.sqlparser.syntax.grammar.ErrorMessage.Severity;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Column_definitionContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Column_nameContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Column_refContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Create_table_statementContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.ExpressionContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Insert_statementContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.ProjectionContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Select_statementContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Table_clauseContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Table_refContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.ValueContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Where_clauseContext;
import org.voltdb.sqlparser.syntax.symtab.IColumn;
import org.voltdb.sqlparser.syntax.symtab.IParserFactory;
import org.voltdb.sqlparser.syntax.symtab.ISymbolTable;
import org.voltdb.sqlparser.syntax.symtab.ITable;
import org.voltdb.sqlparser.syntax.symtab.IType;

public class DDLListener extends SQLParserBaseListener implements ANTLRErrorListener {
	private ITable m_currentlyCreatedTable = null;
    private ISymbolTable m_symbolTable;
    private IParserFactory m_factory;
    private ICatalog m_catalog;
    private List<ErrorMessage> m_errorMessages = new ArrayList<ErrorMessage>();
    private ISelectQuery m_selectQuery = null;
    private IInsertStatement m_insertStatement = null;

    public DDLListener(IParserFactory aFactory) {
        m_factory = aFactory;
        m_symbolTable = aFactory.getStandardPrelude();
        m_catalog = aFactory.getCatalog();
        m_selectQuery = null;
        m_insertStatement = null;
    }

    public boolean hasErrors() {
        return m_errorMessages.size() > 0;
    }

    private final void addError(int line, int col, String errorMessageFormat, Object ... args) {
        String msg = String.format(errorMessageFormat, args);
        m_errorMessages.add(new ErrorMessage(line,
                                             col,
                                             Severity.Error,
                                             msg));
    }

    public final List<ErrorMessage> getErrorMessages() {
        return m_errorMessages;
    }

    public String getErrorMessagesAsString() {
        StringBuffer sb = new StringBuffer();
        int nerrs = getErrorMessages().size();
        sb.append(String.format("\nOh, dear, there seem%s to be %serror%s here.\n",
                                nerrs > 1 ? "" : "s",
                                nerrs > 1 ? "" : "an ",
                                nerrs > 1 ? "s" : ""));
        for (ErrorMessage em : getErrorMessages()) {
            sb.append(String.format("line %d, column %d: %s\n", em.getLine(), em.getCol(), em.getMsg()));
        }
        return sb.toString();
    }

	/**
	 * {@inheritDoc}
	 */
	@Override public void exitColumn_definition(SQLParserParser.Column_definitionContext ctx) {
	    String colName = ctx.column_name().IDENTIFIER().getText();
	    String type = ctx.type_expression().type_name().IDENTIFIER().getText();
	    Type colType = (Type) m_symbolTable.getType(type);
	    if (colType == null) {
	        addError(ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Type expected");
	    } else {
	        m_currentlyCreatedTable.addColumn(colName, m_factory.newColumn(colName, colType));
	    }
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterCreate_table_statement(SQLParserParser.Create_table_statementContext ctx) {
	    String tableName = ctx.table_name().IDENTIFIER().getText();
	    m_currentlyCreatedTable = m_factory.newTable(tableName);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitCreate_table_statement(SQLParserParser.Create_table_statementContext ctx) {
	    m_catalog.addTable(m_currentlyCreatedTable);
	    m_currentlyCreatedTable = null;
	}

	@Override public void enterSelect_statement(SQLParserParser.Select_statementContext ctx) {
	    m_selectQuery = m_factory.newSelectQuery(m_symbolTable);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitSelect_statement(SQLParserParser.Select_statementContext ctx) {
	    m_factory.processQuery(m_selectQuery);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitProjection(SQLParserParser.ProjectionContext ctx) {
	    String tableName = null;
	    String columnName = ctx.projection_ref().column_name().IDENTIFIER().getText();
	    String alias = null;
	    if (ctx.projection_ref().table_name() != null) {
	        tableName = ctx.projection_ref().table_name().IDENTIFIER().getText();
	    }
	    if (ctx.column_name() != null) {
	        alias = ctx.column_name().IDENTIFIER().getText();
	    }
	    m_selectQuery.addProjection(tableName, columnName, alias);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitTable_clause(SQLParserParser.Table_clauseContext ctx) {
        for (SQLParserParser.Table_refContext tr : ctx.table_ref()) {
            String tableName = tr.table_name().get(0).IDENTIFIER().getText();
            String alias = null;
            if (tr.table_name().size() > 1) {
                alias = tr.table_name().get(1).IDENTIFIER().getText();
            }
            ITable table = m_catalog.getTableByName(tableName);
            if (table == null) {
                addError(tr.start.getLine(),
                         tr.start.getCharPositionInLine(),
                         "Cannot find table %s",
                         tableName);
            }
	        m_selectQuery.addTable(table, alias);
	    }
        while(m_selectQuery.hasNeutrinos()) {
        	m_selectQuery.popNeutrino();
        }
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitColumn_ref(@NotNull SQLParserParser.Column_refContext ctx) {
		String columnName = ctx.column_name().IDENTIFIER().getText();
		String tableName = null;
	    if (ctx.table_name() != null) {
	        tableName = ctx.table_name().IDENTIFIER().getText();
	    }
	    m_selectQuery.pushNeutrino(m_selectQuery.getColumnNeutrino(columnName,tableName));
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitWhere_clause(SQLParserParser.Where_clauseContext ctx) {
		Neutrino ret = (Neutrino) m_selectQuery.popNeutrino();
		if (!(ret != null && ret.isBooleanExpression())) { // check if expr is boolean
			addError(ctx.start.getLine(),
			        ctx.start.getCharPositionInLine(),
			        "Boolean expression expected");
		} else {
			// Push where statement, select knows if where exists and can pop it off if it does.
			m_selectQuery.setWhereCondition(ret);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 */
	@Override public void exitExpression(@NotNull SQLParserParser.ExpressionContext ctx) {
		List<ExpressionContext> exprs = ctx.expression();
		// If there is only one expression, the form is ( expression ), and we
		// just processed the inner expression.  So, only worry about
		// The case when there are 0 or 2 expressions.
		if (exprs.size() == 2) { // two expressions separated by something
		    // Calculate the operation.
		    String opString;
		    IOperator op;
		    if (ctx.timesop() != null) {
		        opString = ctx.timesop().getText();
		    } else if (ctx.addop() != null) {
		        opString = ctx.addop().getText();
		    } else if (ctx.relop() != null) {
		        opString = ctx.relop().getText();
		    } else {
		        addError(ctx.start.getLine(),
		                 ctx.start.getCharPositionInLine(),
		                 "Unknown operator");
		        return;
		    }
		    op = m_factory.getExpressionOperator(opString);
		    //
		    // Now, given the kind of operation, calculate the output.
		    //
		    Neutrino rightoperand = (Neutrino) m_selectQuery.popNeutrino();
		    Neutrino leftoperand = (Neutrino) m_selectQuery.popNeutrino();
		    Neutrino answer;
		    if (op.isArithmetic()) {
    		    answer = (Neutrino) m_selectQuery.getNeutrinoMath(op,
    		                                           leftoperand,
    		                                           rightoperand);
		    } else if (op.isRelational()) {
		        answer = (Neutrino) m_selectQuery.getNeutrinoCompare(op,
		                                                  leftoperand,
		                                                  rightoperand);
		    } else if (op.isBoolean()) {
		        answer = (Neutrino) m_selectQuery.getNeutrinoBoolean(op,
		                                                  leftoperand,
		                                                  rightoperand);
		    } else {
		        addError(ctx.start.getLine(),
		                 ctx.start.getCharPositionInLine(),
		                 "Internal Error: Unknown operation kind for operator \"%s\"",
		                 opString);
		        return;
		    }
		    if (answer == null) {
		        addError(ctx.start.getLine(),
		                ctx.start.getCharPositionInLine(),
		                "Incompatible argument types %s and %s",
		                leftoperand.getType().getName(),
		                rightoperand.getType().getName());
		        return;
		    }
		    m_selectQuery.pushNeutrino(answer);
		} else { // zero expressions.
			Column_refContext cref = ctx.column_ref();
			if (cref != null) {
			    String tableName = (cref.table_name() != null) ? cref.table_name().IDENTIFIER().getText() : null;
			    String columnName = cref.column_name().IDENTIFIER().getText();
			    Neutrino crefNeutrino = (Neutrino) m_selectQuery.getColumnNeutrino(columnName, tableName);
			    m_selectQuery.pushNeutrino(crefNeutrino);
			} else {
			    // TRUE,FALSE,or NUMBER constants.
				if (ctx.FALSE() != null) { // FALSE
				    Type boolType = (Type) m_factory.makeBooleanType();
					m_selectQuery.pushNeutrino(
							new Neutrino(boolType,
									     m_factory.makeUnaryAST(boolType, false)));
				} else if (ctx.TRUE() != null ) { // TRUE
				    Type boolType = (Type) m_factory.makeBooleanType();
					m_selectQuery.pushNeutrino(
							new Neutrino(boolType,
									    m_factory.makeUnaryAST(boolType, true)));
				} else { // must be NUMBER
				    Type intType = (Type) m_factory.makeIntegerType();
					m_selectQuery.pushNeutrino(
							new Neutrino(intType,
							             m_factory.makeUnaryAST(intType, Integer.valueOf(ctx.NUMBER().getText()))));
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitInsert_statement(SQLParserParser.Insert_statementContext ctx) {
	    String tableName = ctx.table_name().IDENTIFIER().getText();
	    ITable table = m_catalog.getTableByName(tableName);
	    if (table == null) {
	        addError(ctx.table_name().start.getLine(),
	                 ctx.table_name().start.getCharPositionInLine(),
	                 "Undefined table name %s",
	                 tableName);
	        return;
	    }
	    if (ctx.column_name_list().column_name().size() != ctx.values().value().size()) {
	        addError(ctx.column_name_list().start.getLine(),
	                 ctx.column_name_list().start.getCharPositionInLine(),
	                 (ctx.column_name_list().column_name().size() > ctx.values().value().size())
	                   ? "Too few values in insert statement."
	                   : "Too many values in insert statement.");
	        return;
	    }
	    m_insertStatement = m_factory.newInsertStatement();
	    m_insertStatement.addTable(table);
	    List<String> colNames = new ArrayList<String>();
	    List<IType>  colTypes = new ArrayList<IType>();
	    List<String> colVals  = new ArrayList<String>();
	    for (Column_nameContext colCtx : ctx.column_name_list().column_name()) {
	        String colName = colCtx.IDENTIFIER().getText();
	        IColumn col = table.getColumnByName(colName);
	        if (col == null) {
	            addError(colCtx.start.getLine(),
	                     colCtx.start.getCharPositionInLine(),
	                     "Undefined column name %s in table %s",
	                     colName,
	                     tableName);
	            return;
	        }
	        IType colType = col.getType();
	        colNames.add(colName);
	        colTypes.add(colType);
	    }
	    for (ValueContext val : ctx.values().value()) {
	        String valStr = val.NUMBER().getText();
	        colVals.add(valStr);
	    }
	    for (int idx = 0; idx < colNames.size(); idx += 1) {
	        m_insertStatement.addColumn(colNames.get(idx),
	                                    colTypes.get(idx),
	                                    colVals.get(idx));
	    }
	}

    @Override
    public void reportAmbiguity(Parser aArg0, DFA aArg1, int aArg2, int aArg3,
            boolean aArg4, java.util.BitSet aArg5, ATNConfigSet aArg6) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportAttemptingFullContext(Parser aArg0, DFA aArg1, int aArg2,
            int aArg3, java.util.BitSet aArg4, ATNConfigSet aArg5) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportContextSensitivity(Parser aArg0, DFA aArg1, int aArg2,
            int aArg3, int aArg4, ATNConfigSet aArg5) {
    }

    @Override
    public void syntaxError(Recognizer<?, ?> aArg0, Object aTokObj, int aLine,
            int aCol, String msg, RecognitionException aArg5) {
        addError(aLine, aCol, msg);
    }

    public final ISelectQuery getSelectQuery() {
        return m_selectQuery;
    }

    public final IInsertStatement getInsertStatement() {
        return m_insertStatement;
    }

    public CatalogAdapter getCatalogAdapter() {
        assert(m_catalog instanceof CatalogAdapter);
        return (CatalogAdapter)m_catalog;
    }

    protected final IParserFactory getFactory() {
        return m_factory;
    }

}
