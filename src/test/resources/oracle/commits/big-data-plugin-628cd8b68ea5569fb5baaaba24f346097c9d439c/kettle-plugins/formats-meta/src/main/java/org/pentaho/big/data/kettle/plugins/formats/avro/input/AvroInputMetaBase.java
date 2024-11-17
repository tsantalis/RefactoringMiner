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

package org.pentaho.big.data.kettle.plugins.formats.avro.input;

import java.util.List;

import org.pentaho.big.data.kettle.plugins.formats.FormatInputOutputField;
import org.pentaho.big.data.kettle.plugins.formats.FormatInputFile;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.steps.file.BaseFileInputAdditionalField;
import org.pentaho.di.trans.steps.file.BaseFileInputMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * Avro input meta step without Hadoop-dependent classes. Required for read meta in the spark native code.
 * 
 * @author Alexander Buloichik
 */
public abstract class AvroInputMetaBase extends
    BaseFileInputMeta<BaseFileInputAdditionalField, FormatInputFile, FormatInputOutputField> {

  public AvroInputMetaBase() {
    additionalOutputFields = new BaseFileInputAdditionalField();
    inputFiles = new FormatInputFile();
    inputFields = new FormatInputOutputField[0];
  }

  @Override
  public String getXML() {
    StringBuilder retval = new StringBuilder( 1500 );

    retval.append( "    <file>" ).append( Const.CR );
    for ( int i = 0; i < inputFiles.fileName.length; i++ ) {
      retval.append( "      " ).append( XMLHandler.addTagValue( "environment", inputFiles.environment[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "name", inputFiles.fileName[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "filemask", inputFiles.fileMask[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "exclude_filemask", inputFiles.excludeFileMask[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "file_required", inputFiles.fileRequired[i] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "include_subfolders",
          inputFiles.includeSubFolders[i] ) );
    }
    retval.append( "    </file>" ).append( Const.CR );

    retval.append( "    <fields>" ).append( Const.CR );
    for ( int i = 0; i < inputFields.length; i++ ) {
      FormatInputOutputField field = inputFields[i];
      retval.append( "      <field>" ).append( Const.CR );
      retval.append( "        " ).append( XMLHandler.addTagValue( "path", field.getPath() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "name", field.getName() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "type", field.getTypeDesc() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "format", field.getFormat() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "currency", field.getCurrencySymbol() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "decimal", field.getDecimalSymbol() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "group", field.getGroupSymbol() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "nullif", field.getNullString() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "ifnull", field.getIfNullValue() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "position", field.getPosition() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "length", field.getLength() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "precision", field.getPrecision() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "trim_type", field.getTrimTypeCode() ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "repeat", field.isRepeated() ) );
      retval.append( "      </field>" ).append( Const.CR );
    }
    retval.append( "    </fields>" ).append( Const.CR );

    return retval.toString();
  }

  @Override
  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step )
    throws KettleException {
    try {
      for ( int i = 0; i < inputFiles.fileName.length; i++ ) {
        rep.saveStepAttribute( id_transformation, id_step, i, "environment", inputFiles.environment[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "file_name", inputFiles.fileName[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "file_mask", inputFiles.fileMask[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "exclude_file_mask", inputFiles.excludeFileMask[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "file_required", inputFiles.fileRequired[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "include_subfolders", inputFiles.includeSubFolders[i] );
      }

      for ( int i = 0; i < inputFields.length; i++ ) {
        FormatInputOutputField field = inputFields[i];

        rep.saveStepAttribute( id_transformation, id_step, i, "path", field.getPath() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_name", field.getName() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_type", field.getTypeDesc() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_format", field.getFormat() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_currency", field.getCurrencySymbol() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_decimal", field.getDecimalSymbol() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_group", field.getGroupSymbol() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_nullif", field.getNullString() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_ifnull", field.getIfNullValue() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_position", field.getPosition() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_length", field.getLength() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_precision", field.getPrecision() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_trim_type", field.getTrimTypeCode() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_repeat", field.isRepeated() );
      }
    } catch ( Exception e ) {
      throw new KettleException( "Unable to save step information to the repository for id_step=" + id_step, e );
    }
  }

  @Override
  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    Node filenode = XMLHandler.getSubNode( stepnode, "file" );
    Node fields = XMLHandler.getSubNode( stepnode, "fields" );
    int nrfiles = XMLHandler.countNodes( filenode, "name" );
    int nrfields = XMLHandler.countNodes( fields, "field" );

    allocateFiles( nrfiles );
    for ( int i = 0; i < nrfiles; i++ ) {
      Node envnode = XMLHandler.getSubNodeByNr( filenode, "environment", i );
      Node filenamenode = XMLHandler.getSubNodeByNr( filenode, "name", i );
      Node filemasknode = XMLHandler.getSubNodeByNr( filenode, "filemask", i );
      Node excludefilemasknode = XMLHandler.getSubNodeByNr( filenode, "exclude_filemask", i );
      Node fileRequirednode = XMLHandler.getSubNodeByNr( filenode, "file_required", i );
      Node includeSubFoldersnode = XMLHandler.getSubNodeByNr( filenode, "include_subfolders", i );
      inputFiles.environment[i] = XMLHandler.getNodeValue( envnode );
      inputFiles.fileName[i] = XMLHandler.getNodeValue( filenamenode );
      inputFiles.fileMask[i] = XMLHandler.getNodeValue( filemasknode );
      inputFiles.excludeFileMask[i] = XMLHandler.getNodeValue( excludefilemasknode );
      inputFiles.fileRequired[i] = XMLHandler.getNodeValue( fileRequirednode );
      inputFiles.includeSubFolders[i] = XMLHandler.getNodeValue( includeSubFoldersnode );
    }

    inputFields = new FormatInputOutputField[nrfields];
    for ( int i = 0; i < nrfields; i++ ) {
      Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );
      FormatInputOutputField field = new FormatInputOutputField();

      field.setPath( XMLHandler.getTagValue( fnode, "path" ) );
      field.setName( XMLHandler.getTagValue( fnode, "name" ) );
      field.setType( ValueMetaFactory.getIdForValueMeta( XMLHandler.getTagValue( fnode, "type" ) ) );
      field.setFormat( XMLHandler.getTagValue( fnode, "format" ) );
      field.setCurrencySymbol( XMLHandler.getTagValue( fnode, "currency" ) );
      field.setDecimalSymbol( XMLHandler.getTagValue( fnode, "decimal" ) );
      field.setGroupSymbol( XMLHandler.getTagValue( fnode, "group" ) );
      field.setNullString( XMLHandler.getTagValue( fnode, "nullif" ) );
      field.setIfNullValue( XMLHandler.getTagValue( fnode, "ifnull" ) );
      field.setPosition( Const.toInt( XMLHandler.getTagValue( fnode, "position" ), -1 ) );
      field.setLength( Const.toInt( XMLHandler.getTagValue( fnode, "length" ), -1 ) );
      field.setPrecision( Const.toInt( XMLHandler.getTagValue( fnode, "precision" ), -1 ) );
      field.setTrimType( ValueMetaString.getTrimTypeByCode( XMLHandler.getTagValue( fnode, "trim_type" ) ) );
      field.setRepeated( YES.equalsIgnoreCase( XMLHandler.getTagValue( fnode, "repeat" ) ) );

      inputFields[i] = field;
    }
  }

  @Override
  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases )
    throws KettleException {
    try {
      int nrfiles = rep.countNrStepAttributes( id_step, "file_name" );

      allocateFiles( nrfiles );

      for ( int i = 0; i < nrfiles; i++ ) {
        inputFiles.environment[i] = rep.getStepAttributeString( id_step, i, "environment" );
        inputFiles.fileName[i] = rep.getStepAttributeString( id_step, i, "file_name" );
        inputFiles.fileMask[i] = rep.getStepAttributeString( id_step, i, "file_mask" );
        inputFiles.excludeFileMask[i] = rep.getStepAttributeString( id_step, i, "exclude_file_mask" );
        inputFiles.fileRequired[i] = rep.getStepAttributeString( id_step, i, "file_required" );
        if ( !YES.equalsIgnoreCase( inputFiles.fileRequired[i] ) ) {
          inputFiles.fileRequired[i] = NO;
        }
        inputFiles.includeSubFolders[i] = rep.getStepAttributeString( id_step, i, "include_subfolders" );
        if ( !YES.equalsIgnoreCase( inputFiles.includeSubFolders[i] ) ) {
          inputFiles.includeSubFolders[i] = NO;
        }
      }

      int nrfields = rep.countNrStepAttributes( id_step, "field_name" );
      inputFields = new FormatInputOutputField[nrfields];
      for ( int i = 0; i < nrfields; i++ ) {
        FormatInputOutputField field = new FormatInputOutputField();

        field.setPath( rep.getStepAttributeString( id_step, i, "path" ) );
        field.setName( rep.getStepAttributeString( id_step, i, "field_name" ) );
        field.setType( ValueMetaFactory.getIdForValueMeta( rep.getStepAttributeString( id_step, i, "field_type" ) ) );
        field.setFormat( rep.getStepAttributeString( id_step, i, "field_format" ) );
        field.setCurrencySymbol( rep.getStepAttributeString( id_step, i, "field_currency" ) );
        field.setDecimalSymbol( rep.getStepAttributeString( id_step, i, "field_decimal" ) );
        field.setGroupSymbol( rep.getStepAttributeString( id_step, i, "field_group" ) );
        field.setNullString( rep.getStepAttributeString( id_step, i, "field_nullif" ) );
        field.setIfNullValue( rep.getStepAttributeString( id_step, i, "field_ifnull" ) );
        field.setPosition( (int) rep.getStepAttributeInteger( id_step, i, "field_position" ) );
        field.setLength( (int) rep.getStepAttributeInteger( id_step, i, "field_length" ) );
        field.setPrecision( (int) rep.getStepAttributeInteger( id_step, i, "field_precision" ) );
        field.setTrimType( ValueMetaString.getTrimTypeByCode( rep.getStepAttributeString( id_step, i,
            "field_trim_type" ) ) );
        field.setRepeated( rep.getStepAttributeBoolean( id_step, i, "field_repeat" ) );

        inputFields[i] = field;
      }

    } catch ( Exception e ) {
      throw new KettleException( "Unexpected error reading step information from the repository", e );
    }
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
    inputFields = new FormatInputOutputField[0];
  }
}
