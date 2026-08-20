package io.xlate.edi.internal.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

@SuppressWarnings("unchecked")
class StaEDIStreamReaderSyntaxTest {

    Object[] readInterchange(String data, String schemaPath) throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream(data.getBytes());
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        List<Location> errorLocations = new ArrayList<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_TRANSACTION:
                SchemaFactory schemaFactory = SchemaFactory.newFactory();
                Schema schema = schemaFactory.createSchema(getClass().getResource(schemaPath));
                reader.setTransactionSchema(schema);
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                errorLocations.add(reader.getLocation().copy());
                break;
            default:
                break;
            }
        }

        return new Object[] { errors, errorLocations };
    }

    @Test
    void testFirstSyntaxValidation_NonePresent() throws Exception {
        Object[] result = readInterchange(""
                + "UNB+UNOA:3+SENDER+RECEIVER+200906:1148+1'"
                + "UNH+1+INVOIC:D:93A:UN'"
                + "UXA+++'"
                + "UNT+3+1'"
                + "UNZ+1+1'", "/EDIFACT/fragment-first-syntax-validation.xml");

        List<EDIStreamValidationError> errors = (List<EDIStreamValidationError>) result[0];
        assertEquals(0, errors.size(), () -> "Unexpected errors: " + errors);
    }

    @Test
    void testFirstSyntaxValidation_FirstOnlyPresent() throws Exception {
        Object[] result = readInterchange(""
                + "UNB+UNOA:3+SENDER+RECEIVER+200906:1148+1'"
                + "UNH+1+INVOIC:D:93A:UN'"
                + "UXA+FIRST++'"
                + "UNT+3+1'"
                + "UNZ+1+1'", "/EDIFACT/fragment-first-syntax-validation.xml");

        List<EDIStreamValidationError> errors = (List<EDIStreamValidationError>) result[0];
        assertEquals(0, errors.size(), () -> "Unexpected errors: " + errors);
    }

    @Test
    void testFirstSyntaxValidation_FirstAndSecondPresent() throws Exception {
        Object[] result = readInterchange(""
                + "UNB+UNOA:3+SENDER+RECEIVER+200906:1148+1'"
                + "UNH+1+INVOIC:D:93A:UN'"
                + "UXA+FIRST+SECOND+'"
                + "UNT+3+1'"
                + "UNZ+1+1'", "/EDIFACT/fragment-first-syntax-validation.xml");

        List<EDIStreamValidationError> errors = (List<EDIStreamValidationError>) result[0];
        assertEquals(0, errors.size(), () -> "Unexpected errors: " + errors);
    }

    @Test
    void testFirstSyntaxValidation_SecondAndThirdPresent() throws Exception {
        Object[] result = readInterchange(""
                + "UNB+UNOA:3+SENDER+RECEIVER+200906:1148+1'"
                + "UNH+1+INVOIC:D:93A:UN'"
                + "UXA++SECOND+THIRD'"
                + "UNT+3+1'"
                + "UNZ+1+1'", "/EDIFACT/fragment-first-syntax-validation.xml");

        List<EDIStreamValidationError> errors = (List<EDIStreamValidationError>) result[0];
        assertEquals(0, errors.size(), () -> "Unexpected errors: " + errors);
    }

    @Test
    void testFirstSyntaxValidation_FirstAndThirdPresent() throws Exception {
        Object[] result = readInterchange(""
                + "UNB+UNOA:3+SENDER+RECEIVER+200906:1148+1'"
                + "UNH+1+INVOIC:D:93A:UN'"
                + "UXA+FIRST++THIRD'"
                + "UNT+3+1'"
                + "UNZ+1+1'", "/EDIFACT/fragment-first-syntax-validation.xml");

        List<EDIStreamValidationError> errors = (List<EDIStreamValidationError>) result[0];
        List<Location> errorLocations = (List<Location>) result[1];
        assertEquals(1, errors.size(), () -> "Unexpected errors: " + errors);
        assertEquals(EDIStreamValidationError.EXCLUSION_CONDITION_VIOLATED, errors.get(0));
        assertEquals("UXA", errorLocations.get(0).getSegmentTag());
        assertEquals(3, errorLocations.get(0).getElementPosition());
    }
}
