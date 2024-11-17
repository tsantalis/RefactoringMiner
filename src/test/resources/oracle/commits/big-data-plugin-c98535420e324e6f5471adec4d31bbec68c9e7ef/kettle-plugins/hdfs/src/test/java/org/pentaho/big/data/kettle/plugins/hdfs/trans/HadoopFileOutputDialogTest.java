/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.big.data.kettle.plugins.hdfs.trans;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by bryan on 11/23/15.
 */
public class HadoopFileOutputDialogTest {
  @Test
  public void testGetUrlPathHdfsPrefix() {
    String prefixToBeRemoved = "hdfs://myhost:8020";
    String expected = "/path/to/file";
    assertEquals( expected, HadoopFileOutputDialog.getUrlPath( prefixToBeRemoved + expected ) );
  }

  @Test
  public void testGetUrlPathMapRPRefix() {
    String prefixToBeRemoved = "maprfs://";
    String expected = "/path/to/file";
    assertEquals( expected, HadoopFileOutputDialog.getUrlPath( prefixToBeRemoved + expected ) );
  }

  @Test
  public void testGetUrlPathSpecialPrefix() {
    String prefixToBeRemoved = "mySpecialPrefix://host";
    String expected = "/path/to/file";
    assertEquals( expected, HadoopFileOutputDialog.getUrlPath( prefixToBeRemoved + expected ) );
  }

  @Test
  public void testGetUrlPathNoPrefix() {
    String expected = "/path/to/file";
    assertEquals( expected, HadoopFileOutputDialog.getUrlPath( expected ) );
  }

  @Test
  public void testGetUrlPathVariablePrefix() {
    String expected = "${myTestVar}";
    assertEquals( expected, HadoopFileOutputDialog.getUrlPath( expected ) );
  }
}