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

import org.pentaho.big.data.api.cluster.NamedCluster;
import org.pentaho.big.data.api.cluster.NamedClusterService;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.steps.textfileoutput.TextFileOutputMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.runtime.test.RuntimeTester;
import org.pentaho.runtime.test.action.RuntimeTestActionService;
import org.w3c.dom.Node;

@Step( id = "HadoopFileOutputPlugin", image = "HDO.svg", name = "HadoopFileOutputPlugin.Name",
    description = "HadoopFileOutputPlugin.Description",
    documentationUrl = "http://wiki.pentaho.com/display/EAI/Hadoop+File+Output",
    categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.BigData",
    i18nPackageName = "org.pentaho.di.trans.steps.hadoopfileoutput" )
@InjectionSupported( localizationPrefix = "HadoopFileOutput.Injection.", groups = { "OUTPUT_FIELDS" } )
public class HadoopFileOutputMeta extends TextFileOutputMeta {

  // for message resolution
  private static Class<?> PKG = HadoopFileOutputMeta.class;

  private String sourceConfigurationName;

  private static final String SOURCE_CONFIGURATION_NAME = "source_configuration_name";

  private final NamedClusterService namedClusterService;
  private final RuntimeTestActionService runtimeTestActionService;
  private final RuntimeTester runtimeTester;

  public HadoopFileOutputMeta( NamedClusterService namedClusterService,
                               RuntimeTestActionService runtimeTestActionService, RuntimeTester runtimeTester ) {
    this.namedClusterService = namedClusterService;
    this.runtimeTestActionService = runtimeTestActionService;
    this.runtimeTester = runtimeTester;
  }

  @Override
  public void setDefault() {
    // call the base classes method
    super.setDefault();

    // now set the default for the
    // filename to an empty string
    setFileName( "" );
    super.setFileAsCommand( false );
  }

  @Override
  public void setFileAsCommand( boolean fileAsCommand ) {
    // Don't do anything. We want to keep this property as false
    // Throwing a KettleStepException would be desirable but then we
    // need to change the base class' method which is
    // open source.

    throw new RuntimeException( new RuntimeException( BaseMessages.getString( PKG,
        "HadoopFileOutput.MethodNotSupportedException.Message" ) ) );
  }

  public String getSourceConfigurationName() {
    return sourceConfigurationName;
  }

  public void setSourceConfigurationName( String ncName ) {
    this.sourceConfigurationName = ncName;
  }

  protected String loadSource( Node stepnode, IMetaStore metastore ) {
    String url = XMLHandler.getTagValue( stepnode, "file", "name" );
    sourceConfigurationName = XMLHandler.getTagValue( stepnode, "file", SOURCE_CONFIGURATION_NAME );

    return getProcessedUrl( metastore, url );
  }

  protected String getProcessedUrl( IMetaStore metastore, String url ) {
    if ( url == null ) {
      return null;
    }
    NamedCluster c = namedClusterService.getNamedClusterByName( sourceConfigurationName, metastore );
    if ( c != null ) {
      url = c.processURLsubstitution( url, metastore, new Variables() );
    }
    return url;
  }

  protected void saveSource( StringBuilder retVal, String fileName ) {
    retVal.append( "      " ).append( XMLHandler.addTagValue( "name", fileName ) );
    retVal.append( "      " ).append( XMLHandler.addTagValue( SOURCE_CONFIGURATION_NAME, sourceConfigurationName ) );
  }

  // Receiving metaStore because RepositoryProxy.getMetaStore() returns a hard-coded null
  protected String loadSourceRep( Repository rep, ObjectId id_step,  IMetaStore metaStore ) throws KettleException {
    String url = rep.getStepAttributeString( id_step, "file_name" );
    sourceConfigurationName = rep.getStepAttributeString( id_step, SOURCE_CONFIGURATION_NAME );

    return getProcessedUrl( metaStore, url );
  }

  protected void saveSourceRep( Repository rep, ObjectId id_transformation, ObjectId id_step, String fileName )
    throws KettleException {
    rep.saveStepAttribute( id_transformation, id_step, "file_name", fileName );
    rep.saveStepAttribute( id_transformation, id_step, SOURCE_CONFIGURATION_NAME, sourceConfigurationName );
  }

  public NamedClusterService getNamedClusterService() {
    return namedClusterService;
  }

  public RuntimeTester getRuntimeTester() {
    return runtimeTester;
  }

  public RuntimeTestActionService getRuntimeTestActionService() {
    return runtimeTestActionService;
  }
}
