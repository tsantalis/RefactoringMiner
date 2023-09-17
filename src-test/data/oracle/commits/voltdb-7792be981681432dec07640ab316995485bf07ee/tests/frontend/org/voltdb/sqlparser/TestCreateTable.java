package org.voltdb.sqlparser;

import static org.voltdb.sqlparser.symtab.CatalogAdapterAssert.assertThat;
import static org.voltdb.sqlparser.symtab.ColumnAssert.withColumnTypeNamed;
import static org.voltdb.sqlparser.symtab.TableAssert.withColumnNamed;

import java.io.IOException;

import org.junit.Test;
import org.voltdb.sqlparser.semantics.grammar.DDLListener;
import org.voltdb.sqlparser.semantics.symtab.CatalogAdapter;
import org.voltdb.sqlparser.semantics.symtab.ParserFactory;
import org.voltdb.sqlparser.syntax.SQLParserDriver;

public class TestCreateTable {

    @Test
    public void testMultiTableCreation() throws IOException {
        testDDL1("create table alpha ( id bigint );");
        testDDL2("create table beta ( id bigint not null, local tinyint not null );");
    }

    private void testDDL1(String ddl) throws IOException {
        CatalogAdapter catalog = new CatalogAdapter();
        ParserFactory factory = new ParserFactory(catalog);
        DDLListener listener = new DDLListener(factory);
        SQLParserDriver driver = new SQLParserDriver(ddl, null);
        driver.walk(listener);
        assertThat(catalog)
            .hasTableNamed("alpha",
                      withColumnNamed("id",
                                      withColumnTypeNamed("bigint")));
    }

    private void testDDL2(String ddl) throws IOException {
        CatalogAdapter catalog = new CatalogAdapter();
        ParserFactory factory = new ParserFactory(catalog);
        DDLListener listener = new DDLListener(factory);
        SQLParserDriver driver = new SQLParserDriver(ddl, null);
        driver.walk(listener);

        assertThat(catalog)
            .hasTableNamed("beta",
                      withColumnNamed("id",
                                      withColumnTypeNamed("bigint")),
                      withColumnNamed("local",
                                     withColumnTypeNamed("tinyint")));
    }
}
