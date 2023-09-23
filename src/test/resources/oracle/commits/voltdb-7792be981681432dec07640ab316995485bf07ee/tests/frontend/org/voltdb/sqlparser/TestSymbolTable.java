package org.voltdb.sqlparser;

import static org.voltdb.sqlparser.matchers.SymbolTableAssert.assertThat;

import org.junit.Test;
import org.voltdb.sqlparser.semantics.symtab.IntegerType;
import org.voltdb.sqlparser.semantics.symtab.SymbolTable;

public class TestSymbolTable {
    @Test
    public void test() {
        SymbolTable s = new SymbolTable(null);
        assertThat(s).isEmpty();
        assertThat(s).hasSize(0);
        IntegerType bigint = new IntegerType("bigint", 8, 8);
        s.define(bigint);
        assertThat(s).hasSize(1)
                     .definesType("bigint")
                     .hasMaxSize(8)
                     .hasNominalSize(8);
    }
}
