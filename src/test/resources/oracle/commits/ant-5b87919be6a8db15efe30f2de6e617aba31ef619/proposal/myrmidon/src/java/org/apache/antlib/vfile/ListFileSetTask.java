/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * A debug task, that lists the contents of a {@link FileSet}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:task name="v-list-fileset"
 */
public class ListFileSetTask
    extends AbstractTask
{
    private FileSet m_fileSet;

    public void set( final FileSet fileSet )
    {
        m_fileSet = fileSet;
    }

    /**
     * Execute task.
     */
    public void execute()
        throws TaskException
    {
        FileSetResult result = m_fileSet.getResult( getContext() );
        final FileObject[] files = result.getFiles();
        final String[] paths = result.getPaths();
        for( int i = 0; i < files.length; i++ )
        {
            final FileObject file = files[ i ];
            final String path = paths[ i ];
            getLogger().info( path + " = " + file );
        }
    }
}
