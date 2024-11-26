/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

/**
 * A data container for information parsed from an absolute URI.
 *
 * @author Adam Murdoch
 */
public class ParsedUri
{
    private String m_scheme;
    private String m_rootURI;
    private String m_path;
    private String m_userInfo;
    private String m_hostName;
    private String m_port;

    /** Returns the scheme. */
    public String getScheme()
    {
        return m_scheme;
    }

    /** Sets the scheme. */
    public void setScheme( String scheme )
    {
        m_scheme = scheme;
    }

    /** Returns the root URI, used to identify the file system. */
    public String getRootUri()
    {
        return m_rootURI;
    }

    /** Sets the root URI. */
    public void setRootUri( String rootPrefix )
    {
        m_rootURI = rootPrefix;
    }

    /** Returns the user info part of the URI. */
    public String getUserInfo()
    {
        return m_userInfo;
    }

    /** Sets the user info part of the URI. */
    public void setUserInfo( String userInfo )
    {
        m_userInfo = userInfo;
    }

    /** Returns the host name part of the URI. */
    public String getHostName()
    {
        return m_hostName;
    }

    /** Sets the host name part of the URI. */
    public void setHostName( String hostName )
    {
        m_hostName = hostName;
    }

    /** Returns the port part of the URI. */
    public String getPort()
    {
        return m_port;
    }

    /** Sets the port part of the URI. */
    public void setPort( String port )
    {
        m_port = port;
    }

    /** Returns the path part of the URI.  */
    public String getPath()
    {
        return m_path;
    }

    /** Sets the path part of the URI. */
    public void setPath( String absolutePath )
    {
        m_path = absolutePath;
    }
}
