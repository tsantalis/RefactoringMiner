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

    protected FileSystemProviderContext m_context;

    /**
     * Sets the context for this file system provider.  This method is called
     * before any of the other provider methods.
     */
    public void setContext( FileSystemProviderContext context )
    {
        m_context = context;
    }

    /**
     * Locates a file object, by absolute URI.
     *
     * @param uri
     *          The absolute URI of the file to find.
     */
    public FileObject findFile( String uri ) throws FileSystemException
    {
        // Parse the URI
        ParsedUri parsedURI = null;
        try
        {
            parsedURI = parseURI( uri );
        }
        catch( FileSystemException exc )
        {
            final String message = REZ.getString( "invalid-absolute-uri.error", uri );
            throw new FileSystemException( message, exc );
        }

        // Check in the cache for the file system
        FileSystem fs = m_context.getFileSystem( parsedURI.getRootURI() );
        if( fs == null )
        {
            // Need to create the file system
            fs = createFileSystem( parsedURI );
            m_context.putFileSystem( parsedURI.getRootURI(), fs );
        }

        // Locate the file
        return fs.findFile( parsedURI.getPath() );
    }

    /**
     * Parses a URI into its components.  The returned value is used to
     * locate the file system in the cache (using the root prefix), and is
     * passed to {@link #createFileSystem} to create the file system.
     *
     * <p>The provider can annotate this object with any additional
     * information it requires to create a file system from the URI.
     */
    protected abstract ParsedUri parseURI( String uri ) throws FileSystemException;

    /**
     * Creates the filesystem.
     */
    protected abstract org.apache.aut.vfs.provider.FileSystem createFileSystem( ParsedUri uri ) throws FileSystemException;
}
