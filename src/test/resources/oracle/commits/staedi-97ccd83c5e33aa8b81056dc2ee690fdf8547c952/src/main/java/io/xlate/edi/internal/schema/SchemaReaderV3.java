package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.internal.schema.implementation.BaseComplexImpl;
import io.xlate.edi.internal.schema.implementation.BaseImpl;
import io.xlate.edi.internal.schema.implementation.CompositeImpl;
import io.xlate.edi.internal.schema.implementation.DiscriminatorImpl;
import io.xlate.edi.internal.schema.implementation.ElementImpl;
import io.xlate.edi.internal.schema.implementation.LoopImpl;
import io.xlate.edi.internal.schema.implementation.Positioned;
import io.xlate.edi.internal.schema.implementation.SegmentImpl;
import io.xlate.edi.internal.schema.implementation.TransactionImpl;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.EDIType.Type;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;

class SchemaReaderV3 extends SchemaReaderBase implements SchemaReader {

    private static final Logger LOGGER = Logger.getLogger(SchemaReaderV3.class.getName());

    private static final String ATTR_POSITION = "position";
    private static final String ATTR_DISCRIMINATOR = "discriminator";

    final Deque<EDITypeImplementation> implementedTypes = new LinkedList<>();

    static class ValueSet {
        Set<String> value;

        void set(Set<String> value) {
            this.value = value;
        }

        Set<String> get() {
            return this.value != null ? this.value : Collections.emptySet();
        }

        void clear() {
            this.value = null;
        }
    }

    final ValueSet valueSet = new ValueSet();

    protected SchemaReaderV3(String xmlns, XMLStreamReader reader, Map<String, Object> properties) {
        super(xmlns, reader, properties);
    }

    public SchemaReaderV3(XMLStreamReader reader, Map<String, Object> properties) {
        this(StaEDISchemaFactory.XMLNS_V3, reader, properties);
    }

    @Override
    protected String readReferencedId(XMLStreamReader reader) {
        String id = reader.getAttributeValue(null, "type");
        if (id == null) {
            id = reader.getAttributeValue(null, "ref");

            if (id != null) {
                Location parseLocation = reader.getLocation();
                LOGGER.warning("Attribute 'ref' is deprecated at line "
                        + parseLocation.getLineNumber()
                        + ", column " + parseLocation.getColumnNumber());
            }
        }
        return id;
    }

    @Override
    protected void readInclude(XMLStreamReader reader, Map<String, EDIType> types) throws EDISchemaException {
        // Included schema not supported in V3 Schema
        throw unexpectedElement(reader.getName(), reader);
    }

    @Override
    protected void readImplementation(XMLStreamReader reader, Map<String, EDIType> types) {
        QName element = reader.getName();

        if (qnImplementation.equals(element)) {
            LoopImplementation impl = readImplementation(reader, element, types);

            if (impl != null) {
                types.put(StaEDISchema.IMPLEMENTATION_ID, impl);
            }

            nextTag(reader, "seeking next element after implementation end");
        }
    }

    @Override
    void setReferences(Map<String, EDIType> types) {
        super.setReferences(types);

        StreamSupport.stream(Spliterators.spliteratorUnknownSize(implementedTypes.descendingIterator(),
                                                                 Spliterator.ORDERED),
                             false)
                     .filter(type -> type.getType() != Type.ELEMENT)
                     .map(BaseComplexImpl.class::cast)
                     .forEach(type -> setReferences(type, types));
    }

    void setReferences(BaseComplexImpl type, Map<String, EDIType> types) {
        String typeId = type.getTypeId();
        EDIComplexType standard = (EDIComplexType) types.get(typeId);
        if (standard == null) {
            throw schemaException("Type " + typeId + " does not correspond to a standard type");
        }
        List<EDIReference> standardRefs = standard.getReferences();
        List<EDITypeImplementation> implSequence = type.getSequence();

        if (implSequence.isEmpty()) {
            implSequence.addAll(getDefaultSequence(standardRefs));
            return;
        }

        for (EDITypeImplementation t : implSequence) {
            if (t == null) {
                continue;
            }
            EDIReference stdRef;
            BaseImpl<?> seqImpl = (BaseImpl<?>) t;

            if (t instanceof Positioned) {
                Positioned p = (Positioned) t;
                int offset = p.getPosition() - 1;
                if (standardRefs != null && offset > -1 && offset < standardRefs.size()) {
                    stdRef = standardRefs.get(offset);
                } else {
                    throw schemaException("Position " + p.getPosition()
                            + " does not correspond to an entry in type " + standard.getId());
                }
            } else {
                String refTypeId = seqImpl.getTypeId();

                stdRef = standardRefs.stream().filter(r -> r.getReferencedType()
                                                            .getId()
                                                            .equals(refTypeId))
                                     .findFirst()
                                     .orElseThrow(() -> schemaException("Reference " + refTypeId
                                             + " does not correspond to an entry in type "
                                             + standard.getId()));
            }

            seqImpl.setStandardReference(stdRef);
        }
    }

    List<EDITypeImplementation> getDefaultSequence(List<EDIReference> standardRefs) {
        List<EDITypeImplementation> sequence = new ArrayList<>(standardRefs.size());
        int position = 0;

        for (EDIReference ref : standardRefs) {
            sequence.add(getDefaultImplementation(ref, ++position));
        }

        return sequence;
    }

    EDITypeImplementation getDefaultImplementation(EDIReference standardReference, int position) {
        EDIType std = standardReference.getReferencedType();

        switch (std.getType()) {
        case ELEMENT:
            return new ElementImpl(standardReference, position);
        case COMPOSITE:
            return new CompositeImpl(standardReference, position, getDefaultSequence(((EDIComplexType) std).getReferences()));
        default:
            throw schemaException("Implementation of " + std.getId() + " must not be empty");
        }
    }

    LoopImplementation readImplementation(XMLStreamReader reader,
                                          QName complexType,
                                          Map<String, EDIType> types) {

        LoopImplementation loop = readLoopImplementation(reader, complexType, true);
        String typeId = StaEDISchema.TRANSACTION_ID;
        EDIComplexType standard = (EDIComplexType) types.get(typeId);
        LoopImpl impl = new TransactionImpl(StaEDISchema.IMPLEMENTATION_ID, typeId, loop.getSequence());
        impl.setStandardReference(new Reference(standard, 1, 1));
        implementedTypes.add(impl);
        return impl;
    }

    LoopImplementation readLoopImplementation(XMLStreamReader reader, QName complexType, boolean transactionLoop) {
        List<EDITypeImplementation> sequence = new ArrayList<>();
        String id;
        String typeId;
        int minOccurs = 0;
        int maxOccurs = 0;
        BigDecimal discriminatorAttr = null;
        String title = null;

        if (transactionLoop) {
            id = StaEDISchema.IMPLEMENTATION_ID;
            typeId = null;
        } else {
            id = parseAttribute(reader, "code", String::valueOf);
            typeId = parseAttribute(reader, "type", String::valueOf);
            minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, -1);
            maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, -1);
            discriminatorAttr = parseAttribute(reader, ATTR_DISCRIMINATOR, BigDecimal::new, null);
            title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);
        }

        String descr = readDescription(reader);
        readSequence(reader, e -> readLoopSequenceEntry(e, sequence));
        nextTag(reader, "reading to end of " + complexType);
        requireElement(complexType, reader);

        Discriminator disc = null;

        if (discriminatorAttr != null) {
            SegmentImpl segImpl = (SegmentImpl) sequence.get(0);
            disc = buildDiscriminator(discriminatorAttr, segImpl.getSequence());
        }

        return new LoopImpl(minOccurs, maxOccurs, id, typeId, disc, sequence, title, descr);
    }

    void readLoopSequenceEntry(QName entryName, List<EDITypeImplementation> sequence) {
        if (entryName.equals(qnLoop)) {
            if (sequence.isEmpty()) {
                throw schemaException("segment element must be first child of loop sequence", reader);
            }
            LoopImplementation loop = readLoopImplementation(reader, entryName, false);
            implementedTypes.add(loop);
            sequence.add(loop);
        } else if (entryName.equals(qnSegment)) {
            sequence.add(readSegmentImplementation());
        } else {
            throw unexpectedElement(entryName, reader);
        }
    }

    SegmentImpl readSegmentImplementation() {
        List<EDITypeImplementation> sequence = new ArrayList<>();
        String typeId = parseAttribute(reader, "type", String::valueOf);
        String code = parseAttribute(reader, "code", String::valueOf, typeId);
        int minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, -1);
        int maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, -1);
        BigDecimal discriminatorAttr = parseAttribute(reader, ATTR_DISCRIMINATOR, BigDecimal::new, null);
        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);

        return readTypeImplementation(reader,
                                      () -> readSequence(reader, e -> readPositionedSequenceEntry(e, sequence, true)),
                                      descr -> whenExpected(reader, qnSegment, () -> {
                                          Discriminator disc = buildDiscriminator(discriminatorAttr, sequence);
                                          SegmentImpl segment = new SegmentImpl(minOccurs,
                                                                                maxOccurs,
                                                                                typeId,
                                                                                code,
                                                                                disc,
                                                                                sequence,
                                                                                title,
                                                                                descr);
                                          implementedTypes.add(segment);
                                          return segment;
                                      }));
    }

    void readPositionedSequenceEntry(QName entryName, List<EDITypeImplementation> sequence, boolean composites) {
        EDITypeImplementation type;

        if (entryName.equals(qnElement)) {
            type = readElementImplementation(reader);
        } else if (composites && entryName.equals(qnComposite)) {
            type = readCompositeImplementation(reader);
        } else {
            throw unexpectedElement(entryName, reader);
        }

        implementedTypes.add(type);

        int position = ((Positioned) type).getPosition();

        while (position > sequence.size()) {
            sequence.add(null);
        }

        EDITypeImplementation previous = sequence.set(position - 1, type);

        if (previous != null) {
            throw schemaException("Duplicate value for position " + position, reader);
        }
    }

    Discriminator buildDiscriminator(BigDecimal discriminatorAttr,
                                     List<EDITypeImplementation> sequence) {
        Discriminator disc = null;

        if (discriminatorAttr != null) {
            int elementPosition = discriminatorAttr.intValue();
            int componentPosition = discriminatorAttr.remainder(BigDecimal.ONE)
                                                     .movePointRight(discriminatorAttr.scale()).intValue();

            EDITypeImplementation eleImpl = getDiscriminatorElement(discriminatorAttr, elementPosition, sequence, "element");

            if (eleImpl instanceof CompositeImpl) {
                sequence = ((CompositeImpl) eleImpl).getSequence();
                eleImpl = getDiscriminatorElement(discriminatorAttr, componentPosition, sequence, "component");
            }

            Set<String> discValues;

            if (eleImpl != null) {
                discValues = ((ElementImpl) eleImpl).getValueSet();
            } else {
                throw schemaException("Discriminator position is unused (not specified): " + discriminatorAttr, reader);
            }

            if (!discValues.isEmpty()) {
                disc = new DiscriminatorImpl(elementPosition, componentPosition, discValues);
            } else {
                throw schemaException("Discriminator element does not specify value enumeration: " + discriminatorAttr, reader);
            }
        }

        return disc;
    }

    EDITypeImplementation getDiscriminatorElement(BigDecimal attr, int position, List<EDITypeImplementation> sequence, String type) {
        if (position > 0 && position <= sequence.size()) {
            return sequence.get(position - 1);
        } else {
            throw schemaException("Discriminator " + type + " position invalid: " + attr, reader);
        }
    }

    CompositeImpl readCompositeImplementation(XMLStreamReader reader) {
        List<EDITypeImplementation> sequence = new ArrayList<>(5);
        int position = parseAttribute(reader, ATTR_POSITION, Integer::parseInt, 0);
        int minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, -1);
        int maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, -1);
        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);

        return readTypeImplementation(reader,
                                      () -> readSequence(reader, e -> readPositionedSequenceEntry(e, sequence, false)),
                                      descr -> whenExpected(reader,
                                                            qnComposite,
                                                            () -> new CompositeImpl(minOccurs,
                                                                                    maxOccurs,
                                                                                    null,
                                                                                    position,
                                                                                    sequence,
                                                                                    title,
                                                                                    descr)));
    }

    void readSequence(XMLStreamReader reader, Consumer<QName> startHandler) {
        requireElementStart(qnSequence, reader);

        do {
            if (nextTag(reader, "reading sequence") == XMLStreamConstants.START_ELEMENT) {
                startHandler.accept(reader.getName());
            }
        } while (!reader.getName().equals(qnSequence));
    }

    ElementImpl readElementImplementation(XMLStreamReader reader) {
        this.valueSet.clear();
        int position = parseAttribute(reader, ATTR_POSITION, Integer::parseInt, 0);
        int minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, -1);
        int maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, -1);
        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);

        return readTypeImplementation(reader,
                                      () -> valueSet.set(super.readEnumerationValues(reader)),
                                      descr -> whenExpected(reader,
                                                            qnElement,
                                                            () -> new ElementImpl(minOccurs,
                                                                                  maxOccurs,
                                                                                  (String) null,
                                                                                  position,
                                                                                  valueSet.get(),
                                                                                  title,
                                                                                  descr)));
    }

    <T> T whenExpected(XMLStreamReader reader, QName expected, Supplier<T> supplier) {
        requireElement(expected, reader);
        return supplier.get();
    }

    <T> T readTypeImplementation(XMLStreamReader reader, Runnable contentHandler, Function<String, T> endHandler) {

        String descr = readDescription(reader);

        if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            contentHandler.run();
        } else {
            return endHandler.apply(descr);
        }

        nextTag(reader, "reading type implementation end element");
        requireEvent(XMLStreamConstants.END_ELEMENT, reader);
        return endHandler.apply(descr);
    }
}
