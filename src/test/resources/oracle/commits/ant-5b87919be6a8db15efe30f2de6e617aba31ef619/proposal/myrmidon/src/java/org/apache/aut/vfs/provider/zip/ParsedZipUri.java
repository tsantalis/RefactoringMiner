/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.zip;

import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A parsed Zip URI.
 *
 * @author Adam Murdoch
 */
public class ParsedZipUri extends ParsedUri
{
    private String m_zipFile;

    public String getZipFile()
    {
        return m_zipFile;
    }

    public void setZipFile( String zipFile )
    {
        m_zipFile = zipFile;
    }
}
