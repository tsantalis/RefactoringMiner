/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.zip;

import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.ParsedUri;
import org.apache.aut.vfs.provider.UriParser;

/**
 * A parser for Zip file names.
 *
 * @author Adam Murdoch
 */
public class ZipFileNameParser extends UriParser
{
    /**
     * Parses an absolute URI, splitting it into its components.
     *
     * @param name
     *          The URI.
     */
    public ParsedUri parseUri( String uriStr ) throws FileSystemException
    {
        StringBuffer name = new StringBuffer();
        ParsedZipUri uri = new ParsedZipUri();

        // Extract the scheme
        String scheme = extractScheme( uriStr, name );
        uri.setScheme( scheme );

        // Extract the Zip file name
        String zipName = extractZipName( name );
        uri.setZipFile( zipName );

        // Adjust the separators
        fixSeparators( name );

        // Normalise the file name
        normalisePath( name );
        uri.setPath( name.toString() );

        // Build root URI
        StringBuffer rootUri = new StringBuffer();
        rootUri.append( scheme );
        rootUri.append( ":" );
        rootUri.append( zipName );
        rootUri.append( "!" );
        uri.setRootURI( rootUri.toString() );

        return uri;
    }

    /**
     * Pops the root prefix off a URI, which has had the scheme removed.
     */
    protected String extractZipName( StringBuffer uri ) throws FileSystemException
    {
        // Looking for <name>!<abspath>
        // TODO - how does '!' in the file name get escaped?
        int maxlen = uri.length();
        for( int pos = 0; pos < maxlen; pos++ )
        {
            if( uri.charAt( pos ) == '!' )
            {
                String prefix = uri.substring( 0, pos );
                uri.delete( 0, pos + 1 );
                return prefix;
            }
        }

        // Assume the URI is the Jar file name
        String prefix = uri.toString();
        uri.setLength( 0 );
        return prefix;
    }
}
