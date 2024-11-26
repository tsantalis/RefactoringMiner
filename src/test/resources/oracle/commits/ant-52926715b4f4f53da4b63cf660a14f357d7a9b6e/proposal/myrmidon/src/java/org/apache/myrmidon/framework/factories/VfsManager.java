/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.factories;

import org.apache.aut.vfs.impl.DefaultFileSystemManager;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.FileSystemException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeException;

/**
 * The myrmidon FileSystemManager implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class VfsManager
    extends DefaultFileSystemManager
    implements Serviceable, Initializable, Disposable
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( VfsManager.class );

    private TypeFactory m_typeFactory;

    /**
     * Locate the services used by this service.
     */
    public void service( final ServiceManager serviceManager ) throws ServiceException
    {
        final TypeManager typeManager = (TypeManager)serviceManager.lookup( TypeManager.ROLE );
        try
        {
            m_typeFactory = typeManager.getFactory( FileSystemProvider.class );
        }
        catch( TypeException e )
        {
            throw new ServiceException( e.getMessage(), e );
        }
    }

    /**
     * Initialises this service.
     */
    public void initialize() throws Exception
    {
        // TODO - make this list configurable

        // Required providers
        addProvider( new String[] { "zip", "jar" }, "zip", false );

        // Optional providers
        addProvider( new String[] { "smb" }, "smb", true );
        addProvider( new String[] { "ftp" }, "ftp", true );
    }

    /**
     * Disposes this service.
     */
    public void dispose()
    {
        // Clean-up
        close();
    }

    /**
     * Registers a file system provider.
     */
    public void addProvider( final String[] urlSchemes,
                             final String providerName,
                             final boolean ignoreIfNotPresent )
        throws FileSystemException
    {
        // Create an instance
        if( ignoreIfNotPresent && ! m_typeFactory.canCreate( providerName ) )
        {
            return;
        }

        final FileSystemProvider provider;
        try
        {
            provider = (FileSystemProvider)m_typeFactory.create( providerName );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "create-provider.error", providerName );
            throw new FileSystemException( message, e );
        }

        // Register the provider
        addProvider( urlSchemes, provider );
    }

}
