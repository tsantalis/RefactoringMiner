/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings("resource")
class StaEDISchemaFactoryTest {

    @Test
    void testCreateSchemaByURL() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        URL schemaURL = getClass().getResource("/x12/EDISchema997.xml");
        Schema schema = factory.createSchema(schemaURL);
        assertEquals(StaEDISchema.TRANSACTION_ID, schema.getStandard().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    void testCreateSchemaByURL_NoSuchFile() throws MalformedURLException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        URL schemaURL = new URL("file:./src/test/resources/x12/missing.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(schemaURL));
        assertEquals("Unable to read URL stream", thrown.getOriginalMessage());
        assertTrue(thrown.getCause() instanceof FileNotFoundException);
    }

    @Test
    void testCreateSchemaByStream() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream schemaStream = getClass().getResourceAsStream("/x12/EDISchema997.xml");
        Schema schema = factory.createSchema(schemaStream);
        assertEquals(StaEDISchema.TRANSACTION_ID, schema.getStandard().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    void testCreateEdifactInterchangeSchema() throws EDISchemaException {
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, new String[] { "UNOA", "4", "", "", "02" });
        assertNotNull(schema, "schema was null");
        assertEquals(StaEDISchema.INTERCHANGE_ID, schema.getStandard().getId(), "Incorrect root id");
    }

    @Test
    void testIsPropertySupportedTrue() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory.isPropertySupported(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT));
    }

    @Test
    void testIsPropertySupportedFalse() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(!factory.isPropertySupported("FOO"), "FOO *is* supported");
    }

    @Test
    void testPropertySupported() throws MalformedURLException {
        SchemaFactory factory = SchemaFactory.newFactory();
        URL workDir = new File(System.getProperty("user.dir")).toURI().toURL();
        // Set to current working directory
        factory.setProperty(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT, new File("").toURI().toURL());
        assertEquals(workDir, factory.getProperty(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT));
    }

    @Test
    void testGetPropertyUnsupported() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.getProperty("FOO"));
    }

    @Test
    void testSetProperty() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.setProperty("BAR", "BAZ"));
    }

    @Test
    void testExceptionThrownWithoutSchema() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream("<noschema></noschema>".getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [noschema]", thrown.getOriginalMessage());
    }

    @Test
    void testExceptionThrownWithoutInterchangeAndTransactionV2() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2
                + "'><random/></schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{" + StaEDISchemaFactory.XMLNS_V2 + "}random]", thrown.getOriginalMessage());
    }

    @Test
    void testInterchangeRequiredAttributesV2() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream1 = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2
                + "'><interchange _header='ABC' trailer='XYZ' /></schema>").getBytes());
        EDISchemaException thrown1 = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream1));
        assertEquals("Missing required attribute: [header]", thrown1.getOriginalMessage());

        InputStream stream2 = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2
                + "'><interchange header='ABC' _trailer='XYZ' /></schema>").getBytes());
        EDISchemaException thrown2 = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream2));
        assertEquals("Missing required attribute: [trailer]", thrown2.getOriginalMessage());

    }

    @Test
    void testInvalidStartOfSchema() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2 + "'"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Exception checking start of schema XML", thrown.getOriginalMessage());
    }

    @Test
    void testUnexpectedElementBeforeSequenceV2() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2 + "'>"
                + "<interchange header='ABC' trailer='XYZ'>"
                + "<description><![CDATA[TEXT ALLOWED]]></description>"
                + "<unexpected></unexpected>"
                + "<sequence></sequence>"
                + "</interchange>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{" + StaEDISchemaFactory.XMLNS_V2 + "}unexpected]", thrown.getOriginalMessage());
    }

    @Test
    void testCreateSchemaByStreamV3() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream schemaStream = getClass().getResourceAsStream("/x12/IG-999.xml");
        Schema schema = factory.createSchema(schemaStream);
        assertEquals(StaEDISchema.TRANSACTION_ID, schema.getStandard().getId(), "Incorrect root id");
        assertEquals(StaEDISchema.IMPLEMENTATION_ID, schema.getImplementation().getId(), "Incorrect impl id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    void testCreateSchemaByStreamV4_with_include_equals_V3Schema() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream schemaStreamV4 = getClass().getResourceAsStream("/x12/IG-999-standard-included.xml");
        Schema schemaV4 = factory.createSchema(schemaStreamV4);
        assertEquals(StaEDISchema.TRANSACTION_ID, schemaV4.getStandard().getId(), "Incorrect root id");
        assertEquals(StaEDISchema.IMPLEMENTATION_ID, schemaV4.getImplementation().getId(), "Incorrect impl id");
        assertTrue(schemaV4.containsSegment("AK9"), "Missing AK9 segment");

        InputStream schemaStreamV3 = getClass().getResourceAsStream("/x12/IG-999.xml");
        Schema schemaV3 = factory.createSchema(schemaStreamV3);

        List<EDIType> missingV4 = StreamSupport.stream(schemaV3.spliterator(), false)
                                               .filter(type -> !(type.equals(schemaV4.getType(type.getId()))
                                                       && type.hashCode() == Objects.hashCode(schemaV4.getType(type.getId()))
                                                       && type.toString().equals(String.valueOf(schemaV4.getType(type.getId())))))
                                               .collect(Collectors.toList());
        List<EDIType> missingV3 = StreamSupport.stream(schemaV4.spliterator(), false)
                                               .filter(type -> !(type.equals(schemaV3.getType(type.getId()))
                                                       && type.hashCode() == Objects.hashCode(schemaV3.getType(type.getId()))
                                                       && type.toString().equals(String.valueOf(schemaV3.getType(type.getId())))))
                                               .collect(Collectors.toList());

        assertTrue(missingV4.isEmpty(), () -> "V3 schema contains types not in V4: " + missingV4);
        assertTrue(missingV3.isEmpty(), () -> "V4 schema contains types not in V3: " + missingV3);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testCreateSchemaByStreamV4_with_include_relative_equals_V3Schema() throws Exception {
        SchemaFactory factory = SchemaFactory.newFactory();

        // Reading URL as String
        factory.setProperty(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT, new File("src/test/resources/x12/").toURI().toString());
        Schema schema1 = factory.createSchema(getClass().getResourceAsStream("/x12/IG-999-standard-included-relative.xml"));

        // Reading URL
        factory.setProperty(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT, new File("src/test/resources/x12/").toURI().toURL());
        Schema schema2 = factory.createSchema(getClass().getResourceAsStream("/x12/IG-999-standard-included-relative.xml"));

        factory.setProperty(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT, null);
        Schema schema3 = factory.createSchema(getClass().getResourceAsStream("/x12/IG-999.xml"));

        assertNotEquals(schema1, factory);
        assertNotEquals(factory, schema1);

        assertEquals(schema3, schema1);
        assertEquals(schema1, schema2);

        assertEquals(schema1.hashCode(), schema2.hashCode());
        assertEquals(schema2.hashCode(), schema3.hashCode());

        assertEquals(schema3.getMainLoop(), schema1.getStandard());
    }

    @Test
    void testDuplicateImplPositionSpecified() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/implSchemaDuplicatePosition.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Duplicate value for position 1", cause.getMessage());
    }

    @Test
    void testDiscriminatorElementTooLarge() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-too-large.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator element position invalid: 3.1", cause.getMessage());
    }

    @Test
    void testDiscriminatorElementTooSmall() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-too-small.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator element position invalid: 0.1", cause.getMessage());
    }

    @Test
    void testDiscriminatorElementNotSpecified() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-not-specified.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator position is unused (not specified): 2.1", cause.getMessage());
    }

    @Test
    void testDiscriminatorComponentTooLarge() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/component-too-large.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator component position invalid: 2.3", cause.getMessage());
    }

    @Test
    void testDiscriminatorComponentTooSmall() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/component-too-small.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator component position invalid: 2.0", cause.getMessage());
    }

    @Test
    void testDiscriminatorMissingEnumeration() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-without-enumeration.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator element does not specify value enumeration: 2.2", cause.getMessage());
    }

    @Test
    void testInvalidSyntaxTypeValue() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/fragment-uci-invalid-syntax-type.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Invalid syntax 'type': [conditional-junk]", cause.getMessage());
    }

    @Test
    void testInvalidMinOccursValue() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/fragment-uci-invalid-min-occurs.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Invalid minOccurs", cause.getMessage());
    }

    @Test
    void testDuplicateElementTypeNames() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/fragment-uci-duplicate-element-names.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("duplicate name: DE0004", cause.getMessage());
    }

    @Test
    void testDuplicateElementTypeNames_v4_override() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V4 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <group header='SG3' trailer='SG4'>"
                + "    <transaction header='SG5' trailer='SG6'/>"
                + "  </group>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" minLength='2' maxLength=\"5\" />"
                + "<segmentType name=\"SG1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG3\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG4\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG5\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<elementType name=\"E1\" base=\"string\" minLength='1' maxLength=\"10\" />"
                + "<segmentType name=\"SG6\"><sequence><element type='E1'/></sequence></segmentType>"
                + "</schema>").getBytes());
        Schema schema = factory.createSchema(stream);
        Stream.of("SG1","SG2","SG3","SG4","SG5","SG6")
              .forEach(segmentTag -> {
                  assertEquals(1, ((EDISimpleType) ((EDIComplexType) schema.getType(segmentTag)).getReferences().get(0).getReferencedType()).getMinLength());
                  assertEquals(10, ((EDISimpleType) ((EDIComplexType) schema.getType(segmentTag)).getReferences().get(0).getReferencedType()).getMaxLength());
              });
    }

    @Test
    void testGetControlSchema() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        Schema schema = factory.getControlSchema(Standards.X12, new String[] { "00501" });
        assertNotNull(schema);
        assertEquals(EDIType.Type.SEGMENT, schema.getType("ISA").getType());
        assertEquals(EDIType.Type.SEGMENT, schema.getType("GS").getType());
        assertEquals(EDIType.Type.SEGMENT, schema.getType("ST").getType());
    }

    @Test
    void testGetControlSchema_NotFound() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        Schema schema = factory.getControlSchema(Standards.EDIFACT, new String[] { "UNOA", "0" });
        assertNull(schema);
    }

    @Test
    void testReferenceUndeclared() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='ABC' trailer='XYZ'>"
                + "<sequence>"
                + "  <segment type='NUL'/>"
                + "</sequence>"
                + "</interchange>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Type " + StaEDISchema.INTERCHANGE_ID + " references undeclared segment with ref='ABC'", thrown.getOriginalMessage());
    }

    @Test
    void testReferenceIncorrectType() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <segment type='E1'/>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<segmentType name=\"SG1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Type 'E1' must not be referenced as 'segment' in definition of type '" + StaEDISchema.INTERCHANGE_ID + "'",
                     thrown.getOriginalMessage());
    }

    @Test
    void testUnexpectedUnknownTypeElement() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <segment type='SG3'/>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<segmentType name=\"SG1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG3\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<unknownType xmlns='http://xlate.io'/>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{http://xlate.io}unknownType]",
                     thrown.getOriginalMessage());
    }

    @Test
    void testMissingRequiredTransactionElement() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <group header='SG3' trailer='SG4' use='prohibited'></group>"
                + "</sequence>"
                + "</interchange>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Expected XML element [{" + StaEDISchemaFactory.XMLNS_V3 + "}transaction] not found",
                     thrown.getOriginalMessage());
    }

    @Test
    void testProhibitedUseType() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <group header='SG3' trailer='SG4' use='prohibited'>"
                + "    <transaction header='SG5' trailer='SG6' use='prohibited'></transaction>"
                + "  </group>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<segmentType name=\"SG1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG3\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG4\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG5\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG6\"><sequence><element type='E1'/></sequence></segmentType>"
                + "</schema>").getBytes());
        Schema schema = factory.createSchema(stream);
        EDIComplexType interchange = schema.getStandard();
        assertEquals(0, interchange.getReferences().get(1).getMinOccurs());
        assertEquals(0, interchange.getReferences().get(1).getMaxOccurs());
    }

    @Test
    void testInvalidUseType() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <group header='SG3' trailer='SG4' use='junk'>"
                + "    <transaction header='SG5' trailer='SG6' use='prohibited'></transaction>"
                + "  </group>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<segmentType name=\"SG1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG3\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG4\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG5\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG6\"><sequence><element type='E1'/></sequence></segmentType>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Invalid value for 'use': junk",
                     thrown.getOriginalMessage());
    }

    @Test
    void testInvalidSegmentName() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='sg1' trailer='SG2'>"
                + "<sequence>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<segmentType name=\"sg1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Invalid segment name [sg1]",
                     thrown.getOriginalMessage());
    }

    @Test
    void testAnyCompositeType() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <group header='SG3' trailer='SG4'>"
                + "    <transaction header='SG5' trailer='SG6'></transaction>"
                + "  </group>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<segmentType name=\"SG1\"><sequence>"
                + "<element type='E1'/>"
                + "<any minOccurs='0' maxOccurs='2'/>"
                + "</sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG3\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG4\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG5\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG6\"><sequence><element type='E1'/></sequence></segmentType>"
                + "</schema>").getBytes());
        Schema schema = factory.createSchema(stream);
        EDIComplexType segmentSG1 = (EDIComplexType) schema.getType("SG1");
        assertEquals(3, segmentSG1.getReferences().size());
        // Two "ANY" references refer to the same object
        assertSame(segmentSG1.getReferences().get(1), segmentSG1.getReferences().get(2));
    }

    @Test
    void testAnyElementType() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<interchange header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <group header='SG3' trailer='SG4'>"
                + "    <transaction header='SG5' trailer='SG6'></transaction>"
                + "  </group>"
                + "</sequence>"
                + "</interchange>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<compositeType name=\"C001\"><sequence><any maxOccurs='5'/></sequence></compositeType>"
                + "<segmentType name=\"SG1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG3\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG4\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG5\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG6\"><sequence><composite type='C001'/></sequence></segmentType>"
                + "</schema>").getBytes());
        Schema schema = factory.createSchema(stream);
        EDIComplexType segmentSG6 = (EDIComplexType) schema.getType("SG6");
        assertEquals(1, segmentSG6.getReferences().size());
        // Two "ANY" references refer to the same object
        assertEquals("C001", segmentSG6.getReferences().get(0).getReferencedType().getId());
        assertEquals(5, ((EDIComplexType) segmentSG6.getReferences().get(0).getReferencedType()).getReferences().size());
    }

    @Test
    void testAnySegmentTypeInvalid() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "<transaction header='SG1' trailer='SG2'>"
                + "<sequence>"
                + "  <segment type='SG3'/>"
                + "  <any maxOccurs='2' />"
                + "</sequence>"
                + "</transaction>"
                + "<elementType name=\"E1\" base=\"string\" maxLength=\"5\" />"
                + "<segmentType name=\"SG1\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG2\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG3\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG4\"><sequence><element type='E1'/></sequence></segmentType>"
                + "<segmentType name=\"SG5\"><sequence><element type='E1'/></sequence></segmentType>"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Element {" + StaEDISchemaFactory.XMLNS_V3 + "}any may only be present for segmentType and compositeType",
                     thrown.getOriginalMessage());
    }

    @Test
    void testInvalidIncludeV2() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2 + "'>"
                + "  <include schemaLocation='./src/test/x12/EDISchema997.xml' />"
                + "</schema>").getBytes());

        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{" + StaEDISchemaFactory.XMLNS_V2 + "}include]",
                     thrown.getOriginalMessage());
    }

    @Test
    void testInvalidIncludeV3() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V3 + "'>"
                + "  <include schemaLocation='./src/test/x12/EDISchema997.xml' />"
                + "</schema>").getBytes());

        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{" + StaEDISchemaFactory.XMLNS_V3 + "}include]",
                     thrown.getOriginalMessage());
    }

    @Test
    void testValidIncludeV4() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V4 + "'>"
                + "  <include schemaLocation='file:./src/test/resources/x12/EDISchema997.xml' />"
                + "  <elementType name=\"DUMMY\" base=\"string\" maxLength=\"5\" />"
                + "</schema>").getBytes());

        Schema schema = factory.createSchema(stream);
        assertNotNull(schema);
    }

    @Test
    void testExceptionAtDocumentEnd() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V4 + "'>"
                + "  <include schemaLocation='file:./src/test/resources/x12/EDISchema997.xml' />"
                + "  <elementType name=\"DUMMY\" base=\"string\" maxLength=\"5\" />"
                + "</schema>"
                + "</schema>").getBytes());

        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("XMLStreamException reading end of document", thrown.getOriginalMessage());
        assertTrue(thrown.getCause() instanceof StaEDISchemaReadException);
        assertTrue(thrown.getCause().getCause() instanceof XMLStreamException);
    }

    @Test
    void testInvalidUrlIncludeV4() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V4 + "'>"
                + "  <include schemaLocation='./src/test/resources/x12/EDISchema997.xml' />"
                + "</schema>").getBytes());

        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Exception reading included schema", thrown.getOriginalMessage());
        assertTrue(thrown.getCause() instanceof StaEDISchemaReadException);
        assertTrue(thrown.getCause().getCause() instanceof MalformedURLException);
    }

    @Test
    void testIncludeV4_FileNotFound() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V4 + "'>\n"
                + "  <include schemaLocation='file:./src/test/resources/x12/missing.xml' />\n"
                + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Exception reading included schema", thrown.getOriginalMessage());
        assertEquals(2, thrown.getLocation().getLineNumber());
        assertEquals(73, thrown.getLocation().getColumnNumber());
        Throwable root = thrown;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertTrue(root instanceof IOException);
    }

    @Test
    void testControlNumbersHaveZeroScaleByDefault() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        Schema schema = factory.getControlSchema(Standards.X12, new String[] { "00501" });
        assertNotNull(schema);
        // ISA13 = N0
        assertEquals(Integer.valueOf(0), ((EDISimpleType) ((EDIComplexType) schema.getType("ISA")).getReferences().get(12).getReferencedType()).getScale());
        // GS06 = N0
        assertEquals(Integer.valueOf(0), ((EDISimpleType) ((EDIComplexType) schema.getType("GS")).getReferences().get(5).getReferencedType()).getScale());
    }
}
