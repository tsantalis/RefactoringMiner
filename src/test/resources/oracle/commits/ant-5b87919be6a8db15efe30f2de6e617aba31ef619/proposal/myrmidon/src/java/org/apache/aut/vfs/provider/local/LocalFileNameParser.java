/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.local;

import java.io.File;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.ParsedUri;
import org.apache.aut.vfs.provider.UriParser;

/**
 * A name parser.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
abstract class LocalFileNameParser
    extends UriParser
{
    public LocalFileNameParser()
    {
        super( new char[]{File.separatorChar, '/', '\\'} );
    }

    /**
     * Determines if a name is an absolute file name.
     */
    public boolean isAbsoluteName( final String name )
    {
        // TODO - this is yucky
        StringBuffer b = new StringBuffer( name );
        try
        {
            fixSeparators( b );
            extractRootPrefix( name, b );
            return true;
        }
        catch( FileSystemException e )
        {
            return false;
        }
    }

    /**
     * Parses an absolute URI, splitting it into its components.
     *
     * @param uriStr The URI.
     */
    public ParsedUri parseUri( final String uriStr )
        throws FileSystemException
    {
        StringBuffer name = new StringBuffer();
        ParsedFileUri uri = new ParsedFileUri();

        // Extract the scheme
        String scheme = extractScheme( uriStr, name );
        uri.setScheme( scheme );

        // Adjust the separators
        fixSeparators( name );

        // Extract the root prefix
        String rootFile = extractRootPrefix( uriStr, name );
        uri.setRootFile( rootFile );

        // Normalise the path
        normalisePath( name );
        uri.setPath( name.toString() );

        // Build the root URI
        StringBuffer rootUri = new StringBuffer();
        rootUri.append( scheme );
        rootUri.append( "://" );
        rootUri.append( rootFile );
        uri.setRootURI( rootUri.toString() );

        return uri;
    }

    /**
     * Pops the root prefix off a URI, which has had the scheme removed.
     */
    protected abstract String extractRootPrefix( final String uri,
                                                 final StringBuffer name )
        throws FileSystemException;

}
