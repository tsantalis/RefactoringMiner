/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
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

import org.eclipse.swt.custom.CCombo;
import org.junit.Test;
import org.pentaho.di.core.Const;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.internal.verification.VerificationModeFactory.times;

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
    assertNull( HadoopFileOutputDialog.getUrlPath( expected ) );
  }

  @Test
  public void testGetUrlPathVariablePrefix() {
    String expected = "${myTestVar}";
    assertNull( HadoopFileOutputDialog.getUrlPath( expected ) );
  }

  @Test
  public void testGetUrlPathRootPath() {
    assertEquals( "/", HadoopFileOutputDialog.getUrlPath( "hdfs://myhost:8020/" ) );
  }

  @Test
  public void testGetUrlPathRootPathWithoutSlash() {
    assertEquals( "/", HadoopFileOutputDialog.getUrlPath( "hdfs://myhost:8020" ) );
  }

  @Test
  public void testFillWithSupportedDateFormats() {
    HadoopFileOutputDialog dialog = mock( HadoopFileOutputDialog.class );
    CCombo combo = mock( CCombo.class );

    String[] dates = Const.getDateFormats();
    assertEquals( 20, dates.length );

    // currently there are 20 date formats, 10 of which contain ':' characters which are illegal in hadoop filenames
    // if the formats returned change, the numbers on this test should be adjusted

    doCallRealMethod().when( dialog ).fillWithSupportedDateFormats( any(), any() );
    dialog.fillWithSupportedDateFormats( combo, dates );

    verify( combo, times( 10 ) ).add( any() );
  }
}
