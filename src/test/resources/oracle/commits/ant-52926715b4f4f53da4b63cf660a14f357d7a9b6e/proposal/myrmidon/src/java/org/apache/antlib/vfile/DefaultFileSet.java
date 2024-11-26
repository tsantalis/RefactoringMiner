/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.util.ArrayList;
import org.apache.antlib.vfile.selectors.AndFileSelector;
import org.apache.antlib.vfile.selectors.FileSelector;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A file set, that contains those files under a directory that match
 * a set of selectors.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 *
 * @ant:data-type name="v-fileset"
 * @ant:type type="v-fileset" name="v-fileset"
 */
public class DefaultFileSet
    implements FileSet
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultFileSet.class );

    private FileObject m_dir;
    private final AndFileSelector m_selector = new AndFileSelector();

    public DefaultFileSet()
    {
    }

    public DefaultFileSet( final FileObject dir )
    {
        m_dir = dir;
    }

    /**
     * Sets the root directory.
     */
    public void setDir( final FileObject dir )
    {
        m_dir = dir;
    }

    /**
     * Adds a selector.
     */
    public void add( final FileSelector selector )
    {
        m_selector.add( selector );
    }

    /**
     * Returns the contents of the set.
     */
    public FileSetResult getResult( final TaskContext context )
        throws TaskException
    {
        if( m_dir == null )
        {
            final String message = REZ.getString( "fileset.dir-not-set.error" );
            throw new TaskException( message );
        }

        try
        {
            final DefaultFileSetResult result = new DefaultFileSetResult();
            final ArrayList stack = new ArrayList();
            final ArrayList pathStack = new ArrayList();
            stack.add( m_dir );
            pathStack.add( "" );

            while( stack.size() > 0 )
            {
                // Pop next folder off the stack
                FileObject folder = (FileObject)stack.remove( 0 );
                String path = (String)pathStack.remove( 0 );

                // Queue the children of the folder
                FileObject[] children = folder.getChildren();
                for( int i = 0; i < children.length; i++ )
                {
                    FileObject child = children[ i ];
                    String childPath = path + child.getName().getBaseName();

                    // Check whether to include the file in the result
                    if( m_selector.accept( child, childPath, context ) )
                    {
                        result.addElement( child, childPath );
                    }

                    if( child.getType() == FileType.FOLDER )
                    {
                        // A folder - push it on to the stack
                        stack.add( 0, child );
                        pathStack.add( 0, childPath + '/' );
                    }
                }
            }

            return result;
        }
        catch( FileSystemException e )
        {
            final String message = REZ.getString( "fileset.list-files.error", m_dir );
            throw new TaskException( message, e );
        }
    }
}
