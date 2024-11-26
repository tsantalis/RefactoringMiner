/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.local;

import java.io.File;
import org.apache.aut.nativelib.Os;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystemProvider;
import org.apache.aut.vfs.provider.DefaultFileName;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A file system provider, which uses direct file access.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class LocalFileSystemProvider extends AbstractFileSystemProvider
    implements FileSystemProvider
{
    private final LocalFileNameParser m_parser;

    public LocalFileSystemProvider()
    {
        if( Os.isFamily( Os.OS_FAMILY_WINDOWS ) )
        {
            m_parser = new WindowsFileNameParser();
        }
        else
        {
            m_parser = new GenericFileNameParser();
        }
    }

    /**
     * Determines if a name is an absolute file name.
     */
    public boolean isAbsoluteLocalName( final String name )
    {
        return m_parser.isAbsoluteName( name );
    }

    /**
     * Finds a local file, from its local name.
     */
    public FileObject findLocalFile( final String name ) throws FileSystemException
    {
        // TODO - tidy this up, no need to turn the name into an absolute URI,
        // and then straight back again
        return findFile( "file:" + name );
    }

    /**
     * Finds a local file.
     */
    public FileObject findFileByLocalName( final File file ) throws FileSystemException
    {
        // TODO - tidy this up, should build file object straight from the file
        return findFile( "file:" + file.getAbsolutePath() );
    }

    /**
     * Parses a URI into its components.  The returned value is used to
     * locate the file system in the cache (using the root prefix), and is
     * passed to {@link #createFileSystem} to create the file system.
     *
     * <p>The provider can annotate this object with any additional
     * information it requires to create a file system from the URI.
     */
    protected ParsedUri parseURI( final String uri ) throws FileSystemException
    {
        return m_parser.parseUri( uri );
    }

    /**
     * Creates the filesystem.
     */
    protected FileSystem createFileSystem( final ParsedUri uri ) throws FileSystemException
    {
        // Build the name of the root file.
        final ParsedFileUri fileUri = (ParsedFileUri)uri;
        final String rootFile = fileUri.getRootFile();

        // Create the file system
        final DefaultFileName rootName = new DefaultFileName( m_parser, fileUri.getRootURI(), "/" );
        return new LocalFileSystem( rootName, rootFile );
    }
}
