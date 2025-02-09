package org.gedcomx.source;

import org.junit.Test;
import org.gedcomx.common.URI;
import org.gedcomx.rt.SerializationUtil;
import org.gedcomx.types.ResourceStatusType;

import javax.xml.bind.JAXBException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Class for testing the SourceDescription class.
 * User: Randy Wilson
 * Date: 11/25/2014
 * Time: 2:42 PM
 */
public class SourceDescriptionTest {

  @Test
  public void testXml() throws JAXBException, UnsupportedEncodingException {
    SourceDescription sd = new SourceDescription();
    sd.setAbout(new URI("https://company.com/resource/id"));
    sd.addKnownStatus(ResourceStatusType.Deprecated);
    sd.addStatus(new URI("http://company.com/custom/resource/status/Forged"));
    sd.setReplacedBy(new URI("http://company.com/updated/id"));
    sd.setReplaces(Arrays.asList(new URI("http://company.com/old/id"), new URI("http://company.com/old/id2")));
    sd.setVersion("1");

    sd = SerializationUtil.processThroughXml(sd);

    assertEquals("https://company.com/resource/id", sd.getAbout().toString());
    assertEquals(ResourceStatusType.Deprecated, ResourceStatusType.fromQNameURI(sd.getStatuses().get(0)));
    assertEquals("http://company.com/custom/resource/status/Forged", sd.getStatuses().get(1).toString());
    assertEquals("http://company.com/updated/id", sd.getReplacedBy().toString());
    assertEquals("http://company.com/old/id", sd.getReplaces().get(0).toString());
    assertEquals("http://company.com/old/id2", sd.getReplaces().get(1).toString());
    assertEquals("1", sd.getVersion());
    assertEquals("2", sd.version("2").getVersion());

  }

}
