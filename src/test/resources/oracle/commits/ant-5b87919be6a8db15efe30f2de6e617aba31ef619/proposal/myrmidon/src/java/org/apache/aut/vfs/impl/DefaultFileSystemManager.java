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
import java.util.Iterator;
import java.util.Map;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileSystemManager;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.provider.FileSystemProviderContext;
import org.apache.aut.vfs.provider.UriParser;
import org.apache.aut.vfs.provider.local.LocalFileSystemProvider;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A default file system manager implementation.
 *
 * @author Adam Murdoch
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
    private final ProviderContextImpl m_context = new ProviderContextImpl();

    /** The base file to use for relative URI. */
    private FileObject m_baseFile;

    /**
     * The cached file systems.  This is a mapping from root URI to
     * FileSystem object.
     */
    private final Map m_fileSystems = new HashMap();

    public DefaultFileSystemManager() throws Exception
    {
        // Create the local provider
        m_localFileProvider = new LocalFileSystemProvider();
        m_providers.put( "file", m_localFileProvider );

        // TODO - make this list configurable
        // Create the providers

        FileSystemProvider provider = createProvider( "org.apache.aut.vfs.provider.zip.ZipFileSystemProvider" );
        if( provider != null )
        {
            m_providers.put( "zip", provider );
            m_providers.put( "jar", provider );
        }

        provider = createProvider( "org.apache.aut.vfs.provider.smb.SmbFileSystemProvider" );
        if( provider != null )
        {
            m_providers.put( "smb", provider );
        }

        provider = createProvider( "org.apache.aut.vfs.provider.ftp.FtpFileSystemProvider" );
        if( provider != null )
        {
            m_providers.put( "ftp", provider );
        }

        // Contextualise the providers
        for( Iterator iterator = m_providers.values().iterator(); iterator.hasNext(); )
        {
            provider = (FileSystemProvider)iterator.next();
            provider.setContext( m_context );
        }
    }

    /**
     * Creates a provider instance, returns null if the provider class is
     * not found.
     */
    private FileSystemProvider createProvider( final String className )
        throws Exception
    {
        try
        {
            // TODO - wrap exceptions
            return (FileSystemProvider)Class.forName( className ).newInstance();
        }
        catch( ClassNotFoundException e )
        {
            // This is fine, for now
            return null;
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
        final FileObject baseFileObj = m_localFileProvider.findFileByLocalName( baseFile );
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
                return provider.findFile( uri );
            }
        }

        // Handle absolute file names
        if( m_localFileProvider.isAbsoluteLocalName( uri ) )
        {
            return m_localFileProvider.findLocalFile( uri );
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
        return baseFile.resolveFile( uri );
    }

    /**
     * A provider context implementation.
     */
    private final class ProviderContextImpl
        implements FileSystemProviderContext
    {
        /**
         * Locates a cached file system by root URI.
         */
        public FileSystem getFileSystem( final String rootURI )
        {
            // TODO - need to have a per-fs uri comparator
            return (FileSystem)m_fileSystems.get( rootURI );
        }

        /**
         * Registers a file system for caching.
         */
        public void putFileSystem( final String rootURI, final FileSystem fs )
            throws FileSystemException
        {
            // TODO - should really check that there's not one already cached
            m_fileSystems.put( rootURI, fs );
        }
    }
}
