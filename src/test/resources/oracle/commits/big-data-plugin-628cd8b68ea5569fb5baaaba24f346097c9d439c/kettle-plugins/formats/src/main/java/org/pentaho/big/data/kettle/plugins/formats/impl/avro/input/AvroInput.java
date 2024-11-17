/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.big.data.kettle.plugins.formats.impl.avro.input;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.big.data.api.cluster.service.locator.NamedClusterServiceLocator;
import org.pentaho.big.data.kettle.plugins.formats.FormatInputOutputField;
import org.pentaho.bigdata.api.format.FormatService;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.file.BaseFileInputStep;
import org.pentaho.di.trans.steps.file.IBaseFileInputReader;
import org.pentaho.hadoop.shim.api.format.IPentahoInputFormat.IPentahoInputSplit;
import org.pentaho.hadoop.shim.api.format.IPentahoAvroInputFormat;
import org.pentaho.hadoop.shim.api.format.SchemaDescription;

public class AvroInput extends BaseFileInputStep<AvroInputMeta, AvroInputData> {
  public static long SPLIT_SIZE = 128 * 1024 * 1024;

  private final NamedClusterServiceLocator namedClusterServiceLocator;

  public AvroInput( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
      Trans trans, NamedClusterServiceLocator namedClusterServiceLocator ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
    this.namedClusterServiceLocator = namedClusterServiceLocator;
  }

  @Override
  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    meta = (AvroInputMeta) smi;
    data = (AvroInputData) sdi;

    try {
      if ( data.splits == null ) {
        initSplits();
      }

      if ( data.currentSplit >= data.splits.size() ) {
        setOutputDone();
        return false;
      }

      if ( data.reader == null ) {
        openReader( data );
      }

      if ( data.rowIterator.hasNext() ) {
        RowMetaAndData row = data.rowIterator.next();
        putRow( row.getRowMeta(), row.getData() );
        return true;
      } else {
        data.reader.close();
        data.reader = null;
        logDebug( "Close split {0}", data.currentSplit );
        data.currentSplit++;
        return true;
      }
    } catch ( KettleException ex ) {
      throw ex;
    } catch ( Exception ex ) {
      throw new KettleException( ex );
    }
  }

  void initSplits() throws Exception {
    FormatService formatService = namedClusterServiceLocator.getService( meta.getNamedCluster(), FormatService.class );
    if ( meta.inputFiles == null || meta.inputFiles.fileName == null || meta.inputFiles.fileName.length == 0 ) {
      throw new KettleException( "No input files defined" );
    }
    SchemaDescription schema = new SchemaDescription();
    for ( FormatInputOutputField f : meta.inputFields ) {
      SchemaDescription.Field field = schema.new Field( f.getPath(), f.getName(), f.getType(), true );
      schema.addField( field );
    }

    data.input = formatService.createInputFormat( IPentahoAvroInputFormat.class );
    data.input.setSchema( schema );
    data.input.setInputFile( meta.inputFiles.fileName[0] );
    data.input.setSplitSize( SPLIT_SIZE );

    data.splits = data.input.getSplits();
    logDebug( "Input split count: {0}", data.splits.size() );
    data.currentSplit = 0;
  }

  void openReader( AvroInputData data ) throws Exception {
    logDebug( "Open split {0}", data.currentSplit );
    IPentahoInputSplit sp = data.splits.get( data.currentSplit );
    data.reader = data.input.createRecordReader( sp );
    data.rowIterator = data.reader.iterator();
  }

  @Override
  protected boolean init() {
    return true;
  }

  @Override
  protected IBaseFileInputReader createReader( AvroInputMeta meta, AvroInputData data, FileObject file )
    throws Exception {
    return null;
  }
}
