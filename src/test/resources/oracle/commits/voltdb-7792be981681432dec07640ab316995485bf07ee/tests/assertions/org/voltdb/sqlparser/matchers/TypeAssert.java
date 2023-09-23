package org.voltdb.sqlparser.matchers;

import org.assertj.core.api.AbstractAssert;
import org.voltdb.sqlparser.semantics.symtab.Type;

public class TypeAssert extends AbstractAssert<TypeAssert, Type> {

    protected TypeAssert(Type aActual) {
        super(aActual, TypeAssert.class);
        // TODO Auto-generated constructor stub
    }

    public TypeAssert hasName(String aTypeName) {
        isNotNull();
        if (actual.getName().equalsIgnoreCase(aTypeName)) {
            failWithMessage("Expected type named <%s>.", aTypeName);
        }
        return this;
    }

    public TypeAssert hasMaxSize(int aMaxSize) {
        isNotNull();
        if (actual.getMaxSize() != aMaxSize) {
            failWithMessage("Expected type name <%s> to have max size %d not %d",
                            actual.getName(), aMaxSize, actual.getMaxSize());
        }
        return this;
    }

    public TypeAssert hasNominalSize(int aNominalSize) {
        isNotNull();
        if (actual.getNominalSize() != aNominalSize) {
            failWithMessage("Expected type name <%s> to have nominal size %d not %d",
                            actual.getName(), aNominalSize, actual.getNominalSize());
        }
        return this;
    }

}
