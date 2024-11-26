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
import org.apache.aut.vfs.FileSystemManager;

/**
 * Used for a file system provider to access the services it needs, such
 * as the file system cache or other file system providers.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface FileSystemProviderContext
{
    /**
     * Locate a file by name.  See
     * {@link FileSystemManager#resolveFile(FileObject, String)} for a
     * description of how this works.
     */
    FileObject resolveFile( FileObject baseFile, String name )
        throws FileSystemException;

    /**
     * Locates a cached file system by root URI.
     */
    FileSystem getFileSystem( String rootURI );

    /**
     * Registers a file system for caching.
     */
    void putFileSystem( String rootURI, FileSystem fs ) throws FileSystemException;
}
