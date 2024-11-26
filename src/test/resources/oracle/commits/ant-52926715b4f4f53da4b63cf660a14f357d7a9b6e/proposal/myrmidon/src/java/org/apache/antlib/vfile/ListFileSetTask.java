/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.util.ArrayList;
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
    private final ArrayList m_fileSets = new ArrayList();

    public void add( final FileSet fileSet )
    {
        m_fileSets.add( fileSet );
    }

    /**
     * Execute task.
     */
    public void execute()
        throws TaskException
    {
        final int count = m_fileSets.size();
        for( int i = 0; i < count; i++ )
        {
            final FileSet fileSet = (FileSet)m_fileSets.get(i );
            FileSetResult result = fileSet.getResult( getContext() );
            final FileObject[] files = result.getFiles();
            final String[] paths = result.getPaths();
            for( int j = 0; j < files.length; j++ )
            {
                final FileObject file = files[ j ];
                final String path = paths[ j ];
                getLogger().info( path + " = " + file );
            }
        }
    }
}
