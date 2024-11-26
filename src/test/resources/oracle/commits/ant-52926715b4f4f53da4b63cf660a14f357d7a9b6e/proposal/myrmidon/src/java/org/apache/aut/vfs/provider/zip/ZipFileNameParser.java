/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.zip;

import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.UriParser;

/**
 * A parser for Zip file names.
 *
 * @author Adam Murdoch
 */
public class ZipFileNameParser
    extends UriParser
{
    private static final char[] ZIP_URL_RESERVED_CHARS = { '!' };

    /**
     * Parses an absolute URI, splitting it into its components.
     *
     * @param uriStr
     *          The URI.
     */
    public ParsedZipUri parseZipUri( final String uriStr )
        throws FileSystemException
    {
        final StringBuffer name = new StringBuffer();
        final ParsedZipUri uri = new ParsedZipUri();

        // Extract the scheme
        final String scheme = extractScheme( uriStr, name );
        uri.setScheme( scheme );

        // Extract the Zip file name
        final String zipName = extractZipName( name );
        uri.setZipFileName( zipName );

        // Decode and normalise the file name
        decode( name, 0, name.length() );
        normalisePath( name );
        uri.setPath( name.toString() );

        return uri;
    }

    /**
     * Assembles a root URI from the components of a parsed URI.
     */
    public String buildRootUri( final ParsedZipUri uri )
    {
        final StringBuffer rootUri = new StringBuffer();
        rootUri.append( uri.getScheme() );
        rootUri.append( ":" );
        appendEncoded( rootUri, uri.getZipFile().getName().getURI(), ZIP_URL_RESERVED_CHARS );
        rootUri.append( "!" );
        return rootUri.toString();
    }

    /**
     * Pops the root prefix off a URI, which has had the scheme removed.
     */
    private String extractZipName( final StringBuffer uri )
        throws FileSystemException
    {
        // Looking for <name>!<abspath>
        int maxlen = uri.length();
        int pos = 0;
        for( ; pos < maxlen && uri.charAt( pos ) != '!'; pos++ )
        {
        }

        // Extract the name
        String prefix = uri.substring( 0, pos );
        if( pos < maxlen )
        {
            uri.delete( 0, pos + 1 );
        }
        else
        {
            uri.setLength( 0 );
        }

        // Decode the name
        return decode( prefix );
    }
}
