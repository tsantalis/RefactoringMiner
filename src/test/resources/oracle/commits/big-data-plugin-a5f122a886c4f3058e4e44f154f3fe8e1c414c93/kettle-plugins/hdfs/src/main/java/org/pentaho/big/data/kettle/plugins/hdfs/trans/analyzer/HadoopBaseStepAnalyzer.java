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

import org.apache.commons.lang.StringUtils;
import org.pentaho.big.data.kettle.plugins.hdfs.trans.HadoopFileMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.steps.file.BaseFileMeta;
import org.pentaho.dictionary.DictionaryConst;
import org.pentaho.metaverse.api.IMetaverseNode;
import org.pentaho.metaverse.api.MetaverseException;
import org.pentaho.metaverse.api.StepField;
import org.pentaho.metaverse.api.analyzer.kettle.step.ExternalResourceStepAnalyzer;
import org.pentaho.metaverse.api.model.IExternalResourceInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Common functionality for Hadoop input and output step analyzers.
 */
public abstract class HadoopBaseStepAnalyzer<M extends BaseFileMeta> extends ExternalResourceStepAnalyzer<M> {

  @Override protected boolean normalizeFilePath() {
    return false;
  }

  @Override protected Set<StepField> getUsedFields( final M meta ) {
    return null;
  }

  /**
   * The Hadoop file input step supports local and remote files. Since we can have a mix of both, we intentionally
   * use the generic "File Field" type, rather than the more specific "Hadoop Field" type.
   */
  @Override public String getResourceInputNodeType() {
    return DictionaryConst.NODE_TYPE_FILE_FIELD;
  }

  @Override public String getResourceOutputNodeType() {
    return DictionaryConst.NODE_TYPE_HDFS_FIELD;
  }

  @Override
  public Set<Class<? extends BaseStepMeta>> getSupportedSteps() {
    return new HashSet<Class<? extends BaseStepMeta>>() {
      {
        add( getMetaClass() );
      }
    };
  }

  public abstract Class<M> getMetaClass();

  @Override public IMetaverseNode createResourceNode( final IExternalResourceInfo resource ) throws MetaverseException {
    return createFileNode( resource.getName(), descriptor );
  }

  @Override public IMetaverseNode createResourceNode( final M meta, final IExternalResourceInfo resource )
    throws MetaverseException {

    IMetaverseNode resourceNode = null;
    if ( meta instanceof HadoopFileMeta ) {
      resourceNode = createResourceNode( resource );
      final HadoopFileMeta hMeta = (HadoopFileMeta) meta;
      final String hostName = hMeta.getUrlHostName( resource.getName() );
      if ( StringUtils.isNotBlank( hostName ) ) {
        resourceNode.setProperty( DictionaryConst.PROPERTY_HOST_NAME, hostName );
        // update the default "File" type to "HDFS File"
        resourceNode.setProperty( DictionaryConst.PROPERTY_TYPE, DictionaryConst.NODE_TYPE_HDFS_FILE );

        final String clusterName = hMeta.getClusterName( resource.getName() );
        if ( StringUtils.isNotBlank( clusterName ) ) {
          resourceNode.setProperty( DictionaryConst.PROPERTY_CLUSTER, clusterName );
        }
      }
    }
    return resourceNode;
  }
}
