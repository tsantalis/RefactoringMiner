/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.aut.vfs.FileContent;
import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.aut.vfs.NameScope;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A partial file object implementation.
 *
 * @author Adam Murdoch
 */
public abstract class AbstractFileObject
    implements FileObject
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( AbstractFileObject.class );

    private final static FileObject[] EMPTY_FILE_ARRAY = {};

    private FileName m_name;
    private AbstractFileSystem m_fs;
    private DefaultFileContent m_content;

    // Cached info
    private boolean m_attached;
    private AbstractFileObject m_parent;
    private FileType m_type;
    private FileObject[] m_children;

    protected AbstractFileObject( FileName name, AbstractFileSystem fs )
    {
        m_name = name;
        m_fs = fs;
    }

    /**
     * Returns true if this file is read-only.
     */
    protected boolean isReadOnly()
    {
        return false;
    }

    /**
     * Attaches this file object to its file resource.  This method is called
     * before any of the doBlah() or onBlah() methods.  Sub-classes can use
     * this method to perform lazy initialisation.
     */
    protected void doAttach() throws Exception
    {
    }

    /**
     * Detaches this file object from its file resource.
     *
     * <p>Called when this file is closed, or its type changes.  Note that
     * the file object may be reused later, so should be able to be reattached.
     */
    protected void doDetach()
    {
    }

    /**
     * Determines the type of the file, returns null if the file does not
     * exist.  The return value of this method is cached, so the
     * implementation can be expensive.
     */
    protected abstract FileType doGetType() throws Exception;

    /**
     * Lists the children of the file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.  The return value of this method
     * is cached, so the implementation can be expensive.
     */
    protected abstract String[] doListChildren() throws Exception;

    /**
     * Deletes the file.  Is only called when:
     * <ul>
     * <li>{@link #isReadOnly} returns false.
     * <li>{@link #doGetType} does not return null.
     * <li>If this file is a folder, it has no children.
     * </ul>
     */
    protected void doDelete() throws Exception
    {
        final String message = REZ.getString( "delete-not-supported.error" );
        throw new FileSystemException( message );
    }

    /**
     * Creates this file as a folder.  Is only called when:
     * <ul>
     * <li>{@link #isReadOnly} returns false.
     * <li>{@link #doGetType} returns null.
     * <li>The parent folder exists or this file is the root of the file
     *     system.
     * </ul>
     */
    protected void doCreateFolder() throws Exception
    {
        final String message = REZ.getString( "create-folder-not-supported.error" );
        throw new FileSystemException( message );
    }

    /**
     * Called when the children of this file change.
     */
    protected void onChildrenChanged()
    {
    }

    /**
     * Returns the size of the file content (in bytes).  Is only called if
     * {@link #doGetType} returns {@link FileType#FILE}.
     */
    protected abstract long doGetContentSize() throws Exception;

    /**
     * Creates an input stream to read the file content from.  Is only called
     * if  {@link #doGetType} returns {@link FileType#FILE}.
     *
     * <p>There is guaranteed never to be more than one stream for this file
     * (input or output) open at any given time.
     *
     * <p>The returned stream does not have to be buffered.
     */
    protected abstract InputStream doGetInputStream() throws Exception;

    /**
     * Creates an output stream to write the file content to.  Is only
     * called if:
     * <ul>
     * <li>This file is not read-only.
     * <li>{@link #doGetType} returns {@link FileType#FILE}, or
     * {@link #doGetType} returns null, and the file's parent exists
     * and is a folder.
     * </ul>
     *
     * <p>There is guaranteed never to be more than one stream for this file
     * (input or output) open at any given time.
     *
     * <p>The returned stream does not have to be buffered.
     */
    protected OutputStream doGetOutputStream() throws Exception
    {
        final String message = REZ.getString( "write-not-supported.error" );
        throw new FileSystemException( message );
    }

    /**
     * Notification of the output stream being closed.
     * TODO - get rid of this.
     */
    protected void doEndOutput() throws Exception
    {
    }

    /**
     * Notification of the input stream being closed.
     * TODO - get rid of this.
     */
    protected void doEndInput() throws Exception
    {
    }

    /**
     * Returns the URI of the file.
     */
    public String toString()
    {
        return m_name.getURI();
    }

    /**
     * Returns the name of the file.
     */
    public FileName getName()
    {
        return m_name;
    }

    /**
     * Determines if the file exists.
     */
    public boolean exists() throws FileSystemException
    {
        attach();
        return ( m_type != null );
    }

    /**
     * Returns the file's type.
     */
    public FileType getType() throws FileSystemException
    {
        attach();
        if( m_type == null )
        {
            final String message = REZ.getString( "get-type-no-exist.error", m_name );
            throw new FileSystemException( message );
        }
        return m_type;
    }

    /**
     * Returns the parent of the file.
     */
    public FileObject getParent() throws FileSystemException
    {
        if( this == m_fs.getRoot() )
        {
            // Root file has no parent
            return null;
        }

        // Locate the parent of this file
        if( m_parent == null )
        {
            m_parent = (AbstractFileObject)m_fs.findFile( m_name.getParent() );
        }
        return m_parent;
    }

    /**
     * Returns the root of the file system containing the file.
     */
    public FileObject getRoot() throws FileSystemException
    {
        return m_fs.getRoot();
    }

    /**
     * Returns the children of the file.
     */
    public FileObject[] getChildren() throws FileSystemException
    {
        attach();
        if( m_type == null )
        {
            final String message = REZ.getString( "list-children-no-exist.error", m_name );
            throw new FileSystemException( message );
        }
        if( m_type != FileType.FOLDER )
        {
            final String message = REZ.getString( "list-children-not-folder.error", m_name );
            throw new FileSystemException( message );
        }

        // Use cached info, if present
        if( m_children != null )
        {
            return m_children;
        }

        // List the children
        String[] files;
        try
        {
            files = doListChildren();
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "list-children.error", m_name );
            throw new FileSystemException( message, exc );
        }

        if( files == null || files.length == 0 )
        {
            // No children
            m_children = EMPTY_FILE_ARRAY;
        }
        else
        {
            // Create file objects for the children
            m_children = new FileObject[ files.length ];
            for( int i = 0; i < files.length; i++ )
            {
                String file = files[ i ];
                m_children[ i ] = m_fs.findFile( m_name.resolveName( file, NameScope.CHILD ) );
            }
        }

        return m_children;
    }

    /**
     * Returns a child by name.
     */
    public FileObject resolveFile( String name, NameScope scope ) throws FileSystemException
    {
        // TODO - cache children (only if they exist)
        return m_fs.findFile( m_name.resolveName( name, scope ) );
    }

    /**
     * Finds a file, relative to this file.
     *
     * @param path
     *          The path of the file to locate.  Can either be a relative
     *          path, which is resolved relative to this file, or an
     *          absolute path, which is resolved relative to the file system
     *          that contains this file.
     */
    public FileObject resolveFile( String path ) throws FileSystemException
    {
        FileName name = m_name.resolveName( path );
        return m_fs.findFile( name );
    }

    /**
     * Deletes this file, once all its children have been deleted
     */
    private void deleteSelf() throws FileSystemException
    {
        if( isReadOnly() )
        {
            final String message = REZ.getString( "delete-read-only.error", m_name );
            throw new FileSystemException( message );
        }

        // Delete the file
        try
        {
            doDelete();
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "delete.error", m_name );
            throw new FileSystemException( message, exc );
        }

        // Update cached info
        updateType( null );
    }

    /**
     * Deletes this file, and all children.
     */
    public void delete() throws FileSystemException
    {
        attach();
        if( m_type == null )
        {
            // File does not exist
            return;
        }

        // Recursively delete this file and all its children
        List queue = new ArrayList();
        Set expanded = new HashSet();
        queue.add( this );

        // Recursively delete each file
        // TODO - recover from errors
        while( queue.size() > 0 )
        {
            AbstractFileObject file = (AbstractFileObject)queue.get( 0 );
            file.attach();

            if( file.m_type == null )
            {
                // Shouldn't happen
                queue.remove( 0 );
            }
            else if( file.m_type == FileType.FILE )
            {
                // Delete the file
                file.deleteSelf();
                queue.remove( 0 );
            }
            else if( expanded.contains( file ) )
            {
                // Have already deleted all the children of this folder -
                // delete it
                file.deleteSelf();
                queue.remove( 0 );
            }
            else
            {
                // Delete the folder's children
                FileObject[] children = file.getChildren();
                for( int i = 0; i < children.length; i++ )
                {
                    FileObject child = children[ i ];
                    queue.add( 0, child );
                }
                expanded.add( file );
            }
        }

        // Update parent's child list
        notifyParent();
    }

    /**
     * Creates this file, if it does not exist.  Also creates any ancestor
     * files which do not exist.
     */
    public void create( FileType type ) throws FileSystemException
    {
        attach();
        if( m_type == type )
        {
            // Already exists as correct type
            return;
        }
        if( m_type != null )
        {
            final String message = REZ.getString( "create-mismatched-type.error", type, m_name, m_type );
            throw new FileSystemException( message );
        }
        if( isReadOnly() )
        {
            final String message = REZ.getString( "create-read-only.error", type, m_name );
            throw new FileSystemException( message );
        }

        // Traverse up the heirarchy and make sure everything is a folder
        FileObject parent = getParent();
        if( parent != null )
        {
            parent.create( FileType.FOLDER );
        }

        // Create the folder
        try
        {
            if( type == FileType.FOLDER )
            {
                doCreateFolder();
                m_children = EMPTY_FILE_ARRAY;
            }
            else if( type == FileType.FILE )
            {
                OutputStream outStr = doGetOutputStream();
                outStr.close();
                endOutput();
            }
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "create.error", type, m_name );
            throw new FileSystemException( message, exc );
        }

        // Update cached info
        updateType( type );
    }

    /**
     * Returns the file's content.
     */
    public FileContent getContent() throws FileSystemException
    {
        attach();
        if( m_type == FileType.FOLDER )
        {
            final String message = REZ.getString( "get-folder-content.error", m_name );
            throw new FileSystemException( message );
        }
        if( m_content == null )
        {
            m_content = new DefaultFileContent( this );
        }
        return m_content;
    }

    /**
     * Closes this file, and its content.
     */
    public void close() throws FileSystemException
    {
        FileSystemException exc = null;

        // Close the content
        if( m_content != null )
        {
            try
            {
                m_content.close();
            }
            catch( FileSystemException e )
            {
                exc = e;
            }
        }

        // Detach from the file
        if( m_attached )
        {
            doDetach();
            m_attached = false;
            m_type = null;
            m_children = null;
        }

        if( exc != null )
        {
            throw exc;
        }
    }

    /**
     * Prepares this file for writing.  Makes sure it is either a file,
     * or its parent folder exists.  Returns an output stream to use to
     * write the content of the file to.
     */
    public OutputStream getOutputStream() throws FileSystemException
    {
        attach();
        if( isReadOnly() )
        {
            final String message = REZ.getString( "write-read-only.error", m_name );
            throw new FileSystemException( message );
        }
        if( m_type == FileType.FOLDER )
        {
            final String message = REZ.getString( "write-folder.error", m_name );
            throw new FileSystemException( message );
        }

        if( m_type == null )
        {
            // Does not exist - make sure parent does
            FileObject parent = getParent();
            if( parent != null )
            {
                parent.create( FileType.FOLDER );
            }
        }

        // Get the raw output stream
        try
        {
            return doGetOutputStream();
        }
        catch( FileSystemException exc )
        {
            throw exc;
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "write.error", m_name );
            throw new FileSystemException( message, exc );
        }
    }

    /**
     * Attaches to the file.
     */
    private void attach() throws FileSystemException
    {
        if( m_attached )
        {
            return;
        }

        try
        {
            // Attach and determine the file type
            doAttach();
            m_attached = true;
            m_type = doGetType();
        }
        catch( FileSystemException exc )
        {
            throw exc;
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "get-type.error", m_name );
            throw new FileSystemException( message, exc );
        }

    }

    /**
     * Called when the ouput stream for this file is closed.
     */
    public void endOutput() throws Exception
    {
        updateType( FileType.FILE );
        doEndOutput();
    }

    /**
     * Update cached info when this file's type changes.
     */
    private void updateType( FileType type )
    {
        // Notify parent that its child list may no longer be valid
        notifyParent();

        // Detach
        doDetach();
        m_attached = false;
        m_type = null;
        m_children = null;
    }

    /**
     * Notify the parent of a change to its children, when a child is created
     * or deleted.
     */
    private void notifyParent()
    {
        if( m_parent == null )
        {
            // Locate the parent, if it is cached
            m_parent = (AbstractFileObject)m_fs.getFile( m_name.getParent() );
        }

        if( m_parent != null )
        {
            m_parent.invalidateChildren();
        }
    }

    /**
     * Notifies a file that children have been created or deleted.
     */
    private void invalidateChildren()
    {
        m_children = null;
        onChildrenChanged();
    }
}
