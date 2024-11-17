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

import org.pentaho.big.data.kettle.plugins.hdfs.trans.HadoopFileOutputMeta;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInput;
import org.pentaho.metaverse.api.analyzer.kettle.step.BaseStepExternalResourceConsumer;

public class HadoopFileOutputExternalResourceConsumer
  extends BaseStepExternalResourceConsumer<TextFileInput, HadoopFileOutputMeta> {

  @Override
  public Class<HadoopFileOutputMeta> getMetaClass() {
    return HadoopFileOutputMeta.class;
  }

  @Override
  public boolean isDataDriven( final HadoopFileOutputMeta meta ) {
    return meta.isFileNameInField();
  }
}
