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

/**
 * A file system provider, or factory.
 */
public interface FileSystemProvider
{
    /**
     * Sets the context for this file system provider.  This method is called
     * before any of the other provider methods.
     */
    void setContext( FileSystemProviderContext context );

    /**
     * Locates a file object, by absolute URI.
     *
     * @param uri
     *          The absolute URI of the file to find.
     */
    FileObject findFile( String uri ) throws FileSystemException;
}
