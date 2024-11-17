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
import org.pentaho.big.data.kettle.plugins.hdfs.trans.HadoopFileOutputMeta;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith( MockitoJUnitRunner.class )
public class HadoopFileOutputStepAnalyzerTest extends HadoopBaseStepAnalyzerTest<HadoopFileOutputStepAnalyzer,
  HadoopFileOutputMeta> {

  @Mock private HadoopFileOutputMeta meta;

  @Override
  protected HadoopFileOutputStepAnalyzer getAnalyzer() {
    return new HadoopFileOutputStepAnalyzer();
  }

  @Override
  protected HadoopFileOutputMeta getMetaMock() {
    return meta;
  }

  @Override
  protected Class<HadoopFileOutputMeta> getMetaClass() {
    return HadoopFileOutputMeta.class;
  }

  @Test
  public void testIsOutput() throws Exception {
    assertTrue( analyzer.isOutput() );
  }

  @Test
  public void testIsInput() throws Exception {
    assertFalse( analyzer.isInput() );
  }
}
