/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc. and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fudgemsg.taxon;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * Tests the RESTfulTaxonomyResolver implementation
 * 
 * @author Andrew Griffin
 */
public class RESTfulTaxonomyResolverTest {

  private static final String XML_PATH = System.getProperty ("user.name") + "/rest/";
  private static final String TAXONOMY_XML = "/var/www/html/" + XML_PATH;
  private static final String TAXONOMY_URL = "http://localhost/" + XML_PATH;
  
  /**
   * 
   * @throws IOException [documentation not available]
   */
  @Test
  public void testResolver () throws IOException {
    final File f = new File (TAXONOMY_XML + "42.xml");
    if ((f.exists () && !f.canWrite ()) || !f.getParentFile ().canWrite ()) {
      System.out.println ("Skipping RESTfulTaxonomyResolverTest::testResolver - no local webserver");
      return;
    }
    PropertyFileTaxonomyTest.writeTaxonomyXML (f);
    final TaxonomyResolver tr = new RESTfulTaxonomyResolver (TAXONOMY_URL, ".xml");
    assertNotNull (tr.resolveTaxonomy ((short)42));
    assertNull (tr.resolveTaxonomy ((short)43));
  }
  
}