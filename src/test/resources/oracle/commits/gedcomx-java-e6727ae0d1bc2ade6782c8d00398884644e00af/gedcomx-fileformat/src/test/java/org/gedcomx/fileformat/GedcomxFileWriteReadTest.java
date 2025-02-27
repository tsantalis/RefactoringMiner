/**
 * Copyright 2012 Intellectual Reserve, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gedcomx.fileformat;

import org.gedcomx.Gedcomx;
import org.gedcomx.rt.GedcomxConstants;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GedcomxFileWriteReadTest {

  @Test
  public void testWriteRead() throws Exception {
    File tempFile = File.createTempFile("FsTestTmp", ".gedx");
    try {
      Gedcomx bundle = ExampleGedcomxFileData.create();

      GedcomxOutputStream gedxOutputStream = new GedcomxOutputStream(new FileOutputStream(tempFile));
      final String CREATED_BY = "Created-By";
      final String createdByValue = "FamilySearch Platform API 0.1";
      try {
        gedxOutputStream.addAttribute(CREATED_BY, createdByValue);
        gedxOutputStream.addResource(bundle);
        gedxOutputStream.addResource("image/png", "person1.png", getClass().getResourceAsStream("/person1.png"), new Date(), null);
        gedxOutputStream.addResource("image/png", "person2.png", getClass().getResourceAsStream("/person2.png"), new Date(), null);
      }
      finally {
        gedxOutputStream.close();
      }

      JarFile jarFile = new JarFile(tempFile);
      try {
        GedcomxFile gedxFile = new GedcomxFile(jarFile);
        try {
          jarFile = null; // setting to null so the finally block will not attempt to close it as it was successfully wrapped in the gedxFile

          Map<String, String> attributes = gedxFile.getAttributes();
          for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String value = gedxFile.getAttribute(entry.getKey());
            assertEquals(value, entry.getValue());
          }
          assertTrue(attributes.containsKey(CREATED_BY));
          assertEquals(attributes.get(CREATED_BY), createdByValue);

          for (GedcomxFileEntry gedxEntry : gedxFile.getEntries()) {
            String name = gedxEntry.getJarEntry().getName();
            if ((name != null) && (!name.endsWith("png"))) {
              assertEquals(gedxEntry.getContentType(), GedcomxConstants.GEDCOMX_JSON_MEDIA_TYPE);
              Map<String, String> entryAttributes = gedxEntry.getAttributes();
              for (Map.Entry<String, String> entry : entryAttributes.entrySet()) {
                String value = gedxEntry.getAttribute(entry.getKey());
                assertEquals(value, entry.getValue());
              }
              assertTrue(entryAttributes.containsKey(Attributes.Name.CONTENT_TYPE.toString()));
              Object resource = gedxFile.readResource(gedxEntry);
              ExampleGedcomxFileData.assertContains((Gedcomx) resource, bundle);
            }
          }
        }
        finally {
          gedxFile.close();
        }
      }
      finally {
        if (jarFile != null) {
          // file was opened, but not successfully wrapped
          jarFile.close();
        }
      }
    }
    finally {
      tempFile.delete();
    }
  }

  @Test ( expected = NullPointerException.class )
  public void testGedcomxFileEntryNullPointerException() {
    new GedcomxFileEntry(null);
  }

  @Test ( expected = NullPointerException.class )
  public void testGedcomxOutputStreamAddResourceNullPointerException1() throws IOException {
    GedcomxOutputStream gedxOutputStream = new GedcomxOutputStream(new ByteArrayOutputStream());
    gedxOutputStream.addResource(null, null, null, null);
  }

  @Test ( expected = IllegalArgumentException.class )
  public void testGedcomxOutputStreamAddResourceIllegalArgumentException1() throws IOException {
    GedcomxOutputStream gedxOutputStream = new GedcomxOutputStream(new ByteArrayOutputStream());
    gedxOutputStream.addResource("", null, null, null);
  }

  @Test ( expected = NullPointerException.class )
  public void testGedcomxOutputStreamAddResourceNullPointerException2() throws IOException {
    GedcomxOutputStream gedxOutputStream = new GedcomxOutputStream(new ByteArrayOutputStream());
    gedxOutputStream.addResource(GedcomxConstants.GEDCOMX_JSON_MEDIA_TYPE, null, null, null);
  }

}
