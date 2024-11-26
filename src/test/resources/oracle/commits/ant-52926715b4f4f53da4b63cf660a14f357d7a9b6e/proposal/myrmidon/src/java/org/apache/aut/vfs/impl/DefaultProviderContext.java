/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.impl;

import java.util.HashMap;
import java.util.Map;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.FileSystemProviderContext;

/**
 * A provider context implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
final class DefaultProviderContext
    implements FileSystemProviderContext
{
    private final DefaultFileSystemManager m_manager;

    /**
     * The cached file systems.  This is a mapping from root URI to
     * FileSystem object.
     */
    private final Map m_fileSystems = new HashMap();

    public DefaultProviderContext( final DefaultFileSystemManager manager )
    {
        m_manager = manager;
    }

    /**
     * Locate a file by name.
     */
    public FileObject resolveFile( final FileObject baseFile, final String name )
        throws FileSystemException
    {
        return m_manager.resolveFile( baseFile, name );
    }

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
