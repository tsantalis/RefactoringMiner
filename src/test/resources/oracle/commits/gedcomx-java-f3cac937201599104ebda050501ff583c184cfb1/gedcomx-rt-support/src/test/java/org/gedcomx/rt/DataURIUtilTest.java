package org.gedcomx.rt;

import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.AssertJUnit.assertEquals;

@SuppressWarnings ( "unchecked" )
@Test
public class DataURIUtilTest {

  public void testEncodeDecode() throws Exception {
    String value = "Birthplace may be Maryland:  http://www.smokykin.com/tng/getperson.php?personID=I4441&tree=Smokykin";
    assertEquals(value, DataURIUtil.getValueAsString(DataURIUtil.encodeDataURI(value)));

    String encoded = "data:,Birthplace%20may%20be%20Maryland:%20%20http://www.smokykin.com/tng/getperson.php?personID=I4441&tree=Smokykin";
    assertEquals(value, DataURIUtil.getValueAsString(URI.create(encoded)));

  }
}