/*******************************************************************************
 * Pentaho Big Data
 * <p/>
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 * <p/>
 * ******************************************************************************
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/

package org.pentaho.big.data.kettle.plugins.hdfs.trans;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.big.data.api.cluster.NamedCluster;
import org.pentaho.big.data.api.cluster.NamedClusterService;
import org.pentaho.di.core.fileinput.FileInputList;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.file.BaseFileField;
import org.pentaho.di.trans.steps.fileinput.text.TextFileFilter;
import org.pentaho.di.trans.steps.named.cluster.NamedClusterEmbedManager;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.runtime.test.RuntimeTester;
import org.pentaho.runtime.test.action.RuntimeTestActionService;
import org.w3c.dom.Node;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyInt;

/**
 * @author Vasilina Terehova
 */
public class HadoopFileInputMetaTest {

  public static final String TEST_CLUSTER_NAME = "TEST-CLUSTER-NAME";
  public static final String SAMPLE_HADOOP_FILE_INPUT_STEP = "sample-hadoop-file-input-step.xml";
  public static final String TEST_FILE_NAME = "test-file-name";
  public static final String TEST_FOLDER_NAME = "test-folder-name";
  private static Logger logger = Logger.getLogger( HadoopFileInputMetaTest.class );
  // for message resolution
  private NamedClusterService namedClusterService;
  private RuntimeTestActionService runtimeTestActionService;
  private RuntimeTester runtimeTester;

  @Before
  public void setUp() throws Exception {
    namedClusterService = mock( NamedClusterService.class );
    runtimeTestActionService = mock( RuntimeTestActionService.class );
    runtimeTester = mock( RuntimeTester.class );
  }

  /**
   * BACKLOG-7972 - Hadoop File Output: Hadoop Clusters dropdown doesn't preserve selected cluster after reopen a
   * transformation after changing signature of loadSource in , saveSource in HadoopFileOutputMeta wasn't called
   *
   * @throws Exception
   */
  @Test
  public void testSaveSourceCalledFromGetXml() throws Exception {
    HadoopFileInputMeta hadoopFileInputMeta = new HadoopFileInputMeta( namedClusterService, runtimeTestActionService,
      runtimeTester );
    hadoopFileInputMeta.allocateFiles( 1 );
    //create spy to check whether saveSource now is called
    HadoopFileInputMeta spy = initHadoopMetaInput( hadoopFileInputMeta );
    HashMap<String, String> mappings = new HashMap<>();
    mappings.put( TEST_FILE_NAME, HadoopFileOutputMetaTest.TEST_CLUSTER_NAME );
    spy.setNamedClusterURLMapping( mappings );
    StepMeta parentStepMeta = mock( StepMeta.class );
    TransMeta parentTransMeta = mock( TransMeta.class );
    when( parentStepMeta.getParentTransMeta() ).thenReturn( parentTransMeta );
    NamedClusterEmbedManager embedManager = mock( NamedClusterEmbedManager.class );
    when( parentTransMeta.getNamedClusterEmbedManager() ).thenReturn( embedManager );
    spy.setParentStepMeta( parentStepMeta );
    String xml = spy.getXML();
    Document hadoopOutputMetaStep = HadoopFileOutputMetaTest.getDocumentFromString( xml, new SAXBuilder() );
    Element fileElement = HadoopFileOutputMetaTest.getChildElementByTagName( hadoopOutputMetaStep.getRootElement(), "file" );
    //getting from file node cluster attribute value
    Element clusterNameElement =
      HadoopFileOutputMetaTest.getChildElementByTagName( fileElement, HadoopFileInputMeta.SOURCE_CONFIGURATION_NAME );
    assertEquals( TEST_CLUSTER_NAME, clusterNameElement.getValue() );
    //check that saveSource is called from TextFileOutputMeta
    verify( spy, times( 1 ) ).saveSource( any( StringBuilder.class ), any( String.class ) );
    verify( embedManager ).registerUrl( "test-file-name" );
  }

  private HadoopFileInputMeta initHadoopMetaInput( HadoopFileInputMeta hadoopFileInputMeta ) {
    HadoopFileInputMeta spy = Mockito.spy( hadoopFileInputMeta );
    when( spy.getFileName() ).thenReturn( new String[] {} );
    spy.setFileName( new String[] { TEST_FILE_NAME } );
    spy.setFilter( new TextFileFilter[] {} );
    spy.inputFields = new BaseFileField[] {};
    spy.inputFiles.fileMask = new String[] { TEST_FILE_NAME };
    spy.inputFiles.fileRequired = new String[] { TEST_FILE_NAME };
    spy.inputFiles.includeSubFolders = new String[] { TEST_FOLDER_NAME };
    spy.content.dateFormatLocale = Locale.getDefault();
    return spy;
  }

  public Node loadNodeFromXml( String fileName ) throws Exception {
    URL resource = getClass().getClassLoader().getResource( fileName );
    if ( resource == null ) {
      logger.error( "no file " + fileName + " found in resources" );
      throw new IllegalArgumentException( "no file " + fileName + " found in resources" );
    } else {
      return XMLHandler.getSubNode( XMLHandler.loadXMLFile( resource ), "entry" );
    }
  }

  @Test
  public void testLoadSourceCalledFromLoadXml() throws Exception {
    HadoopFileInputMeta hadoopFileInputMeta = new HadoopFileInputMeta( namedClusterService, runtimeTestActionService,
      runtimeTester );
    //set required data for step - empty
    HadoopFileInputMeta spy = Mockito.spy( hadoopFileInputMeta );
    Node node = loadNodeFromXml( SAMPLE_HADOOP_FILE_INPUT_STEP );
    //create spy to check whether saveSource now is called
    IMetaStore metaStore = mock( IMetaStore.class );
    spy.loadXML( node, Collections.emptyList(), metaStore );
    assertEquals( TEST_CLUSTER_NAME, hadoopFileInputMeta.getNamedClusterURLMapping().get( TEST_FILE_NAME ) );
    verify( spy, times( 1 ) ).loadSource( any( Node.class ), any( Node.class ), anyInt(), any( IMetaStore.class ) );
  }

  @Test
  public void testLoadSourceRepForUrlRefresh() throws Exception {
    final String URL_FROM_CLUSTER = "urlFromCluster";
    IMetaStore mockMetaStore = mock( IMetaStore.class );
    NamedCluster mockNamedCluster = mock( NamedCluster.class );
    when( mockNamedCluster.processURLsubstitution( any(), eq( mockMetaStore ), any() ) ).thenReturn( URL_FROM_CLUSTER );
    when( namedClusterService.getNamedClusterByName( TEST_CLUSTER_NAME, mockMetaStore ) ).thenReturn( mockNamedCluster );
    Repository mockRep = mock( Repository.class );
    when( mockRep.getJobEntryAttributeString( anyObject(), eq( 0 ), eq( "source_configuration_name" ) ) ).thenReturn( TEST_CLUSTER_NAME );
    HadoopFileInputMeta hadoopFileInputMeta =  new HadoopFileInputMeta( namedClusterService, runtimeTestActionService, runtimeTester );
    when( mockRep.getStepAttributeString( anyObject(), eq( 0 ), eq( "file_name" ) ) ).thenReturn( URL_FROM_CLUSTER );

    assertEquals( URL_FROM_CLUSTER, hadoopFileInputMeta.loadSourceRep( mockRep, null, 0, mockMetaStore ) );
  }

  @Test
  public void testGetFileInputList() {
    final String URL_FROM_CLUSTER = "urlFromCluster";
    StepMeta parentStepMeta = mock( StepMeta.class );
    IMetaStore mockMetaStore = mock( IMetaStore.class );
    NamedCluster mockNamedCluster = mock( NamedCluster.class );
    TransMeta parentTransMeta = mock( TransMeta.class );
    when( parentStepMeta.getParentTransMeta() ).thenReturn( parentTransMeta );
    when( parentTransMeta.getMetaStore() ).thenReturn( mockMetaStore );
    when( mockNamedCluster.processURLsubstitution( any(), eq( mockMetaStore ), any() ) ).thenReturn( URL_FROM_CLUSTER );
    when( namedClusterService.getNamedClusterByName( TEST_CLUSTER_NAME, mockMetaStore ) ).thenReturn( mockNamedCluster );
    HadoopFileInputMeta hadoopFileInputMetaSpy =  initHadoopMetaInput( new HadoopFileInputMeta( namedClusterService, runtimeTestActionService, runtimeTester ) );
    hadoopFileInputMetaSpy.environment = new String[] { TEST_CLUSTER_NAME };
    hadoopFileInputMetaSpy.setParentStepMeta( parentStepMeta );
    doReturn( new FileInputList() ).when( hadoopFileInputMetaSpy ).createFileList( any( VariableSpace.class ) );
    hadoopFileInputMetaSpy.getFileInputList( new Variables() );

    assertEquals( "urlFromCluster", hadoopFileInputMetaSpy.inputFiles.fileName[0] );
  }

}
