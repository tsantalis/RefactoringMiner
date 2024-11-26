/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * This interface represents a file, and is used to access the content and
 * structure of the file.
 *
 * <p>Files are arranged in a hierarchy.  Each hierachy forms a
 * <i>file system</i>.  A file system represents things like a local OS
 * file system, a windows share, an HTTP server, or the contents of a Zip file.
 *
 * <p>There are two types of files: <i>Folders</i>, which contain other files,
 * and <i>normal files</i>, which contain data, or <i>content</i>.  A folder may
 * not have any content, and a normal file cannot contain other files.
 *
 * <h4>File Naming</h4>
 *
 * <p>TODO - write this.
 *
 * <h4>Reading and Writing a File</h4>
 *
 * <p>Reading and writing a file, and all other operations on the file's
 * <i>content</i>, is done using the {@link FileContent} object returned
 * by {@link #getContent}.
 *
 * <h4>Creating and Deleting a File</h4>
 *
 * <p>A file is created using either {@link #create}, or by writing to the
 * file using one of the {@link FileContent} methods.
 *
 * <p>A file is deleted using {@link #delete}.  Deletion is recursive, so
 * that when a folder is deleted, so are all its child files.
 *
 * <h4>Finding Files</h4>
 *
 * <p>Other files in the <i>same</i> file system as this file can be found using:
 * <ul>
 * <li>{@link #resolveFile} to find another file relative to this file.
 * <li>{@link #getChildren} to find the children of this file.
 * <li>{@link #getParent} to find the folder containing this file.
 * <li>{@link #getRoot} to find the root folder of the file system.
 * </ul>
 *
 * <p>To find files in another file system, use a {@link FileSystemManager}.
 *
 * @see FileSystemManager
 * @see FileContent
 * @see FileName
 *
 * @author Adam Murdoch
 */
public interface FileObject
{
    /**
     * Returns the name of this file.
     */
    FileName getName();

    /**
     * Determines if this file exists.
     *
     * @return
     *      <code>true</code> if this file exists, <code>false</code> if not.
     *
     * @throws FileSystemException
     *      On error determining if this file exists.
     */
    boolean exists() throws FileSystemException;

    /**
     * Returns this file's type.
     *
     * @return
     *      Either {@link FileType#FILE} or {@link FileType#FOLDER}.  Never
     *      returns null.
     *
     * @throws FileSystemException
     *      If the file does not exist, or on error determining the file's type.
     */
    FileType getType() throws FileSystemException;

    /**
     * Returns the folder that contains this file.
     *
     * @return
     *      The folder that contains this file.  Returns null if this file is
     *      the root of a file system.
     *
     * @throws FileSystemException
     *      On error finding the file's parent.
     */
    FileObject getParent() throws FileSystemException;

    /**
     * Returns the root of the file system containing this file.
     *
     * @return
     *      The root of the file system.
     *
     * @throws FileSystemException
     *      On error finding the root of the file system.
     */
    FileObject getRoot() throws FileSystemException;

    /**
     * Lists the children of this file.
     *
     * @return
     *      An array containing the children of this file.  The array is
     *      unordered.  If the file does not have any children, a zero-length
     *      array is returned.  This method never returns null.
     *
     * @throws FileSystemException
     *      If this file does not exist, or is not a folder, or on error
     *      listing this file's children.
     */
    FileObject[] getChildren() throws FileSystemException;

    /**
     * Finds a file, relative to this file.  Refer to {@link NameScope}
     * for a description of how names are resolved in the different scopes.
     *
     * @param name
     *      The name to resolve.
     *
     * @return
     *      The file.
     *
     * @throws FileSystemException
     *      On error parsing the path, or on error finding the file.
     */
    FileObject resolveFile( String name, NameScope scope ) throws FileSystemException;

    /**
     * Finds a file, relative to this file.  Equivalent to calling
     * <code>resolveFile( path, NameScope.FILE_SYSTEM )</code>.
     *
     * @param path
     *      The path of the file to locate.  Can either be a relative
     *      path or an absolute path.
     *
     * @return
     *      The file.
     *
     * @throws FileSystemException
     *      On error parsing the path, or on error finding the file.
     */
    FileObject resolveFile( String path ) throws FileSystemException;

    /**
     * Deletes this file, and all children.  Does nothing if the file
     * does not exist.
     *
     * <p>This method is not transactional.  If it fails and throws an
     * exception, some of this file's descendents may have been deleted.
     *
     * @throws FileSystemException
     *      If this file or one of its descendents is read-only, or on error
     *      deleting this file or one of its descendents.
     */
    void delete() throws FileSystemException;

    /**
     * Creates this file, if it does not exist.  Also creates any ancestor
     * folders which do not exist.  This method does nothing if the file
     * already exists with the requested type.
     *
     * @param type
     *      The type of file to create.
     *
     * @throws FileSystemException
     *      If the file already exists with the wrong type, or the parent
     *      folder is read-only, or on error creating this file or one of
     *      its ancestors.
     */
    void create( FileType type ) throws FileSystemException;

    /**
     * Copies the content of another file to this file.
     *
     * If this file does not exist, it is created.  Its parent folder is also
     * created, if necessary.  If this file does exist, its content is replaced.
     *
     * @param file The file to copy the content from.
     *
     * @throws FileSystemException
     *      If this file is read-only, or is a folder, or if the supplied file
     *      is not a file, or on error copying the content.
     */
    void copy( FileObject file ) throws FileSystemException;

    /**
     * Returns this file's content.  The {@link FileContent} returned by this
     * method can be used to read and write the content of the file.
     *
     * <p>This method can be called if the file does not exist, and
     * the returned {@link FileContent} can be used to create the file
     * by writing its content.
     *
     * @todo Do not throw an exception if this file is a folder.  Instead,
     *       throw the exceptions when (if) any methods on the returned object
     *       are called.  This is to hand 2 cases: when the folder is deleted
     *       and recreated as a file, and to allow attributes of the folder
     *       to be set (last modified time, permissions, etc).
     *
     * @return
     *      This file's content.
     *
     * @throws FileSystemException
     *      If this file is a folder.
     */
    FileContent getContent() throws FileSystemException;

    /**
     * Closes this file, and its content.  This method is a hint to the
     * implementation that it can release any resources asociated with
     * the file.
     *
     * <p>The file object can continue to be used after this method is called.
     *
     * @see FileContent#close
     *
     * @throws FileSystemException
     *      On error closing the file.
     */
    void close() throws FileSystemException;
}
