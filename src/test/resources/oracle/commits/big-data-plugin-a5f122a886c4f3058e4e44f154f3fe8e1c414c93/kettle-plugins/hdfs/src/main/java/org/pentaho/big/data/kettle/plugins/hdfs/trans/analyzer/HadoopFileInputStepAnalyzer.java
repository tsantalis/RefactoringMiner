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

import org.pentaho.big.data.kettle.plugins.hdfs.trans.HadoopFileInputMeta;
import org.pentaho.metaverse.api.IMetaverseNode;
import org.pentaho.metaverse.api.MetaverseAnalyzerException;

public class HadoopFileInputStepAnalyzer extends HadoopBaseStepAnalyzer<HadoopFileInputMeta> {

  @Override
  public Class<HadoopFileInputMeta> getMetaClass() {
    return HadoopFileInputMeta.class;
  }

  @Override public boolean isOutput() {
    return false;
  }

  @Override public boolean isInput() {
    return true;
  }

  @Override
  protected void customAnalyze( final HadoopFileInputMeta meta, final IMetaverseNode rootNode )
    throws MetaverseAnalyzerException {
    super.customAnalyze( meta, rootNode );
    if ( meta.isAcceptingFilenames() ) {
      rootNode.setProperty( "fileNameStep", meta.getAcceptingStepName() );
      rootNode.setProperty( "fileNameField", meta.getAcceptingField() );
      rootNode.setProperty( "passingThruFields", meta.inputFiles.passingThruFields );
    }
    rootNode.setProperty( "fileType", meta.content.fileType );
    rootNode.setProperty( "separator", meta.content.separator );
    rootNode.setProperty( "enclosure", meta.content.enclosure );
    rootNode.setProperty( "breakInEnclosureAllowed", meta.content.breakInEnclosureAllowed );
    rootNode.setProperty( "escapeCharacter", meta.content.escapeCharacter );
    if ( meta.content.header ) {
      rootNode.setProperty( "nrHeaderLines", meta.content.nrHeaderLines );
    }
    if ( meta.content.footer ) {
      rootNode.setProperty( "nrFooterLines", meta.content.nrFooterLines );
    }
    if ( meta.content.lineWrapped ) {
      rootNode.setProperty( "nrWraps", meta.content.nrWraps );
    }
    if ( meta.content.layoutPaged ) {
      rootNode.setProperty( "nrLinesPerPage", meta.content.nrLinesPerPage );
      rootNode.setProperty( "nrLinesDocHeader", meta.content.nrLinesDocHeader );
    }
    rootNode.setProperty( "fileCompression", meta.content.fileCompression );
    rootNode.setProperty( "noEmptyLines", meta.content.noEmptyLines );
    rootNode.setProperty( "includeFilename", meta.content.includeFilename );
    if ( meta.content.includeFilename ) {
      rootNode.setProperty( "filenameField", meta.content.filenameField );
    }
    rootNode.setProperty( "includeRowNumber", meta.content.includeRowNumber );
    if ( meta.content.includeFilename ) {
      rootNode.setProperty( "rowNumberField", meta.content.rowNumberField );
      rootNode.setProperty( "rowNumberByFile", meta.content.rowNumberByFile );
    }
    rootNode.setProperty( "fileFormat", meta.content.fileFormat );
    rootNode.setProperty( "encoding", meta.content.encoding );
    rootNode.setProperty( "rowLimit", Long.toString( meta.content.rowLimit ) );
    rootNode.setProperty( "dateFormatLenient", meta.content.dateFormatLenient );
    rootNode.setProperty( "dateFormatLocale", meta.content.dateFormatLocale );
    rootNode.setProperty( "addFilenamesToResult", meta.inputFiles.isaddresult );
  }
}
