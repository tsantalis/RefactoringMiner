/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.factories;

import org.apache.aut.vfs.FileSystemManager;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.interfaces.service.AntServiceException;
import org.apache.myrmidon.interfaces.service.ServiceFactory;

/**
 * A factory that creates the {@link FileSystemManager} service.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class VfsManagerFactory
    implements ServiceFactory
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( VfsManagerFactory.class );

    /**
     * Create a service that coresponds to this factory.
     */
    public Object createService()
        throws AntServiceException
    {
        try
        {
            return new VfsManager();
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "create-vfs-manager.error" );
            throw new AntServiceException( message );
        }
    }
}
