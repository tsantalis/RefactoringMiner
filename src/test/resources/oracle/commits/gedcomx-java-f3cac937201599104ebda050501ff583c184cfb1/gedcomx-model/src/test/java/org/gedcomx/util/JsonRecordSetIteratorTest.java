/**
 * Copyright Intellectual Reserve, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gedcomx.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.gedcomx.Gedcomx;
import org.gedcomx.records.RecordSet;
import org.gedcomx.rt.json.GedcomJacksonModule;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * User: Brent Hale
 * Date: 6/4/2015
 * <p/>
 */
public class JsonRecordSetIteratorTest extends TestCase {

  public void testReadingWritingJsonRecordSetFromFile() throws JAXBException, IOException {
    URL url = getClass().getClassLoader().getResource("gedcomx-recordset.json");
    assertNotNull(url);
    JsonRecordSetIterator jsonRecordSetIterator = new JsonRecordSetIterator(url.getFile());

    RecordSet recordSet = new RecordSet();
    recordSet.setRecords(new ArrayList<Gedcomx>());

    // Read in the records one at a time.
    while (jsonRecordSetIterator.hasNext()) {
      recordSet.getRecords().add(jsonRecordSetIterator.next());
    }

    jsonRecordSetIterator.close();

    assertEquals(recordSet.getRecords().size(), 3);
  }
  public void testReadingWritingJsonRecordSet() throws JAXBException, IOException {
    RecordSet recordSet1 = new RecordSet();

    recordSet1.setMetadata(TestRecordSetWriter.getMetadataFromFile());
    List<Gedcomx> records = getRecordsFromRecordSetFile();
    recordSet1.setRecords(records);
    recordSet1.setId("r1");

    for (boolean isGzipped : new boolean[]{false, true}) {
      // Write the RecordSet to a String as Json
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      OutputStream outputStream = isGzipped ? new GZIPOutputStream(bos) : bos;
      ObjectMapper objectMapper = GedcomJacksonModule.createObjectMapper();
      objectMapper.writeValue(outputStream, recordSet1);

      // Read the Json String back into a RecordSet using a JsonRecordSetIterator.
      RecordSet recordSet2 = new RecordSet();
      recordSet2.setRecords(new ArrayList<Gedcomx>());
      byte[] bytes = bos.toByteArray();
      InputStream inputStream = new ByteArrayInputStream(bytes);

      JsonRecordSetIterator jsonRecordSetIterator = new JsonRecordSetIterator(inputStream, isGzipped);

      // Read in the records one at a time.
      while (jsonRecordSetIterator.hasNext()) {
        recordSet2.getRecords().add(jsonRecordSetIterator.next());
      }

      // Read in the Metadata
      Gedcomx metadata = jsonRecordSetIterator.getMetadata();
      recordSet2.setId(jsonRecordSetIterator.getId());

      assertNotNull(metadata);

      assertEquals(recordSet1.getId(), recordSet2.getId());
      assertEquals(recordSet1.getRecords().size(), recordSet2.getRecords().size());

      jsonRecordSetIterator.close();
    }
  }

  public void testRemoveIsUnsupported() throws JAXBException, IOException {
    RecordSet recordSet1 = new RecordSet();

    recordSet1.setMetadata(TestRecordSetWriter.getMetadataFromFile());
    List<Gedcomx> records = getRecordsFromRecordSetFile();
    recordSet1.setRecords(records);

    // Write the RecordSet to a String as Json
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectMapper objectMapper = GedcomJacksonModule.createObjectMapper();
    objectMapper.writeValue(bos, recordSet1);

    // Read the Json String back into a RecordSet using a JsonRecordSetIterator.
    RecordSet recordSet2 = new RecordSet();
    recordSet2.setRecords(new ArrayList<Gedcomx>());
    byte[] bytes = bos.toByteArray();
    InputStream inputStream = new ByteArrayInputStream(bytes);

    JsonRecordSetIterator jsonRecordSetIterator = new JsonRecordSetIterator(inputStream, false);

    try {
      jsonRecordSetIterator.remove();
      assertTrue(false);    // Shouldn't get here.
    } catch (UnsupportedOperationException e) {
      // expected the exception.
    }

    jsonRecordSetIterator.close();
  }

  public static List<Gedcomx> getRecordsFromRecordSetFile() throws IOException {
    // currently my input has to be xml until I can create a Json version.
    InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("gedcomx-recordset.xml");

    int numRecords = 0;
    RecordSetIterator recordSetIterator = new XmlRecordSetIterator(inputStream, false);    // This is XML specific so far.

    Gedcomx record;
    List<Gedcomx> records = new ArrayList<Gedcomx>();

    String[] expectedRecordIds = new String[]{"r_14946444", "r_21837581269", "r_731503667"};
    // Read a record from the xml input file.
    while ((record = recordSetIterator.next()) != null) {
      assertEquals(expectedRecordIds[numRecords++], record.getId());

      records.add(record);
    }
    inputStream.close();

    return records;
  }
}