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

import junit.framework.TestCase;
import org.gedcomx.Gedcomx;
import org.gedcomx.conclusion.Name;
import org.gedcomx.conclusion.NameForm;
import org.gedcomx.conclusion.NamePart;
import org.gedcomx.conclusion.Person;
import org.gedcomx.records.RecordSet;
import org.gedcomx.types.NamePartType;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Class for testing the RecordSetWriter and RecordSetIterator class.
 *
 * User: Randy Wilson
 * Date: 12/4/13
 * Time: 3:39 PM
 */
public class TestRecordSetWriter extends TestCase {

  public void testRecordSetWriter() throws IOException, JAXBException {
    for (int metadataPos = -1; metadataPos <= 1; metadataPos++) {
      for (boolean isGzipped : new boolean[]{false, true}) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("gedcomx-recordset.xml");
        int numRecords = 0;
        RecordSetIterator recordIterator = new XmlRecordSetIterator(inputStream, false);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream outputStream = isGzipped ? new GZIPOutputStream(bos) : bos;
        RecordSetWriter writer = new RecordSetWriter(outputStream);

        Gedcomx metadata = testWriteMetadata(metadataPos, writer);

        Gedcomx record;
        List<String> recordIds = new ArrayList<String>();
        List<Gedcomx> records = new ArrayList<Gedcomx>();

        String[] expectedRecordIds = new String[]{"r_14946444", "r_21837581269", "r_731503667"};
        boolean isFirst = true;
        // Read a record from the xml input file.
        while ((record = recordIterator.next()) != null) {
          assertEquals(expectedRecordIds[numRecords++], record.getId());

          writer.writeRecord(record);

          records.add(record);
          recordIds.add(record.getId());

          if (metadataPos == 1 && isFirst) {
            // try writing metadata in the middle of the records, to make sure it ends up at the end like it should.
            writer.setMetadata(metadata);
            isFirst = false;
          }
        }
        inputStream.close();

        if (metadataPos == 2) {
          writer.setMetadata(metadata);
        }

        writer.close();
        assertEquals(3, records.size());

        byte[] bytes = bos.toByteArray();
        recordIterator = new XmlRecordSetIterator(new ByteArrayInputStream(bytes), isGzipped);
        for (int i = 0; i < numRecords; i++) {
          record = recordIterator.next();
          assertNotNull(record);
          assertEquals(recordIds.get(i), record.getId());
        }
        Gedcomx metadata2 = recordIterator.getMetadata();
        if (metadataPos != 0) {
          assertEquals(metadata2.getSourceDescription().getTitle().getValue(), metadata.getSourceDescription().getTitle().getValue());
        }
        else {
          assertNull(metadata2);
        }
        assertNull(recordIterator.next());
      }
    }
  }

  private Gedcomx testWriteMetadata(int metadataPos, RecordSetWriter recordSetWriter) throws JAXBException, IOException {
    Gedcomx metadata = null;
    if (metadataPos != 0) {
      metadata = getMetadataFromFile();
      if (metadataPos == -1) {
        // Write metadata at the beginning of the stream.
        recordSetWriter.setMetadata(metadata);
        // Make sure we can't write it twice
        try {
          recordSetWriter.setMetadata(metadata);
          fail("Should have thrown an exception when trying to write metadata twice.");
        }
        catch (IllegalStateException e) {
          // ok
        }
      }
    }
    return metadata;
  }

  public static Gedcomx getMetadataFromFile() throws JAXBException, IOException {
    Gedcomx metadata;
    InputStream metadataInputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("gedcomx-collection.xml");
    metadata = (Gedcomx) MarshalUtil.createUnmarshaller().unmarshal(metadataInputStream);
    metadataInputStream.close();
    return metadata;
  }

  public void testCjk() throws Exception {
    final String surname = "岩\uD842\uDFB7";//"\uD850\uDDAC成功";
    final String givenName = "岩\uD842\uDFB7";
    assertEquals(givenName, CleanXMLStreamWriter.escapeCharacters(givenName));
    final String fullName = surname + givenName;
    final Name name = new Name().nameForm(new NameForm(fullName,
                                                       new NamePart(NamePartType.Surname, surname),
                                                       new NamePart(NamePartType.Given, givenName)));
    final Gedcomx doc = new Gedcomx().person(new Person().name(name));
    for (int i = 0; i < 2; i++) {
      name.addNameForm(new NameForm(fullName, new NamePart(NamePartType.Surname, surname), new NamePart(NamePartType.Given, givenName)).id(i + "-" + surname));
    }

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    RecordSetWriter writer = new RecordSetWriter(bos, false, Gedcomx.class, RecordSet.class);
    writer.writeRecord(doc);
    writer.close();
    byte[] bytes = bos.toByteArray();

    XmlRecordSetIterator iterator = new XmlRecordSetIterator(new ByteArrayInputStream(bytes));
    Gedcomx result = iterator.next();
    iterator.close();
    NameForm nameForm = result.getPerson().getName().getNameForm();
    if (!nameForm.getFullText().equals(fullName) || !nameForm.getParts().get(0).getValue().equals(surname) ||
            !nameForm.getParts().get(1).getValue().equals(givenName)) {
      System.out.println("Error! From " + n(name.getNameForm()) + " => " + n(nameForm));
      System.out.println("Orig:========\n" + MarshalUtil.toXml(doc));
      String rs = new String(bytes, "UTF-8");
      System.out.println("RecordSet:===\n" + rs);
      RecordSet recordSet = new RecordSet();
      recordSet.setRecords(Collections.singletonList(doc));
      System.out.println("RS2:=========\n" + MarshalUtil.toXml(recordSet));
      System.out.println("Then:========\n" + MarshalUtil.toXml(result));
    }
    assertEquals(fullName, nameForm.getFullText());
    assertEquals(surname, nameForm.getParts().get(0).getValue());
    assertEquals(givenName, nameForm.getParts().get(1).getValue());
  }

  private String n(NameForm name) {
    return name.getFullText() + " (/" + name.getParts().get(0).getValue() + "/" + name.getParts().get(1).getValue() + ")";
  }
}
