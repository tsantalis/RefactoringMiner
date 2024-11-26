/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * Tests for FTP file systems.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class FtpFileSystemTest extends AbstractWritableFileSystemTest
{
    public FtpFileSystemTest( String name )
    {
        super( name );
    }

    /**
     * Returns the URI for the base folder.
     */
    protected String getBaseFolderURI()
    {
        return System.getProperty( "test.ftp.uri" ) + "/read-tests";
    }

    /**
     * Returns the URI for the area to do tests in.
     */
    protected String getWriteFolderURI()
    {
        return System.getProperty( "test.ftp.uri" ) + "/write-tests";
    }
}
