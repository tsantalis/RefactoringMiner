/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.ftp;

import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystemProvider;
import org.apache.aut.vfs.provider.DefaultFileName;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A provider for FTP file systems.
 *
 * @author Adam Murdoch
 *
 * @ant:type type="file-system" name="ftp"
 */
public class FtpFileSystemProvider extends AbstractFileSystemProvider
{
    private final FtpFileNameParser m_parser = new FtpFileNameParser();

    /**
     * Parses a URI into its components.
     */
    protected ParsedUri parseUri( final FileObject baseFile,
                                  final String uri )
        throws FileSystemException
    {
        return m_parser.parseFtpUri( uri );
    }

    /**
     * Creates the filesystem.
     */
    protected FileSystem createFileSystem( final ParsedUri uri )
        throws FileSystemException
    {
        final ParsedFtpUri ftpUri = (ParsedFtpUri)uri;

        // Build the root name
        final FileName rootName = new DefaultFileName( m_parser, ftpUri.getRootUri(), "/" );

        // Determine the username and password to use
        String username = ftpUri.getUserName();
        if( username == null )
        {
            username = "anonymous";
        }
        String password = ftpUri.getPassword();
        if( password == null )
        {
            password = "anonymous";
        }

        // Create the file system
        return new FtpFileSystem( rootName, ftpUri.getHostName(), username, password );
    }
}
