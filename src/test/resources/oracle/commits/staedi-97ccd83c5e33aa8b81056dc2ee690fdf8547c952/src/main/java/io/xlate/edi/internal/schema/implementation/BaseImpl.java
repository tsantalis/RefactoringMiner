package io.xlate.edi.internal.schema.implementation;

import java.util.Objects;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public abstract class BaseImpl<T extends EDIType> implements EDITypeImplementation {

    protected String typeId;
    protected T standard;
    protected int minOccurs = -1;
    protected int maxOccurs = -1;
    protected String title;
    protected String description;

    protected BaseImpl(String title, String description) {
        super();
        this.title = title;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().isInstance(o)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        BaseImpl<T> other = (BaseImpl<T>) o;

        return Objects.equals(typeId, other.typeId) &&
                Objects.equals(standard, other.standard) &&
                Objects.equals(minOccurs, other.minOccurs) &&
                Objects.equals(maxOccurs, other.maxOccurs) &&
                Objects.equals(title, other.title) &&
                Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId, standard, minOccurs, maxOccurs, title, description);
    }

    @Override
    public String getId() {
        return typeId;
    }

    @Override
    public String getCode() {
        return typeId;
    }

    @Override
    public EDIType getReferencedType() {
        return getStandard();
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getTypeId() {
        return typeId;
    }

    public T getStandard() {
        return standard;
    }

    @SuppressWarnings("unchecked")
    public void setStandardReference(EDIReference reference) {
        this.standard = (T) reference.getReferencedType();

        if (this.typeId == null) {
            this.typeId = standard.getId();
        }

        if (this.minOccurs < 0) {
            this.minOccurs = reference.getMinOccurs();
        }

        if (this.maxOccurs < 0) {
            this.maxOccurs = reference.getMaxOccurs();
        }
    }

}
