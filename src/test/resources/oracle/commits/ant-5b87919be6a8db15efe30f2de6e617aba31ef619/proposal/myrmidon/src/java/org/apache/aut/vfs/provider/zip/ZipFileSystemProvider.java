/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.zip;

import java.io.File;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystemProvider;
import org.apache.aut.vfs.provider.DefaultFileName;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A file system provider for Zip/Jar files.  Provides read-only file
 * systems, for local Zip files only.
 *
 * @author Adam Murdoch
 */
public class ZipFileSystemProvider extends AbstractFileSystemProvider
    implements FileSystemProvider
{
    private ZipFileNameParser m_parser = new ZipFileNameParser();

    /**
     * Parses a URI into its components.
     */
    protected ParsedUri parseURI( String uri ) throws FileSystemException
    {
        return m_parser.parseUri( uri );
    }

    /**
     * Creates the filesystem.
     */
    protected FileSystem createFileSystem( ParsedUri uri ) throws FileSystemException
    {
        // Locate the Zip file
        ParsedZipUri zipUri = (ParsedZipUri)uri;
        String fileName = zipUri.getZipFile();
        // TODO - use the context to resolve zip file to a FileObject
        File file = new File( fileName ).getAbsoluteFile();
        DefaultFileName name = new DefaultFileName( m_parser, zipUri.getRootURI(), "/" );
        return new ZipFileSystem( name, file );
    }
}
