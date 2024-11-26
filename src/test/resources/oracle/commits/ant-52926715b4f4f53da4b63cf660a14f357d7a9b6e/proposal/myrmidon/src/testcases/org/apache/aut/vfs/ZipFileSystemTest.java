/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import java.io.File;
import org.apache.aut.vfs.provider.zip.ZipFileSystemProvider;

/**
 * Tests for the Zip file system.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class ZipFileSystemTest extends AbstractReadOnlyFileSystemTest
{
    public ZipFileSystemTest( String name )
    {
        super( name );
    }

    /**
     * Returns the URI for the base folder.
     */
    protected FileObject getBaseFolder() throws Exception
    {
        File zipFile = getTestResource( "test.zip" );
        String uri = "zip:" + zipFile.getAbsolutePath() + "!basedir";
        m_manager.addProvider( "zip", new ZipFileSystemProvider() );
        return m_manager.resolveFile( uri );
    }
}
