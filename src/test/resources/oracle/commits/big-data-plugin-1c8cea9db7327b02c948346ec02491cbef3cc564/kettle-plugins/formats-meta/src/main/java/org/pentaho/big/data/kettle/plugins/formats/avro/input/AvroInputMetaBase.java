/*! ******************************************************************************
 *
 * Pentaho Data Integration
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

package org.pentaho.big.data.kettle.plugins.formats.avro.input;

import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.pentaho.big.data.kettle.plugins.formats.FormatInputFile;
import org.pentaho.big.data.kettle.plugins.formats.avro.output.AvroOutputMetaBase;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.vfs.AliasedFileObject;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.steps.file.BaseFileInputAdditionalField;
import org.pentaho.di.trans.steps.file.BaseFileInputMeta;
import org.pentaho.di.workarounds.ResolvableResource;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * Avro input meta step without Hadoop-dependent classes. Required for read meta in the spark native code.
 *
 * @author Alexander Buloichik
 */
@SuppressWarnings( "deprecation" )
public abstract class AvroInputMetaBase extends
    BaseFileInputMeta<BaseFileInputAdditionalField, FormatInputFile, AvroInputField> implements ResolvableResource {

  private static final Class<?> PKG = AvroOutputMetaBase.class;

  @Injection( name = "AVRO_FILENAME" )
  private String filename;

  @Injection( name = "SCHEMA_FILENAME" )
  protected String schemaFilename;

  @Injection( name = "STREAM_FIELDNAME" )
  protected String inputStreamFieldName;

  @Injection( name = "USE_INPUT_STREAM" )
  protected boolean useFieldAsInputStream;

  public AvroInputMetaBase() {
    additionalOutputFields = new BaseFileInputAdditionalField();
    inputFiles = new FormatInputFile();
    inputFields = new AvroInputField[0];
  }


  public void allocateFiles( int nrFiles ) {
    inputFiles.environment = new String[nrFiles];
    inputFiles.fileName = new String[nrFiles];
    inputFiles.fileMask = new String[nrFiles];
    inputFiles.excludeFileMask = new String[nrFiles];
    inputFiles.fileRequired = new String[nrFiles];
    inputFiles.includeSubFolders = new String[nrFiles];
  }

  /**
   * TODO: remove from base
   */
  @Override
  public String getEncoding() {
    return null;
  }

  @Override
  public void setDefault() {
    allocateFiles( 0 );
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename( String filename ) {
    this.filename = filename;
  }

  public AvroInputField[] getInputFields() {
    return this.inputFields;
  }

  public void setInputFields( AvroInputField[] inputFields )  {
    this.inputFields = inputFields;
  }

  public void setInputFields( List<AvroInputField> inputFields ) {
    this.inputFields = new AvroInputField[inputFields.size()];
    this.inputFields = inputFields.toArray( this.inputFields );
  }

  @Override
  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode, metaStore );
  }

  private void readData( Node stepnode, IMetaStore metastore ) throws KettleXMLException {
    try {
      String passFileds = XMLHandler.getTagValue( stepnode, "passing_through_fields" ) == null ? "false" : XMLHandler.getTagValue( stepnode, "passing_through_fields" );
      inputFiles.passingThruFields = ValueMetaBase.convertStringToBoolean( passFileds );
      filename = XMLHandler.getTagValue( stepnode, "filename" );
      useFieldAsInputStream = ValueMetaBase.convertStringToBoolean(
        XMLHandler.getTagValue( stepnode, "useStreamField" ) == null ? "false" : XMLHandler.getTagValue( stepnode, "useStreamField" ) );
      inputStreamFieldName = XMLHandler.getTagValue( stepnode, "stream_fieldname" );
      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
      int nrfields = XMLHandler.countNodes( fields, "field" );
      this.inputFields = new AvroInputField[ nrfields];
      for ( int i = 0; i < nrfields; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );
        AvroInputField inputField = new AvroInputField();
        inputField.setFormatFieldName( XMLHandler.getTagValue( fnode, "path" ) );
        inputField.setPentahoFieldName( XMLHandler.getTagValue( fnode, "name" ) );
        inputField.setPentahoType( XMLHandler.getTagValue( fnode, "type" ) );
        inputField.setAvroType( XMLHandler.getTagValue( fnode, "avro_type" ) );
        String stringFormat = XMLHandler.getTagValue( fnode, "format" );
        inputField.setStringFormat( stringFormat == null ? "" : stringFormat );
        this.inputFields[ i ] = inputField;
      }
      schemaFilename = XMLHandler.getTagValue( stepnode, "schemaFilename" );
    } catch ( Exception e ) {
      throw new KettleXMLException( "Unable to load step info from XML", e );
    }
  }

  @Override
  public String getXML() {
    StringBuffer retval = new StringBuffer( 800 );
    final String INDENT = "    ";

    //we need the equals by size arrays for inputFiles.fileName[i], inputFiles.fileMask[i], inputFiles.fileRequired[i], inputFiles.includeSubFolders[i]
    //to prevent the ArrayIndexOutOfBoundsException
    //This line was introduced to prevent future bug if we will suppport the several input files for avro like we do for orc and parquet
    inputFiles.normalizeAllocation( inputFiles.fileName.length );

    retval.append( INDENT ).append( XMLHandler.addTagValue( "passing_through_fields", inputFiles.passingThruFields ) );
    retval.append( INDENT ).append( XMLHandler.addTagValue( "filename", getFilename() ) );
    retval.append( INDENT ).append( XMLHandler.addTagValue( "useStreamField", isUseFieldAsInputStream() ) );
    retval.append( INDENT ).append( XMLHandler.addTagValue( "stream_fieldname", getInputStreamFieldName() ) );

    retval.append( "    <fields>" ).append( Const.CR );
    for ( int i = 0; i < inputFields.length; i++ ) {
      AvroInputField field = inputFields[ i ];

      if ( field.getPentahoFieldName() != null && field.getPentahoFieldName().length() != 0 ) {
        retval.append( "      <field>" ).append( Const.CR );
        retval.append( "        " ).append( XMLHandler.addTagValue( "path", field.getAvroFieldName() ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "name", field.getPentahoFieldName() ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "type", field.getTypeDesc() ) );
        if ( field.getAvroType() != null ) {
          retval.append( "        " ).append( XMLHandler.addTagValue( "avro_type", field.getAvroType().getName() ) );
        }
        if ( field.getStringFormat() != null ) {
          retval.append( "        " ).append( XMLHandler.addTagValue( "format", field.getStringFormat() ) );
        }
        retval.append( "      </field>" ).append( Const.CR );
      }
    }
    retval.append( "    </fields>" ).append( Const.CR );

    retval.append( INDENT ).append( XMLHandler.addTagValue( "schemaFilename", getSchemaFilename() ) );
    return retval.toString();
  }

  @Override
  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases )
      throws KettleException {
    try {

      inputFiles.passingThruFields = rep.getStepAttributeBoolean( id_step, "passing_through_fields" );
      filename = rep.getStepAttributeString( id_step, "filename" );
      useFieldAsInputStream = rep.getStepAttributeBoolean( id_step, "useStreamField" );
      inputStreamFieldName = rep.getStepAttributeString( id_step, "stream_fieldname" );

      // using the "type" column to get the number of field rows because "type" is guaranteed not to be null.
      int nrfields = rep.countNrStepAttributes( id_step, "type" );

      this.inputFields = new AvroInputField[ nrfields];
      for ( int i = 0; i < nrfields; i++ ) {
        AvroInputField inputField = new AvroInputField();
        inputField.setFormatFieldName( rep.getStepAttributeString( id_step, i, "path" ) );
        inputField.setPentahoFieldName( rep.getStepAttributeString( id_step, i, "name" ) );
        inputField.setPentahoType( rep.getStepAttributeString( id_step, i, "type" ) );
        inputField.setAvroType( rep.getStepAttributeString( id_step, i, "avro_type" ) );
        String stringFormat = rep.getStepAttributeString( id_step, i, "format" );
        inputField.setStringFormat( stringFormat == null ? "" : stringFormat );
        this.inputFields[ i ] = inputField;
      }
      schemaFilename = rep.getStepAttributeString( id_step, "schemaFilename" );
    } catch ( Exception e ) {
      throw new KettleException( "Unexpected error reading step information from the repository", e );
    }
  }

  @Override
  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step )
      throws KettleException {
    try {
      rep.saveStepAttribute( id_transformation, id_step, "passing_through_fields", inputFiles.passingThruFields );
      rep.saveStepAttribute( id_transformation, id_step, "filename", getFilename() );
      rep.saveStepAttribute( id_transformation, id_step, "useStreamField", isUseFieldAsInputStream() );
      rep.saveStepAttribute( id_transformation, id_step, "stream_fieldname", getInputStreamFieldName() );

      for ( int i = 0; i < inputFields.length; i++ ) {
        AvroInputField field = inputFields[ i ];

        rep.saveStepAttribute( id_transformation, id_step, i, "path", field.getAvroFieldName() );
        rep.saveStepAttribute( id_transformation, id_step, i, "name", field.getPentahoFieldName() );
        rep.saveStepAttribute( id_transformation, id_step, i, "type", field.getTypeDesc() );
        if ( field.getAvroType() != null ) {
          rep.saveStepAttribute( id_transformation, id_step, i, "avro_type", field.getAvroType().getName() );
        }
        if ( field.getStringFormat() != null ) {
          rep.saveStepAttribute( id_transformation, id_step, i, "format", field.getStringFormat() );
        }
      }
      super.saveRep( rep, metaStore, id_transformation, id_step );
      rep.saveStepAttribute( id_transformation, id_step, "schemaFilename", getSchemaFilename() );
    } catch ( Exception e ) {
      throw new KettleException( "Unable to save step information to the repository for id_step=" + id_step, e );
    }
  }

  @Override
  public void resolve() {
    if ( filename != null && !filename.isEmpty() ) {
      try {
        String realFileName = getParentStepMeta().getParentTransMeta().environmentSubstitute( filename );
        FileObject fileObject = KettleVFS.getFileObject( realFileName );
        if ( AliasedFileObject.isAliasedFile( fileObject ) ) {
          filename = ( (AliasedFileObject) fileObject ).getOriginalURIString();
        }
      } catch ( KettleFileException e ) {
        throw new RuntimeException( e );
      }
    }

    if ( schemaFilename != null && !schemaFilename.isEmpty() ) {
      try {
        String realSchemaFilename = getParentStepMeta().getParentTransMeta().environmentSubstitute( schemaFilename );
        FileObject fileObject = KettleVFS.getFileObject( realSchemaFilename );
        if ( AliasedFileObject.isAliasedFile( fileObject ) ) {
          schemaFilename = ( (AliasedFileObject) fileObject ).getOriginalURIString();
        }
      } catch ( KettleFileException e ) {
        throw new RuntimeException( e );
      }
    }
  }

  public String getSchemaFilename() {
    return schemaFilename;
  }

  public void setSchemaFilename( String schemaFilename ) {
    this.schemaFilename = schemaFilename;
  }

  public String getInputStreamFieldName() {
    return inputStreamFieldName;
  }

  public void setInputStreamFieldName( String inputStreamFieldName ) {
    this.inputStreamFieldName = inputStreamFieldName;
  }

  public boolean isUseFieldAsInputStream() {
    return useFieldAsInputStream;
  }

  public void setUseFieldAsInputStream( boolean useFieldAsInputStream ) {
    this.useFieldAsInputStream = useFieldAsInputStream;
  }
}
