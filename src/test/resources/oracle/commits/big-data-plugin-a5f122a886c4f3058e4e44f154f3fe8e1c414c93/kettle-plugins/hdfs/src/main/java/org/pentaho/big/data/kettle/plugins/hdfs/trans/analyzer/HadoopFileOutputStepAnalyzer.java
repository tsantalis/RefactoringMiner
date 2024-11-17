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
import org.pentaho.metaverse.api.IMetaverseNode;
import org.pentaho.metaverse.api.MetaverseAnalyzerException;

public class HadoopFileOutputStepAnalyzer extends HadoopBaseStepAnalyzer<HadoopFileOutputMeta> {

  @Override
  public Class<HadoopFileOutputMeta> getMetaClass() {
    return HadoopFileOutputMeta.class;
  }

  @Override public boolean isOutput() {
    return true;
  }

  @Override public boolean isInput() {
    return false;
  }

  @Override
  protected void customAnalyze( final HadoopFileOutputMeta meta, final IMetaverseNode rootNode )
    throws MetaverseAnalyzerException {
    super.customAnalyze( meta, rootNode );
    rootNode.setProperty( "createParentFolder", meta.isCreateParentFolder() );
    rootNode.setProperty( "doNotOpenNewFileInit", meta.isDoNotOpenNewFileInit() );
    if ( meta.isFileNameInField() ) {
      rootNode.setProperty( "fileNameField", meta.getFileNameField() );
    }
    rootNode.setProperty( "extension", meta.getExtension() );
    rootNode.setProperty( "stepNrInFilename", meta.isStepNrInFilename() );
    rootNode.setProperty( "partNrInFilename", meta.isPartNrInFilename() );
    rootNode.setProperty( "dateInFilename", meta.isDateInFilename() );
    rootNode.setProperty( "timeInFilename", meta.isTimeInFilename() );
    if ( meta.isSpecifyingFormat() ) {
      rootNode.setProperty( "dateTimeFormat", meta.getDateTimeFormat() );
    }
    rootNode.setProperty( "addFilenamesToResult", meta.isAddToResultFiles() );
    rootNode.setProperty( "append", meta.isFileAppended() );
    rootNode.setProperty( "separator", meta.getSeparator() );
    rootNode.setProperty( "enclosure", meta.getEnclosure() );
    rootNode.setProperty( "forceEnclosure", meta.isEnclosureForced() );
    rootNode.setProperty( "addHeader", meta.isHeaderEnabled() );
    rootNode.setProperty( "addFooter", meta.isFooterEnabled() );
    rootNode.setProperty( "fileFormat", meta.getFileFormat() );
    rootNode.setProperty( "fileCompression", meta.getFileCompression() );
    rootNode.setProperty( "encoding", meta.getEncoding() );
    rootNode.setProperty( "rightPadFields", meta.isPadded() );
    rootNode.setProperty( "fastDataDump", meta.isFastDump() );
    rootNode.setProperty( "splitEveryRows", meta.getSplitEveryRows() );
    rootNode.setProperty( "endingLine", meta.getEndedLine() );
  }
}
