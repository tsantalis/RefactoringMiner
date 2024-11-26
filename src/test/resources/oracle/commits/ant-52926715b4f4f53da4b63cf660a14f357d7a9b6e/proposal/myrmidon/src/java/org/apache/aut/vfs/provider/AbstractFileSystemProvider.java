/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A partial file system provider implementation.
 *
 * @author Adam Murdoch
 */
public abstract class AbstractFileSystemProvider
    implements FileSystemProvider
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( AbstractFileSystemProvider.class );

    private FileSystemProviderContext m_context;

    /**
     * Returns the context for this provider.
     */
    protected FileSystemProviderContext getContext()
    {
        return m_context;
    }

    /**
     * Sets the context for this file system provider.  This method is called
     * before any of the other provider methods.
     */
    public void setContext( final FileSystemProviderContext context )
    {
        m_context = context;
    }

    /**
     * Locates a file object, by absolute URI.
     *
     * @param uri
     *          The absolute URI of the file to find.
     */
    public FileObject findFile( final FileObject baseFile,
                                final String uri ) throws FileSystemException
    {
        // Parse the URI
        ParsedUri parsedUri = null;
        try
        {
            parsedUri = parseUri( baseFile, uri );
        }
        catch( FileSystemException exc )
        {
            final String message = REZ.getString( "invalid-absolute-uri.error", uri );
            throw new FileSystemException( message, exc );
        }

        // Locate the file
        return findFile( parsedUri );

    }

    /**
     * Locates a file from its parsed URI.
     */
    private FileObject findFile( final ParsedUri parsedUri )
        throws FileSystemException
    {
        // Check in the cache for the file system
        final String rootUri = parsedUri.getRootUri();
        FileSystem fs = m_context.getFileSystem( rootUri );
        if( fs == null )
        {
            // Need to create the file system, and cache it
            fs = createFileSystem( parsedUri );
            m_context.putFileSystem( rootUri, fs );
        }

        // Locate the file
        return fs.findFile( parsedUri.getPath() );
    }

    /**
     * Creates a layered file system.
     */
    public FileObject createFileSystem( final String scheme, final FileObject file )
        throws FileSystemException
    {
        // TODO - this is a pretty shonky model for layered FS; need to revise

        // Build the URI
        final ParsedUri uri = buildUri( scheme, file );

        // Locate the file
        return findFile( uri );
    }

    /**
     * Parses a URI into its components.  The returned value is used to
     * locate the file system in the cache (using the root prefix).
     *
     * <p>The provider can annotate this object with any additional
     * information it requires to create a file system from the URI.
     */
    protected abstract ParsedUri parseUri( final FileObject baseFile, final String uri )
        throws FileSystemException;

    /**
     * Builds the URI for the root of a layered file system.
     */
    protected ParsedUri buildUri( final String scheme,
                                  final FileObject file )
        throws FileSystemException
    {
        final String message = REZ.getString( "not-layered-fs.error" );
        throw new FileSystemException( message );
    }

    /**
     * Creates the filesystem.
     */
    protected abstract FileSystem createFileSystem( final ParsedUri uri )
        throws FileSystemException;
}
