/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2018 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.big.data.kettle.plugins.hdfs.trans.analyzer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.big.data.kettle.plugins.hdfs.trans.HadoopFileInputMeta;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith( MockitoJUnitRunner.class )
public class HadoopFileInputStepAnalyzerTest
  extends HadoopBaseStepAnalyzerTest<HadoopFileInputStepAnalyzer, HadoopFileInputMeta> {

  @Mock private HadoopFileInputMeta meta;

  @Override
  protected HadoopFileInputStepAnalyzer getAnalyzer() {
    return new HadoopFileInputStepAnalyzer();
  }

  @Override
  protected HadoopFileInputMeta getMetaMock() {
    return meta;
  }

  @Override
  protected Class<HadoopFileInputMeta> getMetaClass() {
    return HadoopFileInputMeta.class;
  }

  @Test
  public void testIsOutput() throws Exception {
    assertFalse( analyzer.isOutput() );
  }

  @Test
  public void testIsInput() throws Exception {
    assertTrue( analyzer.isInput() );
  }
}
