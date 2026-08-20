// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.openstreetmap.josm.TestUtils;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Unit tests of {@link XmlUtils} class.
 */
class XmlUtilsTest {
    private static final String EXPECTED = "External Entity: Failed to read external document 'passwd', " +
            "because 'file' access is not allowed due to restriction set by the accessExternalDTD property.";

    @Test
    void testExternalEntitiesParsingDom() throws IOException, ParserConfigurationException {
        try {
            final String source = TestUtils.getTestDataRoot() + "dom_external_entity.xml";
            XmlUtils.parseSafeDOM(new FileInputStream(source));
            fail("Parsing a document with external entities should not be allowed.");
        } catch (SAXException e) {
            assertEquals("External Entity: Failed to read external document 'passwd', " +
                    "because 'file' access is not allowed due to restriction set by the accessExternalDTD property.", e.getMessage());
        }
    }

    @Test
    void testExternalEntitiesTransformer() throws IOException {
        try {
            final String source = TestUtils.getTestDataRoot() + "dom_external_entity.xml";
            final Transformer transformer = XmlUtils.newSafeTransformerFactory().newTransformer();
            transformer.transform(new StreamSource(new FileInputStream(source)), new StreamResult(new StringWriter()));
            fail("Parsing a document with external entities should not be allowed.");
        } catch (TransformerException e) {
            assertNotNull(e.getCause());
            assertEquals(EXPECTED, e.getCause().getMessage());
        }
    }

    @Test
    void testExternalEntitiesSaxParser() throws IOException, ParserConfigurationException {
        try {
            final String source = TestUtils.getTestDataRoot() + "dom_external_entity.xml";
            final DefaultHandler handler = new DefaultHandler();
            XmlUtils.parseSafeSAX(new InputSource(new FileInputStream(source)), handler);
            fail("Parsing a document with external entities should not be allowed.");
        } catch (SAXException e) {
            String expected = "DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true.";
            assertEquals(expected, e.getMessage());
        }
    }
}
