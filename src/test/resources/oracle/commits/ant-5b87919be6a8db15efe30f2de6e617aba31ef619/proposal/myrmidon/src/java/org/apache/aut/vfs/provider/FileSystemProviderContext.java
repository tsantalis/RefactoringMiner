/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import org.apache.aut.vfs.FileSystemException;

/**
 * Used for a file system provider to access the services it needs, such
 * as the file system cache or other file system providers.
 *
 * @author Adam Murdoch
 */
public interface FileSystemProviderContext
{
    /**
     * Locates a cached file system by root URI.
     */
    FileSystem getFileSystem( String rootURI );

    /**
     * Registers a file system for caching.
     */
    void putFileSystem( String rootURI, FileSystem fs ) throws FileSystemException;
}
