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
package org.pentaho.big.data.kettle.plugins.formats.impl.avro.input;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.big.data.kettle.plugins.formats.avro.input.AvroInputField;
import org.pentaho.big.data.kettle.plugins.formats.impl.avro.BaseAvroStepDialog;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.hadoop.shim.api.format.AvroSpec;
import org.pentaho.hadoop.shim.api.format.IAvroInputField;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.vfs.ui.VfsFileChooserDialog;

public class AvroInputDialog extends BaseAvroStepDialog<AvroInputMeta> {

  private static final int SHELL_WIDTH = 698;
  private static final int SHELL_HEIGHT = 554;

  private static final int AVRO_PATH_COLUMN_INDEX = 1;

  private static final int FIELD_NAME_COLUMN_INDEX = 2;

  private static final int FIELD_TYPE_COLUMN_INDEX = 3;

  private static final int FORMAT_COLUMN_INDEX = 4;

  private static final String SCHEMA_SCHEME_DEFAULT = "hdfs";

  private TableView wInputFields;
  protected TextVar wSchemaPath;
  protected Button wbSchemaBrowse;

  private Button wPassThruFields;

  public AvroInputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (AvroInputMeta) in, transMeta, sname );
  }

  protected Control createAfterFile( Composite afterFile ) {
    CTabFolder wTabFolder = new CTabFolder( afterFile, SWT.BORDER );
    props.setLook( wTabFolder, Props.WIDGET_STYLE_TAB );
    wTabFolder.setSimple( false );

    addFileTab( wTabFolder );
    addSchemaTab( wTabFolder );
    addFieldsTab( wTabFolder );

    new FD( wTabFolder ).left( 0, 0 ).top( 0, MARGIN ).right( 100, 0 ).bottom( 100, 0 ).apply();
    wTabFolder.setSelection( 0 );

    return wTabFolder;
  }

  /**
   * this method must be changed only with change {@link #extractAvroType(String)}
   * since this method converts the field for show user and the extract methods myst convert to internal format
   */
  private String concatenateAvroNameAndType( String avroFieldName, AvroSpec.DataType avroType ) {
    return avroFieldName + " (" + avroType.getName() + ")";
  }

  /**
   * this method must be changed only with change
   * 
   * @see #concatenateAvroNameAndType(String, org.pentaho.hadoop.shim.api.format.AvroSpec.DataType)
   */
  private AvroSpec.DataType extractAvroType( String avroNameTypeFromUI ) {
    if ( avroNameTypeFromUI != null ) {
      String uiType = StringUtils.substringBetween( avroNameTypeFromUI, "(", ")" );
      if ( uiType != null ) {
        String uiTypeTrimmed = uiType.trim();
        for ( AvroSpec.DataType temp : AvroSpec.DataType.values() ) {
          if ( temp.getName().equalsIgnoreCase( uiTypeTrimmed ) ) {
            return temp;
          }
        }
      }
    }
    return null;
  }

  protected void populateFieldsTable() {
    // this schema overrides any that might be in a container file
    String schemaFileName = wSchemaPath.getText();
    schemaFileName = transMeta.environmentSubstitute( schemaFileName );

    String avroFileName = wPath.getText();
    avroFileName = transMeta.environmentSubstitute( avroFileName );
    try {
      List<? extends IAvroInputField> defaultFields =
          AvroInput.getDefaultFields( meta.getNamedClusterServiceLocator(), meta.getNamedCluster(), schemaFileName,
              avroFileName );
      wInputFields.clearAll();
      for ( IAvroInputField field : defaultFields ) {
        TableItem item = new TableItem( wInputFields.table, SWT.NONE );
        if ( field != null ) {
          setField( item, concatenateAvroNameAndType( field.getDisplayableAvroFieldName(), field.getAvroType() ), AVRO_PATH_COLUMN_INDEX );
          setField( item, field.getPentahoFieldName(), FIELD_NAME_COLUMN_INDEX );
          setField( item, ValueMetaFactory.getValueMetaName( field.getPentahoType() ), FIELD_TYPE_COLUMN_INDEX );
          setField( item, field.getStringFormat(), FORMAT_COLUMN_INDEX );
        }
      }

      wInputFields.removeEmptyRows();
      wInputFields.setRowNums();
      wInputFields.optWidth( true );
    } catch ( Exception ex ) {
      logError( BaseMessages.getString( PKG, "AvroInput.Error.UnableToLoadSchemaFromContainerFile" ), ex );
      new ErrorDialog( shell, stepname, BaseMessages.getString( PKG,
        "AvroInput.Error.UnableToLoadSchemaFromContainerFile", avroFileName ), ex );
    }
  }

  private void setField( TableItem item, String fieldValue, int fieldIndex ) {
    if ( !Utils.isEmpty( fieldValue ) ) {
      item.setText( fieldIndex, fieldValue );
    }
  }

  private void addFieldsTab( CTabFolder wTabFolder ) {
    CTabItem wTab = new CTabItem( wTabFolder, SWT.NONE );
    wTab.setText( BaseMessages.getString( PKG, "AvroInputDialog.FieldsTab.TabTitle" ) );

    Composite wComp = new Composite( wTabFolder, SWT.NONE );
    props.setLook( wComp );

    FormLayout layout = new FormLayout();
    layout.marginWidth = MARGIN;
    layout.marginHeight = MARGIN;
    wComp.setLayout( layout );

    // Accept fields from previous steps?
    //
    wPassThruFields = new Button( wComp, SWT.CHECK );
    wPassThruFields.setText( BaseMessages.getString( PKG, "AvroInputDialog.PassThruFields.Label" ) );
    wPassThruFields.setToolTipText( BaseMessages.getString( PKG, "AvroInputDialog.PassThruFields.Tooltip" ) );
    wPassThruFields.setOrientation( SWT.LEFT_TO_RIGHT );
    props.setLook( wPassThruFields );
    new FD( wPassThruFields ).left( 0, 0 ).top( wComp, 0 ).apply();

    //get fields button
    lsGet = new Listener() {
      public void handleEvent( Event e ) {
        populateFieldsTable();
      }
    };
    Button wGetFields = new Button( wComp, SWT.PUSH );
    wGetFields.setText( BaseMessages.getString( PKG, "AvroInputDialog.Fields.Get" ) );
    props.setLook( wGetFields );
    new FD( wGetFields ).bottom( 100, 0 ).right( 100, 0 ).apply();
    wGetFields.addListener( SWT.Selection, lsGet );

    // fields table
    ColumnInfo avroPathColumnInfo = new ColumnInfo( BaseMessages.getString( PKG, "AvroInputDialog.Fields.column.Path" ), ColumnInfo.COLUMN_TYPE_TEXT, false, true );
    ColumnInfo nameColumnInfo = new ColumnInfo( BaseMessages.getString( PKG, "AvroInputDialog.Fields.column.Name" ), ColumnInfo.COLUMN_TYPE_TEXT, false, false );
    ColumnInfo typeColumnInfo = new ColumnInfo( BaseMessages.getString( PKG, "AvroInputDialog.Fields.column.Type" ), ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaFactory.getValueMetaNames() );
    ColumnInfo formatColumnInfo = new ColumnInfo( BaseMessages.getString( PKG, "AvroInputDialog.Fields.column.Format" ), ColumnInfo.COLUMN_TYPE_CCOMBO, Const.getDateFormats() );

    ColumnInfo[] parameterColumns = new ColumnInfo[] { avroPathColumnInfo, nameColumnInfo, typeColumnInfo, formatColumnInfo };
    parameterColumns[0].setAutoResize( false );
    parameterColumns[1].setUsingVariables( true );
    parameterColumns[3].setAutoResize( false );

    wInputFields = new TableView( transMeta, wComp, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.NO_SCROLL | SWT.V_SCROLL, parameterColumns, 7, null, props );
    ColumnsResizer resizer = new ColumnsResizer( 0, 40, 20, 20, 20 );
    wInputFields.getTable().addListener( SWT.Resize, resizer );

    props.setLook( wInputFields );
    new FD( wInputFields ).left( 0, 0 ).right( 100, 0 ).top( wPassThruFields, FIELDS_SEP ).bottom( wGetFields, -FIELDS_SEP ).apply();

    wInputFields.setRowNums();
    wInputFields.optWidth( true );

    new FD( wComp ).left( 0, 0 ).top( 0, 0 ).right( 100, 0 ).bottom( 100, 0 ).apply();

    wTab.setControl( wComp );
    for ( ColumnInfo col : parameterColumns ) {
      col.setAutoResize( false );
    }
    resizer.addColumnResizeListeners( wInputFields.getTable() );
    setTruncatedColumn( wInputFields.getTable(), 1 );
    if ( !Const.isWindows() ) {
      addColumnTooltip( wInputFields.getTable(), 1 );
    }
  }

  private void addSchemaTab( CTabFolder wTabFolder ) {
    // Create & Set up a new Tab Item
    CTabItem wTab = new CTabItem( wTabFolder, SWT.NONE );
    wTab.setText( BaseMessages.getString( PKG, "AvroInputDialog.Schema.TabTitle" ) );
    Composite wTabComposite = new Composite( wTabFolder, SWT.NONE );
    wTab.setControl( wTabComposite );
    props.setLook( wTabComposite );
    FormLayout formLayout = new FormLayout();
    formLayout.marginHeight = MARGIN;
    wTabComposite.setLayout( formLayout );

    // Set up the Source Group
    Group wSourceGroup = new Group( wTabComposite, SWT.SHADOW_NONE );
    props.setLook( wSourceGroup );
    wSourceGroup.setText( BaseMessages.getString( PKG, "AvroInputDialog.Schema.SourceTitle" ) );

    FormLayout layout = new FormLayout();
    layout.marginWidth = MARGIN;
    wSourceGroup.setLayout( layout );

    FormData fdSource = new FormData();
    fdSource.top = new FormAttachment( 0, 0 );
    fdSource.right = new FormAttachment( 100, -15 );
    fdSource.left = new FormAttachment( 0, 15 );

    wSourceGroup.setLayoutData( fdSource );

    Label wlSchemaPath = new Label( wSourceGroup, SWT.RIGHT );
    wlSchemaPath.setText( BaseMessages.getString( PKG, "AvroInputDialog.Schema.FileName" ) );
    props.setLook( wlSchemaPath );
    new FD( wlSchemaPath ).left( 0, 0 ).top( 0, 0 ).apply();
    wSchemaPath = new TextVar( transMeta, wSourceGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wSchemaPath );
    new FD( wSchemaPath ).left( 0, 0 ).top( wlSchemaPath, FIELD_LABEL_SEP ).width( FIELD_LARGE + VAR_EXTRA_WIDTH )
      .rright().apply();

    wbSchemaBrowse = new Button( wSourceGroup, SWT.PUSH );
    props.setLook( wbSchemaBrowse );
    wbSchemaBrowse.setText( getMsg( "System.Button.Browse" ) );
    wbSchemaBrowse.addListener( SWT.Selection, event -> browseForFileInputPathForSchema() );
    int bOffset =
        ( wbSchemaBrowse.computeSize( SWT.DEFAULT, SWT.DEFAULT, false ).y - wSchemaPath.computeSize( SWT.DEFAULT,
            SWT.DEFAULT, false ).y ) / 2;
    new FD( wbSchemaBrowse ).left( wSchemaPath, FIELD_LABEL_SEP ).top( wlSchemaPath, FIELD_LABEL_SEP - bOffset )
      .apply();

    layout = new FormLayout();
    layout.marginWidth = MARGIN;
    layout.marginHeight = MARGIN;
    wSourceGroup.setLayout( layout );
  }

  private void browseForFileInputPathForSchema() {
    try {
      String path = transMeta.environmentSubstitute( wSchemaPath.getText() );
      VfsFileChooserDialog fileChooserDialog;
      String fileName;
      if ( Utils.isEmpty( path ) ) {
        fileChooserDialog = getVfsFileChooserDialog( null, null );
        fileName = SCHEMA_SCHEME_DEFAULT + "://";
      } else {
        FileObject initialFile = getInitialFile( wSchemaPath.getText() );
        FileObject rootFile = initialFile.getFileSystem().getRoot();
        fileChooserDialog = getVfsFileChooserDialog( rootFile, initialFile );
        fileName = null;
      }

      FileObject selectedFile =
          fileChooserDialog.open( shell, null, getSchemeFromPath( path ), true, fileName, FILES_FILTERS,
              fileFilterNames, true, VfsFileChooserDialog.VFS_DIALOG_OPEN_FILE_OR_DIRECTORY, true, true );
      if ( selectedFile != null ) {
        wSchemaPath.setText( selectedFile.getURL().toString() );
      }
    } catch ( KettleFileException ex ) {
      log.logError( getBaseMsg( "AvroInputDialog.SchemaFileBrowser.KettleFileException" ) );
    } catch ( FileSystemException ex ) {
      log.logError( getBaseMsg( "AvroInputDialog.SchemaFileBrowser.FileSystemException" ) );
    }
  }

  /**
   * Read the data from the meta object and show it in this dialog.
   */
  @Override
  protected void getData( AvroInputMeta meta ) {
    if ( meta.getFilename() != null ) {
      wPath.setText( meta.getFilename() );
    }
    if ( meta.getSchemaFilename() != null ) {
      wSchemaPath.setText( meta.getSchemaFilename() );
    }
    wPassThruFields.setSelection( meta.inputFiles.passingThruFields );
    int itemIndex = 0;
    for ( AvroInputField inputField : meta.getInputFields() ) {
      TableItem item = null;
      if ( itemIndex < wInputFields.table.getItemCount() ) {
        item = wInputFields.table.getItem( itemIndex );
      } else {
        item = new TableItem( wInputFields.table, SWT.NONE );
      }

      if ( inputField.getAvroFieldName() != null ) {
        if ( inputField.getAvroType() != null ) {
          item.setText( AVRO_PATH_COLUMN_INDEX,
              concatenateAvroNameAndType( inputField.getDisplayableAvroFieldName(), inputField.getAvroType() ) );
        } else {
          item.setText( AVRO_PATH_COLUMN_INDEX, inputField.getDisplayableAvroFieldName() );
        }
      }
      if ( inputField.getPentahoFieldName() != null ) {
        item.setText( FIELD_NAME_COLUMN_INDEX, inputField.getPentahoFieldName() );
      }
      if ( inputField.getTypeDesc() != null ) {
        item.setText( FIELD_TYPE_COLUMN_INDEX, inputField.getTypeDesc() );
      }
      if ( inputField.getStringFormat() != null ) {
        item.setText( FORMAT_COLUMN_INDEX, inputField.getStringFormat() );
      } else {
        item.setText( FORMAT_COLUMN_INDEX, "" );
      }
      itemIndex++;
    }
  }

  /**
   * Fill meta object from UI options.
   */
  @Override
  protected void getInfo( AvroInputMeta meta, boolean preview ) {
    meta.setFilename( wPath.getText() );
    meta.setSchemaFilename( wSchemaPath.getText() );
    meta.inputFiles.passingThruFields = wPassThruFields.getSelection();
    meta.setUseFieldAsInputStream( wbGetFileFromField.getSelection() );
    meta.setInputStreamFieldName( wFieldNameCombo.getText() );

    int nrFields = wInputFields.nrNonEmpty();
    meta.inputFields = new AvroInputField[ nrFields ];
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = wInputFields.getNonEmpty( i );
      AvroInputField field = new AvroInputField();
      field.setFormatFieldName( extractFieldName( item.getText( AVRO_PATH_COLUMN_INDEX ) ) );
      field.setAvroType( extractAvroType( item.getText( AVRO_PATH_COLUMN_INDEX ) ) );
      field.setPentahoFieldName( item.getText( FIELD_NAME_COLUMN_INDEX ) );
      field.setPentahoType( ValueMetaFactory.getIdForValueMeta( item.getText( FIELD_TYPE_COLUMN_INDEX ) ) );
      field.setStringFormat( item.getText( FORMAT_COLUMN_INDEX ) );
      meta.inputFields[ i ] = field;
    }
  }

  private String extractFieldName( String parquetNameTypeFromUI ) {
    if ( ( parquetNameTypeFromUI != null ) && ( parquetNameTypeFromUI.indexOf( "(" ) >= 0 ) ) {
      return StringUtils.substringBefore( parquetNameTypeFromUI, "(" ).trim();
    }
    return parquetNameTypeFromUI;
  }

  private String getSchemeFromPath( String path ) {
    if ( Utils.isEmpty( path ) ) {
      return SCHEMA_SCHEME_DEFAULT;
    }
    int endIndex = path.indexOf( ':' );
    if ( endIndex > 0 ) {
      return path.substring( 0, endIndex );
    } else {
      return SCHEMA_SCHEME_DEFAULT;
    }
  }

  private void doPreview() {
    getInfo( meta, true );
    TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation( transMeta, meta, wStepname.getText() );
    transMeta.getVariable( "Internal.Transformation.Filename.Directory" );
    previewMeta.getVariable( "Internal.Transformation.Filename.Directory" );

    EnterNumberDialog numberDialog =
      new EnterNumberDialog( shell, props.getDefaultPreviewSize(), BaseMessages.getString( PKG,
        "AvroInputDialog.PreviewSize.DialogTitle" ), BaseMessages.getString( PKG,
        "AvroInputDialog.PreviewSize.DialogMessage" ) );
    int previewSize = numberDialog.open();

    if ( previewSize > 0 ) {
      TransPreviewProgressDialog progressDialog =
          new TransPreviewProgressDialog( shell, previewMeta, new String[] { wStepname.getText() }, new int[] {
            previewSize } );
      progressDialog.open();

      Trans trans = progressDialog.getTrans();
      String loggingText = progressDialog.getLoggingText();

      if ( !progressDialog.isCancelled() ) {
        if ( trans.getResult() != null && trans.getResult().getNrErrors() > 0 ) {
          EnterTextDialog etd =
            new EnterTextDialog( shell, BaseMessages.getString( PKG, "System.Dialog.PreviewError.Title" ),
              BaseMessages.getString( PKG, "System.Dialog.PreviewError.Message" ), loggingText, true );
          etd.setReadOnly();
          etd.open();
        }
      }

      PreviewRowsDialog prd =
          new PreviewRowsDialog( shell, transMeta, SWT.NONE, wStepname.getText(), progressDialog.getPreviewRowsMeta(
              wStepname.getText() ), progressDialog.getPreviewRows( wStepname.getText() ), loggingText );
      prd.open();
    }
  }

  @Override
  protected int getWidth() {
    return SHELL_WIDTH;
  }

  @Override
  protected int getHeight() {
    return SHELL_HEIGHT;
  }

  @Override
  protected String getStepTitle() {
    return BaseMessages.getString( PKG, "AvroInputDialog.Shell.Title" );
  }

  @Override
  protected Listener getPreview() {
    return new Listener() {
      public void handleEvent( Event e ) {
        doPreview();
      }
    };
  }



}
