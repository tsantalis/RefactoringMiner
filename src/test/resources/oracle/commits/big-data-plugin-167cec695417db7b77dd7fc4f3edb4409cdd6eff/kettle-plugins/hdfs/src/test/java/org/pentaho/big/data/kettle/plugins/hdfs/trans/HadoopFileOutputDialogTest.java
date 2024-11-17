/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
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

package org.pentaho.big.data.kettle.plugins.hdfs.trans;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.big.data.api.cluster.NamedCluster;
import org.pentaho.metastore.api.IMetaStore;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import static org.junit.Assert.assertEquals;

/**
 * Created by bryan on 11/23/15.
 */
public class HadoopFileOutputDialogTest {

  NamedCluster cluster = null;
  IMetaStore metaStore = null;

  @Before
  public void setup() {
    cluster = mock( NamedCluster.class );
    metaStore = mock( IMetaStore.class );
  }

  @Test
  public void testGetNoVariableURLRoot() {
    String noVariablesHostname = "hdfs://test_hostname_no_variables:7878";
    when( cluster.processURLsubstitution( eq( "/" ), any(), any() ) ).thenReturn( noVariablesHostname );
    assertEquals( noVariablesHostname, HadoopFileOutputDialog.getRootURL( cluster, metaStore, true ) );
  }

  @Test
  public void testGetHdfsURLRootWithHostnameVariable() {
    when( cluster.getHdfsHost() ).thenReturn( "${VariableHostname}" );
    when( cluster.getHdfsPort() ).thenReturn( "7777" );
    when( cluster.getHdfsUsername() ).thenReturn( "admin" );
    when( cluster.getHdfsPassword() ).thenReturn( "password" );
    when( cluster.isMapr() ).thenReturn( false );
    assertEquals( "hdfs://admin:password@${VariableHostname}:7777", HadoopFileOutputDialog.getRootURL( cluster, metaStore, false ) );
  }

  @Test
  public void testGetMaprURLRootWithHostnameVariable() {
    when( cluster.getHdfsHost() ).thenReturn( "${VariableHostname}" );
    when( cluster.getHdfsPort() ).thenReturn( "7777" );
    when( cluster.isMapr() ).thenReturn( true );
    assertEquals( "maprfs://${VariableHostname}:7777", HadoopFileOutputDialog.getRootURL( cluster, metaStore, false ) );
  }

  @Test
  public void testGetRelativePathToHdfsUrlRootWithVariables() {
    when( cluster.getHdfsHost() ).thenReturn( "${VariableHostname}" );
    when( cluster.getHdfsPort() ).thenReturn( "7777" );
    when( cluster.getHdfsUsername() ).thenReturn( "admin" );
    when( cluster.getHdfsPassword() ).thenReturn( "password" );
    when( cluster.isMapr() ).thenReturn( false );
    String relativeUrl = "/some/path/to";
    assertEquals( relativeUrl, HadoopFileOutputDialog.getUrlPath( "hdfs://admin:password@${VariableHostname}:7777" + relativeUrl, cluster, metaStore, false ) );
  }

  @Test
  public void testGetRelativePathToMaprUrlRootWithVariables() {
    when( cluster.getHdfsHost() ).thenReturn( "${VariableHostname}" );
    when( cluster.getHdfsPort() ).thenReturn( "7777" );
    when( cluster.getHdfsUsername() ).thenReturn( "admin" );
    when( cluster.getHdfsPassword() ).thenReturn( "password" );
    when( cluster.isMapr() ).thenReturn( true );
    String relativeUrl = "/some/path/to";
    assertEquals( relativeUrl, HadoopFileOutputDialog.getUrlPath( "maprfs://admin:password@${VariableHostname}:7777" + relativeUrl, cluster, metaStore, false ) );
  }

  @Test
  public void testGetRelativePathToUrlRootWithoutVariables() {
    String noVariableUrl = "hdfs://admin:password@test_hostname_no_variables:7777";
    String relativeUrl = "/some/path/to";
    when( cluster.processURLsubstitution( eq( "/" ), any(), any() ) ).thenReturn( noVariableUrl );
    when( cluster.isMapr() ).thenReturn( false );
    assertEquals( relativeUrl, HadoopFileOutputDialog.getUrlPath( "hdfs://admin:password@test_hostname_no_variables:7777" + relativeUrl, cluster, metaStore, true ) );
  }

  @Test
  public void testGetRelativePathWithVariables() {
    when( cluster.getHdfsHost() ).thenReturn( "${VariableHostname}" );
    when( cluster.getHdfsPort() ).thenReturn( "7777" );
    when( cluster.getHdfsUsername() ).thenReturn( "admin" );
    when( cluster.getHdfsPassword() ).thenReturn( "password" );
    when( cluster.isMapr() ).thenReturn( true );
    String relativeUrl = "/${VariableTestPath}";
    assertEquals( relativeUrl, HadoopFileOutputDialog.getUrlPath( "maprfs://admin:password@${VariableHostname}:7777" + relativeUrl, cluster, metaStore, false ) );
  }

}
