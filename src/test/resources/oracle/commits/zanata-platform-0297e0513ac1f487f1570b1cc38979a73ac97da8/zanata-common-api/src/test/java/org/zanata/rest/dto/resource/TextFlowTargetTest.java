package org.zanata.rest.dto.resource;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.zanata.rest.dto.DTOUtil;

import static org.junit.Assert.assertEquals;

public class TextFlowTargetTest {
    ObjectMapper om = new ObjectMapper();

    @Test
    public void testReadJsonPlural() throws JsonParseException,
            JsonMappingException, IOException {
        String json =
                "{\n" + "    \"id\" : \"_id\",\n"
                        + "    \"resId\" : \"_resid\",\n"
                        + "    \"revision\" : 17,\n"
                        + "    \"lang\" : \"es-ES\",\n"
                        + "    \"contents\" : [\"plural1\", \"plural2\"]\n"
                        + "}";
        TextFlowTarget tft = om.readValue(json, TextFlowTarget.class);

        TextFlowTarget expected = new TextFlowTarget("_id");
        expected.setResId("_resid");
        expected.setContents("plural1", "plural2");
        expected.setRevision(17);
        assertEquals(tft, expected);
    }

    @Test
    public void testReadJsonSingular() throws JsonParseException,
            JsonMappingException, IOException {
        String json =
                "{\n" + "    \"id\" : \"_id\",\n"
                        + "    \"resId\" : \"_resid\",\n"
                        + "    \"revision\" : 17,\n"
                        + "    \"lang\" : \"es-ES\",\n"
                        + "    \"content\" : \"single\"\n" + "}";
        TextFlowTarget tft = om.readValue(json, TextFlowTarget.class);

        TextFlowTarget expected = new TextFlowTarget("_id");
        expected.setResId("_resid");
        expected.setContents("single");
        expected.setRevision(17);
        assertEquals(tft, expected);
    }

    @Test
    public void testReadXmlPlural() throws JAXBException {
        String xml =
                "<TextFlowTarget revision=\"17\" xml:lang=\"es-ES\" id=\"_id\" res-id=\"_resid\" xmlns:ns2=\"http://zanata.org/namespace/api/gettext/\">\n"
                        + "    <contents>\n"
                        + "        <content>abc</content>\n"
                        + "        <content>def</content>\n"
                        + "    </contents>\n" + "</TextFlowTarget>";

        TextFlowTarget tf = DTOUtil.toObject(xml, TextFlowTarget.class);

        TextFlowTarget expected = new TextFlowTarget("_id");
        expected.setResId("_resid");
        expected.setContents("abc", "def");
        expected.setRevision(17);
        assertEquals(tf, expected);
    }

    @Test
    public void testReadXmlSingular() throws JAXBException {
        String xml =
                "<TextFlowTarget revision=\"17\" xml:lang=\"es-ES\" id=\"_id\" res-id=\"_resid\" xmlns:ns2=\"http://zanata.org/namespace/api/gettext/\">\n"
                        + "    <content>abc</content>\n" + "</TextFlowTarget>";

        TextFlowTarget tft = DTOUtil.toObject(xml, TextFlowTarget.class);

        TextFlowTarget expected = new TextFlowTarget("_id");
        expected.setResId("_resid");
        expected.setContents("abc");
        expected.setRevision(17);
        assertEquals(tft, expected);
    }

    @Test
    public void testWritePlural() throws JsonGenerationException,
            JsonMappingException, IOException {
        TextFlowTarget tft = new TextFlowTarget();
        tft.setContents("abc", "def");

        String expectedXML =
                "<TextFlowTarget state=\"New\" xmlns:ns2=\"http://zanata.org/namespace/api/\">\n"
                        + "    <contents>\n"
                        + "        <content>abc</content>\n"
                        + "        <content>def</content>\n"
                        + "    </contents>\n" + "</TextFlowTarget>";

        assertEquals(tft.toString(), expectedXML);

        String expectedJSON =
                "{\"state\":\"New\",\"content\":\"\",\"contents\":[\"abc\",\"def\"]}";
        assertEquals(om.writeValueAsString(tft), expectedJSON);
    }

    @Test
    public void testWriteSingular() throws JsonGenerationException,
            JsonMappingException, IOException {
        TextFlowTarget tft = new TextFlowTarget();
        tft.setContents("abc");

        String expectedXML =
                "<TextFlowTarget state=\"New\" xmlns:ns2=\"http://zanata.org/namespace/api/\">\n"
                        + "    <content>abc</content>\n" + "</TextFlowTarget>";
        assertEquals(tft.toString(), expectedXML);
        String expectedJSON = "{\"state\":\"New\",\"content\":\"abc\"}";
        assertEquals(om.writeValueAsString(tft), expectedJSON);
    }

}
