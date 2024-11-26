/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import java.io.File;

/**
 * A FileSystemManager is manages a set of file systems.  This interface is
 * used to locate a {@link FileObject} by name from one of those file systems.
 *
 * <p>To locate a {@link FileObject}, use one of the <code>resolveFile()</code>
 * methods.</p>
 *
 * <h4><a name="naming">File Naming</a></h4>
 *
 * <p>A file system manager can recognise several types of file names:
 *
 * <ul>
 *
 * <li><p>Absolute URI.  These must start with a scheme, such as
 * <code>file:</code> or <code>ftp:</code>, followed by a scheme dependent
 * file name.  Some examples:</p>
 * <pre>
 * file:/c:/somefile
 * ftp://somewhere.org/somefile
 * </pre>
 *
 * <li><p>Absolute local file name.  For example,
 * <code>/home/someuser/a-file</code> or <code>c:\dir\somefile.html</code>.
 * Elements in the name can be separated using any of the following
 * characters: <code>/</code>, <code>\</code>, or the native file separator
 * character. For example, the following file names are the same:</p>
 * <pre>
 * c:\somedir\somefile.xml
 * c:/somedir/somefile.xml
 * </pre>
 *
 * <li><p>Relative path.  For example: <code>../somefile</code> or
 * <code>somedir/file.txt</code>.   The file system manager resolves relative
 * paths against its <i>base file</i>.  Elements in the relative path can be
 * separated using <code>/</code>, <code>\</code>, or file system specific
 * separator characters.  Relative paths may also contain <code>..</code> and
 * <code>.</code> elements.  See {@link FileObject#resolveFile} for more details.</p>
 *
 * </ul>
 *
 * @author Adam Murdoch
 * @ant:role shorthand="file-system-manager"
 */
public interface FileSystemManager
{
    String ROLE = FileSystemManager.class.getName();

    /**
     * Returns the base file used to resolve relative paths.
     */
    FileObject getBaseFile();

    /**
     * Locates a file by name.  Equivalent to calling
     * <code>resolveFile(uri, getBaseName())</code>.
     *
     * @param name
     *          The name of the file.
     *
     * @throws FileSystemException
     *          On error parsing the file name.
     */
    FileObject resolveFile( String name )
        throws FileSystemException;

    /**
     * Locates a file by name.  The name is resolved as described
     * <a href="#naming">above</a>.  That is, the name can be either
     * an absolute URI, an absolute file name, or a relative path to
     * be resolved against <code>baseFile</code>.
     *
     * <p>Note that the file does not have to exist when this method is called.
     *
     * @param name
     *          The name of the file.
     *
     * @param baseFile
     *          The base file to use to resolve relative paths.
     *
     * @throws FileSystemException
     *          On error parsing the file name.
     */
    FileObject resolveFile( FileObject baseFile, String name )
        throws FileSystemException;

    /**
     * Locates a file by name.  See {@link #resolveFile(FileObject, String)}
     * for details.
     *
     * @param baseFile
     *          The base file to use to resolve relative paths.
     *
     * @param name
     *          The name of the file.
     *
     * @throws FileSystemException
     *          On error parsing the file name.
     *
     */
    FileObject resolveFile( File baseFile, String name )
        throws FileSystemException;

    /**
     * Converts a local file into a {@link FileObject}.
     *
     * @param file
     *          The file to convert.
     *
     * @return
     *          The {@link FileObject} that represents the local file.
     *
     * @throws FileSystemException
     *          On error converting the file.
     */
    FileObject convert( File file )
        throws FileSystemException;

    /**
     * Creates a layered file system.  A layered file system is a file system
     * that is created from the contents of another file file, such as a zip
     * or tar file.
     *
     * @param provider
     *          The name of the file system provider to use.  This name is
     *          the same as the scheme used in URI to identify the provider.
     *
     * @param file
     *          The file to use to create the file system.
     *
     * @throws FileSystemException
     *          On error creating the file system.
     */
    FileObject createFileSystem( String provider, FileObject file )
        throws FileSystemException;
}
