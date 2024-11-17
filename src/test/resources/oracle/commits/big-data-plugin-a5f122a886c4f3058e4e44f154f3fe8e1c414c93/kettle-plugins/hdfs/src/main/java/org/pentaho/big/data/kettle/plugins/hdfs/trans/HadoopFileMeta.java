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

package org.pentaho.big.data.kettle.plugins.hdfs.trans;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.URLFileName;
import org.pentaho.di.core.vfs.KettleVFS;

/**
 * Common functionality for a hadoop based {@link org.pentaho.di.trans.steps.file.BaseFileMeta}.
 */
public interface HadoopFileMeta {

  default String getUrlHostName( final String incomingURL ) {
    String hostName = null;
    final FileName fileName = getUrlFileName( incomingURL );
    if ( fileName instanceof URLFileName ) {
      hostName = ( (URLFileName) fileName ).getHostName();
    }
    return hostName;
  }

  default FileName getUrlFileName( final String incomingURL ) {
    FileName fileName = null;
    try {
      final String noVariablesURL = incomingURL.replaceAll( "[${}]", "/" );
      fileName = KettleVFS.getInstance().getFileSystemManager().resolveURI( noVariablesURL );
    } catch ( FileSystemException e ) {
      // no-op
    }
    return fileName;
  }

  String getUrlPath( final String incomingURL );

  String getClusterName( final String incomingURL );
}
