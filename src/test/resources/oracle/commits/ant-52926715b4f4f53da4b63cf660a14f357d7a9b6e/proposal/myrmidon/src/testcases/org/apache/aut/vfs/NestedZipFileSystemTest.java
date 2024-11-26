/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import org.apache.aut.vfs.provider.zip.ZipFileSystemProvider;

/**
 * Tests for the Zip file system, using a zip file nested inside another zip file.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class NestedZipFileSystemTest
    extends AbstractReadOnlyFileSystemTest
{
    public NestedZipFileSystemTest( String name )
    {
        super( name );
    }

    /**
     * Returns the URI for the base folder.
     */
    protected FileObject getBaseFolder() throws Exception
    {
        m_manager.addProvider( "zip", new ZipFileSystemProvider() );

        // Locate the base Zip file
        final String zipFilePath = getTestResource( "nested.zip" ).getAbsolutePath();
        String uri = "zip:" + zipFilePath + "!/test.zip";
        final FileObject zipFile = m_manager.resolveFile( uri );

        // Now build the nested file system
        final FileObject nestedFS = m_manager.createFileSystem( "zip", zipFile );
        return nestedFS.resolveFile( "/basedir" );
    }
}
