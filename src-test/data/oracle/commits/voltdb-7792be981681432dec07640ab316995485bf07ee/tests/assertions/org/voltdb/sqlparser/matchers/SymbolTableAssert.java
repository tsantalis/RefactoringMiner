package org.voltdb.sqlparser.matchers;

import org.assertj.core.api.AbstractAssert;
import org.voltdb.sqlparser.semantics.symtab.SymbolTable;
import org.voltdb.sqlparser.semantics.symtab.Type;

public class SymbolTableAssert extends AbstractAssert<SymbolTableAssert, SymbolTable> {
    public SymbolTableAssert(SymbolTable aSymTab) {
        super(aSymTab, SymbolTableAssert.class);
    }

    public static SymbolTableAssert assertThat(SymbolTable aSymTab) {
        return new SymbolTableAssert(aSymTab);
    }
    /**
     * True iff the symbol table is empty.
     */
    public SymbolTableAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected empty symbol table");
        }
        return this;
    };

    public SymbolTableAssert hasSize(int aSize) {
        isNotNull();
        if (actual.size() != aSize) {
            failWithMessage("Expected %d elements, not %d", aSize, actual.size());
        }
        return this;
    }

    public TypeAssert definesType(String aTypeName) {
        isNotNull();
        Type t = actual.getType(aTypeName);
        if (t == null) {
            failWithMessage("Expected type <%s> to be defined", aTypeName);
        }
        return new TypeAssert(t);
    }
}
