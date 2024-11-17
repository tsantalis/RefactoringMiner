/*******************************************************************************
 * Pentaho Big Data
 * <p/>
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
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
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.big.data.api.cluster.NamedCluster;
import org.pentaho.big.data.api.cluster.NamedClusterService;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.steps.textfileoutput.TextFileField;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.runtime.test.RuntimeTester;
import org.pentaho.runtime.test.action.RuntimeTestActionService;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.*;

/**
 * Created by bryan on 11/23/15.
 */
public class HadoopFileOutputMetaTest {

  public static final String TEST_CLUSTER_NAME = "TEST-CLUSTER-NAME";
  public static final String SAMPLE_HADOOP_FILE_OUTPUT_STEP = "sample-hadoop-file-output-step.xml";
  public static final String ENTRY_TAG_NAME = "entry";
  private static Logger logger = Logger.getLogger( HadoopFileOutputMetaTest.class );
  // for message resolution
  private static Class<?> MessagePKG = HadoopFileOutputMeta.class;
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
   * Tests HadoopFileOutputMeta methods: 1. isFileAsCommand returns false 2. setFileAsCommand is not supported
   */
  @Test
  public void testFileAsCommandOption() {

    HadoopFileOutputMeta hadoopFileOutputMeta = new HadoopFileOutputMeta( namedClusterService, runtimeTestActionService,
      runtimeTester );

    // we expect isFileAsCommand to be false
    assertFalse( hadoopFileOutputMeta.isFileAsCommand() );

    // we expect setFileAsCommand(true or false) to be unsupported
    try {
      hadoopFileOutputMeta.setFileAsCommand( true );
    } catch ( Exception e ) {
      // the expected message is "class name":" message from the package that HadoopFileOutputMeta is in
      String expectedMessage =
        e.getClass().getName() + ": "
          + BaseMessages.getString( MessagePKG, "HadoopFileOutput.MethodNotSupportedException.Message" );
      assertTrue( e.getMessage().equals( expectedMessage ) );
    }
  }

  /**
   * BACKLOG-7972 - Hadoop File Output: Hadoop Clusters dropdown doesn't preserve selected cluster after reopen a
   * transformation after changing signature of loadSource in , saveSource in HadoopFileOutputMeta wasn't called
   *
   * @throws Exception
   */
  @Test
  public void testSaveSourceCalledFromGetXml() throws Exception {
    HadoopFileOutputMeta hadoopFileOutputMeta = new HadoopFileOutputMeta( namedClusterService, runtimeTestActionService,
      runtimeTester );
    hadoopFileOutputMeta.setSourceConfigurationName( TEST_CLUSTER_NAME );
    //set required data for step - empty
    hadoopFileOutputMeta.setOutputFields( new TextFileField[] {} );
    //create spy to check whether saveSource now is called
    HadoopFileOutputMeta spy = Mockito.spy( hadoopFileOutputMeta );
    //getting from structure file node
    Document hadoopOutputMetaStep = getDocumentFromString( spy.getXML(), new SAXBuilder() );
    Element fileElement = getChildElementByTagName( hadoopOutputMetaStep.getRootElement(), "file" );
    //getting from file node cluster attribute value
    Element clusterNameElement = getChildElementByTagName( fileElement, HadoopFileInputMeta.SOURCE_CONFIGURATION_NAME );
    assertEquals( TEST_CLUSTER_NAME, clusterNameElement.getValue() );
    //check that saveSource is called from TextFileOutputMeta
    verify( spy, times( 1 ) ).saveSource( any( StringBuilder.class ), any( String.class ) );
  }

  public Node getChildElementByTagName( String fileName ) throws Exception {
    URL resource = getClass().getClassLoader().getResource( fileName );
    if ( resource == null ) {
      logger.error( "no file " + fileName + " found in resources" );
      throw new IllegalArgumentException( "no file " + fileName + " found in resources" );
    } else {
      return XMLHandler.getSubNode( XMLHandler.loadXMLFile( resource ), "entry" );
    }
  }

  public static Element getChildElementByTagName( Element element, String tagName ) throws Exception {
    return (Element) element.getContent( new ElementFilter( tagName ) ).get( 0 );
  }

  @Test
  public void testLoadSourceCalledFromReadData() throws Exception {
    HadoopFileOutputMeta hadoopFileOutputMeta = new HadoopFileOutputMeta( namedClusterService, runtimeTestActionService,
      runtimeTester );
    hadoopFileOutputMeta.setSourceConfigurationName( TEST_CLUSTER_NAME );
    //set required data for step - empty
    hadoopFileOutputMeta.setOutputFields( new TextFileField[] {} );
    HadoopFileOutputMeta spy = Mockito.spy( hadoopFileOutputMeta );
    Node node = getChildElementByTagName( SAMPLE_HADOOP_FILE_OUTPUT_STEP );
    //create spy to check whether saveSource now is called from readData
    spy.readData( node );
    assertEquals( TEST_CLUSTER_NAME, hadoopFileOutputMeta.getSourceConfigurationName() );
    verify( spy, times( 1 ) ).loadSource( any( Node.class ), any( IMetaStore.class ) );
  }

  @Test
  public void testLoadSourceRepForUrlRefresh() throws Exception {
    final String URL_FROM_CLUSTER = "urlFromCluster";
    Repository mockRep = mock( Repository.class );
    when( mockRep.getStepAttributeString( anyObject(), eq( "source_configuration_name" ) ) ).thenReturn(
        TEST_CLUSTER_NAME );
    HadoopFileOutputMeta hadoopFileOutputMeta =
        new HadoopFileOutputMeta( namedClusterService, runtimeTestActionService, runtimeTester );
    when( mockRep.getStepAttributeString( anyObject(), eq( "file_name" ) ) ).thenReturn( URL_FROM_CLUSTER );

    assertEquals( URL_FROM_CLUSTER, hadoopFileOutputMeta.loadSourceRep( mockRep, null, null ) );
  }

  public static Document getDocumentFromString( String xmlStep, SAXBuilder jdomBuilder )
    throws JDOMException, IOException {
    String xml = XMLHandler.openTag( ENTRY_TAG_NAME ) + xmlStep + XMLHandler.closeTag( ENTRY_TAG_NAME );
    return jdomBuilder.build( new ByteArrayInputStream( xml.getBytes() ) );
  }
}
