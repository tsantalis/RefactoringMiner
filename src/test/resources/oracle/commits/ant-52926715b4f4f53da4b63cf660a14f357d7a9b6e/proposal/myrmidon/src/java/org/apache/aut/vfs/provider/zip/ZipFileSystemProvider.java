/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.zip;

import java.io.File;
import java.io.IOException;
import org.apache.aut.vfs.FileObject;
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
 *
 * @ant:type type="file-system" name="zip"
 */
public class ZipFileSystemProvider
    extends AbstractFileSystemProvider
    implements FileSystemProvider
{
    private final ZipFileNameParser m_parser = new ZipFileNameParser();

    /**
     * Parses a URI into its components.
     */
    protected ParsedUri parseUri( final FileObject baseFile,
                                  final String uriStr )
        throws FileSystemException
    {
        // Parse the URI
        final ParsedZipUri uri = m_parser.parseZipUri( uriStr );

        // Make the URI canonical

        // Resolve the Zip file name
        final String fileName = uri.getZipFileName();
        final FileObject file = getContext().resolveFile( baseFile, fileName );
        uri.setZipFile( file );

        // Rebuild the root URI
        final String rootUri = m_parser.buildRootUri( uri );
        uri.setRootUri( rootUri );

        return uri;
    }

    /**
     * Builds the URI for the root of a layered file system.
     */
    protected ParsedUri buildUri( final String scheme,
                                  final FileObject file )
        throws FileSystemException
    {
        ParsedZipUri uri = new ParsedZipUri();
        uri.setScheme( scheme );
        uri.setZipFile( file );
        final String rootUri = m_parser.buildRootUri( uri );
        uri.setRootUri( rootUri );
        uri.setPath( "/" );
        return uri;
    }

    /**
     * Creates the filesystem.
     */
    protected FileSystem createFileSystem( final ParsedUri uri )
        throws FileSystemException
    {
        final ParsedZipUri zipUri = (ParsedZipUri)uri;
        final FileObject file = zipUri.getZipFile();

        // TODO - temporary hack; need to use a converter
        File destFile = null;
        try
        {
            final File cacheDir = new File( "ant_vfs_cache" );
            cacheDir.mkdirs();
            destFile = File.createTempFile( "cache_", "_" + file.getName().getBaseName(), cacheDir );
            destFile.deleteOnExit();
        }
        catch( IOException e )
        {
            throw new FileSystemException( "Could not replicate file", e );
        }
        FileObject destFileObj = getContext().resolveFile( null, destFile.getAbsolutePath() );
        destFileObj.copy( file );

        // Create the file system
        DefaultFileName name = new DefaultFileName( m_parser, zipUri.getRootUri(), "/" );
        return new ZipFileSystem( name, destFile );
    }

}
