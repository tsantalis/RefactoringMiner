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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.pentaho.big.data.kettle.plugins.hdfs.trans.HadoopFileMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.steps.file.BaseFileMeta;
import org.pentaho.dictionary.DictionaryConst;
import org.pentaho.metaverse.api.IComponentDescriptor;
import org.pentaho.metaverse.api.IMetaverseNode;
import org.pentaho.metaverse.api.INamespace;
import org.pentaho.metaverse.api.MetaverseComponentDescriptor;
import org.pentaho.metaverse.api.MetaverseObjectFactory;
import org.pentaho.metaverse.api.model.IExternalResourceInfo;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class HadoopBaseStepAnalyzerTest<A extends HadoopBaseStepAnalyzer, M extends BaseFileMeta> {

  protected A analyzer;

  @Mock private INamespace mockNamespace;
  private IComponentDescriptor descriptor;

  @Before
  public void setUp() throws Exception {
    when( mockNamespace.getParentNamespace() ).thenReturn( mockNamespace );
    descriptor = new MetaverseComponentDescriptor( "test", DictionaryConst.NODE_TYPE_TRANS_STEP, mockNamespace );
    analyzer = spy( getAnalyzer() );
    analyzer.setDescriptor( descriptor );
    when( analyzer.getMetaverseObjectFactory() ).thenReturn( new MetaverseObjectFactory() );
  }

  protected abstract A getAnalyzer();

  protected abstract M getMetaMock();

  @Test
  public void testGetUsedFields() throws Exception {
    assertNull( analyzer.getUsedFields( getMetaMock() ) );
  }

  @Test
  public void testGetResourceInputNodeType() throws Exception {
    assertEquals( DictionaryConst.NODE_TYPE_FILE_FIELD, analyzer.getResourceInputNodeType() );
  }

  @Test
  public void testGetResourceOutputNodeType() throws Exception {
    assertEquals( DictionaryConst.NODE_TYPE_HDFS_FIELD, analyzer.getResourceOutputNodeType() );
  }

  @Test
  public void testGetSupportedSteps() {
    Set<Class<? extends BaseStepMeta>> types = analyzer.getSupportedSteps();
    assertNotNull( types );
    assertEquals( types.size(), 1 );
    assertTrue( types.contains( getMetaClass() ) );
  }

  protected abstract Class<M> getMetaClass();

  @Test
  public void testCreateResourceNode() throws Exception {
    // local
    IExternalResourceInfo localResource = mock( IExternalResourceInfo.class );
    when( localResource.getName() ).thenReturn( "file:///Users/home/tmp/xyz.ktr" );
    IMetaverseNode resourceNode = analyzer.createResourceNode( getMetaMock(), localResource );
    assertNotNull( resourceNode );
    assertEquals( DictionaryConst.NODE_TYPE_FILE, resourceNode.getType() );

    // remote
    final HadoopFileMeta hMeta = (HadoopFileMeta) getMetaMock();
    IExternalResourceInfo remoteResource = mock( IExternalResourceInfo.class );
    final String hostName = "foo.com";
    final String filePath = "hdfs://" + hostName + "/file.csv";
    when( remoteResource.getName() ).thenReturn( filePath );
    when( hMeta.getUrlHostName( filePath ) ).thenReturn( hostName );
    resourceNode = analyzer.createResourceNode( getMetaMock(), remoteResource );
    assertNotNull( resourceNode );
    assertEquals( DictionaryConst.NODE_TYPE_HDFS_FILE, resourceNode.getType() );
  }
}
