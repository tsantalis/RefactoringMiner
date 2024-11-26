/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.smb;

import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.ParsedUri;
import org.apache.aut.vfs.provider.UriParser;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A parser for SMB URI.
 *
 * @author Adam Murdoch
 */
public class SmbFileNameParser
    extends UriParser
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( SmbFileNameParser.class );

    /**
     * Parses an absolute URI, splitting it into its components.
     */
    public ParsedUri parseSmbUri( final String uriStr )
        throws FileSystemException
    {
        final ParsedSmbUri uri = new ParsedSmbUri();
        final StringBuffer name = new StringBuffer();

        // Extract the scheme and authority parts
        extractToPath( uriStr, name, uri );

        // Convert the hostname to lowercase
        final String hostname = uri.getHostName().toLowerCase();
        uri.setHostName( hostname );

        // TODO - drop the default port

        // Decode and adjust separators
        decode( name, 0, name.length() );
        fixSeparators( name );

        // Extract the share
        final String share = extractFirstElement( name );
        if( share == null )
        {
            final String message = REZ.getString( "missing-share-name.error", uriStr );
            throw new FileSystemException( message );
        }
        uri.setShare( share );

        // Normalise the path
        normalisePath( name );

        // Set the path
        uri.setPath( name.toString() );

        // Set the root URI
        StringBuffer rootUri = new StringBuffer();
        appendRootUri( uri, rootUri );
        rootUri.append( '/' );
        rootUri.append( share );
        uri.setRootUri( rootUri.toString() );

        return uri;
    }
}
