package org.voltdb.sqlparser;

import org.junit.Test;
import org.voltdb.sqlparser.semantics.symtab.IntegerType;
import org.voltdb.sqlparser.semantics.symtab.SymbolTable;
import static org.voltdb.sqlparser.symtab.SymbolTableAssert.assertThat;
public class TestSymbolTable {
    @Test
    public void test() {
        SymbolTable s = new SymbolTable(null);
        assertThat(s).isEmpty();
        IntegerType bigint = new IntegerType("bigint", 8, 8);
        s.define(bigint);
        assertThat(s).hasSize(1)
                     .definesType("bigint")
                     .hasMaxSize(8)
                     .hasNominalSize(8);
    }
}
