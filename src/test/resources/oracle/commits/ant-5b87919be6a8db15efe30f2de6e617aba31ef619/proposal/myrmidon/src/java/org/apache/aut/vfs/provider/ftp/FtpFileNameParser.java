/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.ftp;

import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.ParsedUri;
import org.apache.aut.vfs.provider.UriParser;

/**
 * A parser for FTP URI.
 *
 * @author Adam Murdoch
 */
public class FtpFileNameParser extends UriParser
{
    /**
     * Parses an absolute URI, splitting it into its components.
     */
    public ParsedUri parseUri( String uriStr ) throws FileSystemException
    {
        ParsedFtpUri uri = new ParsedFtpUri();

        // FTP URI are generic URI (as per RFC 2396)
        parseGenericUri( uriStr, uri );

        // Split up the userinfo into a username and password
        String userInfo = uri.getUserInfo();
        if( userInfo != null )
        {
            int idx = userInfo.indexOf( ':' );
            if( idx == -1 )
            {
                uri.setUserName( userInfo );
            }
            else
            {
                String userName = userInfo.substring( 0, idx );
                String password = userInfo.substring( idx + 1 );
                uri.setUserName( userName );
                uri.setPassword( password );
            }
        }

        return uri;
    }
}
