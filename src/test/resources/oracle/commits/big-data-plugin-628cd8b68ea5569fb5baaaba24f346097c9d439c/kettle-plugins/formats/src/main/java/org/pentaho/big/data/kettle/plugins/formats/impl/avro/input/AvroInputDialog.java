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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.big.data.kettle.plugins.formats.FormatInputOutputField;
import org.pentaho.big.data.kettle.plugins.formats.impl.avro.BaseAvroStepDialog;
import org.pentaho.big.data.kettle.plugins.formats.avro.input.AvroInputMetaBase;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;

public class AvroInputDialog extends BaseAvroStepDialog<AvroInputMetaBase> {

  private static final int DIALOG_WIDTH = 526;

  private static final int DIALOG_HEIGHT = 506;

  private static final int AVRO_PATH_COLUMN_INDEX = 1;

  private static final int FIELD_NAME_COLUMN_INDEX = 2;

  private static final int FIELD_TYPE_COLUMN_INDEX = 3;

  private TableView wInputFields;

  public AvroInputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (AvroInputMetaBase) in, transMeta, sname );
  }

  @Override
  protected Control createAfterFile( Composite shell ) {

    Button wGetFields = new Button( shell, SWT.PUSH );
    wGetFields.setText( BaseMessages.getString( PKG, "AvroInputDialog.Fields.Get" ) );
    wGetFields.addListener( SWT.Selection, event -> {
      // TODO
      throw new UnsupportedOperationException();
    } );
    props.setLook( wGetFields );
    new FD( wGetFields ).bottom( 100, 0 ).right( 100, 0 ).apply();

    Label wlFields = new Label( shell, SWT.RIGHT );
    wlFields.setText( BaseMessages.getString( PKG, "AvroInputDialog.Fields.Label" ) );
    props.setLook( wlFields );
    new FD( wlFields ).left( 0, 0 ).top( 0, FIELDS_SEP ).apply();
    ColumnInfo[] parameterColumns = new ColumnInfo[] {
      new ColumnInfo( BaseMessages.getString( PKG, "AvroInputDialog.Fields.column.AvroPath" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false, false ),
      new ColumnInfo( BaseMessages.getString( PKG, "AvroInputDialog.Fields.column.Name" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false, false ),
      new ColumnInfo( BaseMessages.getString( PKG, "AvroInputDialog.Fields.column.Type" ),
          ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaFactory.getValueMetaNames() ) };
    wInputFields =
        new TableView( transMeta, shell, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.NO_SCROLL | SWT.V_SCROLL,
            parameterColumns, 7, lsMod, props );
    props.setLook( wInputFields );
    new FD( wInputFields ).left( 0, 0 ).right( 100, 0 ).top( wlFields, FIELD_LABEL_SEP )
      .bottom( wGetFields, -FIELDS_SEP ).apply();

    for ( ColumnInfo col : parameterColumns ) {
      col.setAutoResize( false );
    }
    ColumnsResizer resizer = new ColumnsResizer( 0, 50, 25, 25 );
    wInputFields.getTable().addListener( SWT.Resize, resizer );
    setTruncatedColumn( wInputFields.getTable(), 1 );
    if ( !Const.isWindows() ) {
      addColumnTooltip( wInputFields.getTable(), 1 );
    }
    return wGetFields;
  }

  /**
   * Read the data from the meta object and show it in this dialog.
   */
  @Override
  protected void getData( AvroInputMetaBase meta ) {
    if ( meta.inputFiles.fileName.length > 0 ) {
      wPath.setText( meta.inputFiles.fileName[0] );
    }
    int nrFields = meta.inputFields.length;
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = null;
      if ( i < wInputFields.table.getItemCount() ) {
        item = wInputFields.table.getItem( i );
      } else {
        item = new TableItem( wInputFields.table, SWT.NONE );
      }

      FormatInputOutputField inputField = meta.inputFields[i];
      if ( inputField.getPath() != null ) {
        item.setText( AVRO_PATH_COLUMN_INDEX, inputField.getPath() );
      }
      if ( inputField.getName() != null ) {
        item.setText( FIELD_NAME_COLUMN_INDEX, inputField.getName() );
      }
      item.setText( FIELD_TYPE_COLUMN_INDEX, inputField.getTypeDesc() );
    }
  }

  /**
   * Fill meta object from UI options.
   */
  @Override
  protected void getInfo( AvroInputMetaBase meta, boolean preview ) {
    String filePath = wPath.getText();
    if ( filePath != null && !filePath.isEmpty() ) {
      meta.allocateFiles( 1 );
      meta.inputFiles.fileName[0] = wPath.getText();
    }
    int nrFields = wInputFields.nrNonEmpty();
    meta.inputFields = new FormatInputOutputField[nrFields];
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = wInputFields.getNonEmpty( i );
      FormatInputOutputField field = new FormatInputOutputField();
      field.setPath( item.getText( AVRO_PATH_COLUMN_INDEX ) );
      field.setName( item.getText( FIELD_NAME_COLUMN_INDEX ) );
      field.setType( ValueMetaFactory.getIdForValueMeta( item.getText( FIELD_TYPE_COLUMN_INDEX ) ) );
      meta.inputFields[i] = field;
    }
  }

  @Override
  protected int getWidth() {
    return DIALOG_WIDTH;
  }

  @Override
  protected int getHeight() {
    return DIALOG_HEIGHT;
  }

  @Override
  protected String getStepTitle() {
    return BaseMessages.getString( PKG, "AvroInputDialog.Shell.Title" );
  }

  @Override
  protected Listener getPreview() {
    // TODO
    return event -> { };
  }
}
