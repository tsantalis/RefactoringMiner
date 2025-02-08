package org.zanata.rest.dto.resource;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.zanata.common.LocaleId;
import org.zanata.rest.dto.DTOUtil;

import static org.junit.Assert.assertEquals;

public class TextFlowTest {
    ObjectMapper om = new ObjectMapper();
    LocaleId esES = new LocaleId("es-ES");

    @Test
    public void testReadJsonPlural() throws JsonParseException,
            JsonMappingException, IOException {
        String json =
                "{\n" + "    \"id\" : \"_id\",\n" + "    \"revision\" : 17,\n"
                        + "    \"lang\" : \"es-ES\",\n"
                        + "    \"contents\" : [\"plural1\", \"plural2\"]\n"
                        + "}";
        TextFlow tf = om.readValue(json, TextFlow.class);

        TextFlow expected = new TextFlow("_id", esES, "plural1", "plural2");
        expected.setRevision(17);
        assertEquals(tf, expected);
    }

    @Test
    public void testReadJsonSingular() throws JsonParseException,
            JsonMappingException, IOException {
        String json =
                "{\n" + "    \"id\" : \"_id\",\n" + "    \"revision\" : 17,\n"
                        + "    \"lang\" : \"es-ES\",\n"
                        + "    \"content\" : \"single\"\n" + "}";
        TextFlow tf = om.readValue(json, TextFlow.class);

        TextFlow expected = new TextFlow("_id", esES, "single");
        expected.setRevision(17);
        assertEquals(tf, expected);
    }

    @Test
    public void testReadXmlPlural() throws JAXBException {
        String xml =
                "<TextFlow revision=\"17\" xml:lang=\"es-ES\" id=\"_id\" xmlns:ns2=\"http://zanata.org/namespace/api/gettext/\">\n"
                        + "    <contents>\n"
                        + "        <content>abc</content>\n"
                        + "        <content>def</content>\n"
                        + "    </contents>\n" + "</TextFlow>";

        TextFlow tf = DTOUtil.toObject(xml, TextFlow.class);

        TextFlow expected = new TextFlow("_id", esES, "abc", "def");
        expected.setRevision(17);
        assertEquals(tf, expected);
    }

    @Test
    public void testReadXmlSingular() throws JAXBException {
        String xml =
                "<TextFlow revision=\"17\" xml:lang=\"es-ES\" id=\"_id\" xmlns:ns2=\"http://zanata.org/namespace/api/gettext/\">\n"
                        + "    <content>abc</content>\n" + "</TextFlow>";

        TextFlow tf = DTOUtil.toObject(xml, TextFlow.class);

        TextFlow expected = new TextFlow("_id", esES, "abc");
        expected.setRevision(17);
        assertEquals(tf, expected);
    }

    @Test
    public void testWriteSingular() throws JsonGenerationException,
            JsonMappingException, IOException {
        TextFlow tf = new TextFlow();
        tf.setContents("abc");

        String expectedXML =
                "<TextFlow xmlns:ns2=\"http://zanata.org/namespace/api/gettext/\">\n"
                        + "    <content>abc</content>\n"
                        + "    <plural>false</plural>\n" + "</TextFlow>";
        assertEquals(tf.toString(), expectedXML);
        String expectedJSON = "{\"content\":\"abc\",\"plural\":false}";
        assertEquals(om.writeValueAsString(tf), expectedJSON);
    }

    @Test
    public void testWritePlural() throws JsonGenerationException,
            JsonMappingException, IOException {
        TextFlow tf = new TextFlow();
        tf.setContents("abc", "def");
        tf.setPlural(true);

        String expectedXML =
                "<TextFlow xmlns:ns2=\"http://zanata.org/namespace/api/gettext/\">\n"
                        + "    <contents>\n"
                        + "        <content>abc</content>\n"
                        + "        <content>def</content>\n"
                        + "    </contents>\n" + "    <plural>true</plural>\n"
                        + "</TextFlow>";

        assertEquals(tf.toString(), expectedXML);

        String expectedJSON =
                "{\"content\":\"\",\"contents\":[\"abc\",\"def\"],\"plural\":true}";
        assertEquals(om.writeValueAsString(tf), expectedJSON);
    }

}
