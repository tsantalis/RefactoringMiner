package io.xlate.edi.internal.schema.implementation;

import java.util.List;
import java.util.Objects;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public abstract class BaseComplexImpl extends BaseImpl<EDIComplexType> {

    protected final List<EDITypeImplementation> sequence;

    protected BaseComplexImpl(List<EDITypeImplementation> sequence, String title, String description) {
        super(title, description);
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && Objects.equals(sequence, ((BaseComplexImpl) o).sequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sequence);
    }

    public List<EDITypeImplementation> getSequence() {
        return sequence;
    }
}
