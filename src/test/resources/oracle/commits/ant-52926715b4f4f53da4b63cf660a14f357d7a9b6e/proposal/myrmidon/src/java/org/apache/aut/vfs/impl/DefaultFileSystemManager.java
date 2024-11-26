/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileSystemManager;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.provider.UriParser;
import org.apache.aut.vfs.provider.local.LocalFileSystemProvider;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A default file system manager implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultFileSystemManager
    implements FileSystemManager
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( DefaultFileSystemManager.class );

    /** The default provider. */
    private final LocalFileSystemProvider m_localFileProvider;

    /** Mapping from URI scheme to FileSystemProvider. */
    private final Map m_providers = new HashMap();

    /** The provider context. */
    private final DefaultProviderContext m_context = new DefaultProviderContext( this );

    /** The base file to use for relative URI. */
    private FileObject m_baseFile;

    public DefaultFileSystemManager()
    {
        // Create the local provider
        m_localFileProvider = new LocalFileSystemProvider();
        m_providers.put( "file", m_localFileProvider );
        m_localFileProvider.setContext( m_context );
    }

    /**
     * Registers a file system provider.
     */
    public void addProvider( final String urlScheme,
                             final FileSystemProvider provider )
        throws FileSystemException
    {
        addProvider( new String[] { urlScheme }, provider );
    }

    /**
     * Registers a file system provider.
     */
    public void addProvider( final String[] urlSchemes,
                             final FileSystemProvider provider )
        throws FileSystemException
    {
        // Check for duplicates
        for( int i = 0; i < urlSchemes.length; i++ )
        {
            final String scheme = urlSchemes[i ];
            if( m_providers.containsKey( scheme ) )
            {
                final String message = REZ.getString( "multiple-providers-for-scheme.error", scheme );
                throw new FileSystemException( message );
            }
        }

        // Contextualise
        provider.setContext( m_context );

        // Add to map
        for( int i = 0; i < urlSchemes.length; i++ )
        {
            final String scheme = urlSchemes[ i ];
            m_providers.put( scheme, provider );
        }
    }

    /**
     * Closes all file systems created by this file system manager.
     */
    public void close()
    {
        // TODO - implement this
    }

    /**
     * Sets the base file to use when resolving relative URI.
     */
    public void setBaseFile( final FileObject baseFile ) throws FileSystemException
    {
        m_baseFile = baseFile;
    }

    /**
     * Sets the base file to use when resolving relative URI.
     */
    public void setBaseFile( final File baseFile ) throws FileSystemException
    {
        m_baseFile = m_localFileProvider.findLocalFile( baseFile.getAbsolutePath() );
    }

    /**
     * Returns the base file used to resolve relative URI.
     */
    public FileObject getBaseFile()
    {
        return m_baseFile;
    }

    /**
     * Locates a file by URI.
     */
    public FileObject resolveFile( final String uri ) throws FileSystemException
    {
        return resolveFile( m_baseFile, uri );
    }

    /**
     * Locates a file by URI.
     */
    public FileObject resolveFile( final File baseFile, final String uri )
        throws FileSystemException
    {
        final FileObject baseFileObj = m_localFileProvider.findLocalFile( baseFile );
        return resolveFile( baseFileObj, uri );
    }

    /**
     * Resolves a URI, relative to a base file.
     */
    public FileObject resolveFile( final FileObject baseFile, final String uri )
        throws FileSystemException
    {
        // Extract the scheme
        final String scheme = UriParser.extractScheme( uri );
        if( scheme != null )
        {
            // An absolute URI - locate the provider
            final FileSystemProvider provider = (FileSystemProvider)m_providers.get( scheme );
            if( provider != null )
            {
                return provider.findFile( baseFile, uri );
            }
        }

        // Decode the URI (remove %nn encodings)
        final String decodedUri = UriParser.decode( uri );

        // Handle absolute file names
        if( m_localFileProvider.isAbsoluteLocalName( decodedUri ) )
        {
            return m_localFileProvider.findLocalFile( decodedUri );
        }

        // Assume a bad scheme
        if( scheme != null )
        {
            final String message = REZ.getString( "unknown-scheme.error", scheme, uri );
            throw new FileSystemException( message );
        }

        // Use the supplied base file
        if( baseFile == null )
        {
            final String message = REZ.getString( "find-rel-file.error", uri );
            throw new FileSystemException( message );
        }
        return baseFile.resolveFile( decodedUri );
    }

    /**
     * Converts a local file into a {@link FileObject}.
     */
    public FileObject convert( final File file )
        throws FileSystemException
    {
        return m_localFileProvider.findLocalFile( file );
    }

    /**
     * Creates a layered file system.
     */
    public FileObject createFileSystem( final String scheme,
                                        final FileObject file )
        throws FileSystemException
    {
        FileSystemProvider provider = (FileSystemProvider)m_providers.get( scheme );
        if( provider == null )
        {
            final String message = REZ.getString( "unknown-provider.error", scheme );
            throw new FileSystemException( message );
        }
        return provider.createFileSystem( scheme, file );
    }
}
