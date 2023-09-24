/*
 * Copyright 1999-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.cli;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Date;

import org.apache.commons.lang.math.NumberUtils;
/**
  * This is a temporary implementation. TypeHandler will handle the 
  * pluggableness of OptionTypes and it will direct all of these types 
  * of conversion functionalities to ConvertUtils component in Commons 
  * alreayd. BeanUtils I think.
  *
  * @author Henri Yandell (bayard @ generationjava.com)
  * @version $Revision$
  */
public class TypeHandler {

    /**
     * <p>Returns the <code>Object</code> of type <code>obj</code>
     * with the value of <code>str</code>.</p>
     *
     * @param str the command line value
     * @param obj the type of argument
     * @return The instance of <code>obj</code> initialised with
     * the value of <code>str</code>.
     */
    public static Object createValue(String str, Object obj)
    {
        return createValue(str, (Class) obj);
    }

    /**
     * <p>Returns the <code>Object</code> of type <code>clazz</code>
     * with the value of <code>str</code>.</p>
     *
     * @param str the command line value
     * @param clazz the type of argument
     * @return The instance of <code>clazz</code> initialised with
     * the value of <code>str</code>.
     */
    public static Object createValue(String str, Class clazz)
    {
        if (PatternOptionBuilder.STRING_VALUE == clazz)
        {
            return str;
        }
        else if (PatternOptionBuilder.OBJECT_VALUE == clazz)
        {
            return createObject(str);
        }
        else if (PatternOptionBuilder.NUMBER_VALUE == clazz)
        {
            return createNumber(str);
        }
        else if (PatternOptionBuilder.DATE_VALUE == clazz)
        {
            return createDate(str);
        }
        else if (PatternOptionBuilder.CLASS_VALUE == clazz)
        {
            return createClass(str);
        }
        else if (PatternOptionBuilder.FILE_VALUE == clazz)
        {
            return createFile(str);
        }
        else if (PatternOptionBuilder.EXISTING_FILE_VALUE == clazz)
        {
            return createFile(str);
        }
        else if (PatternOptionBuilder.FILES_VALUE == clazz)
        {
            return createFiles(str);
        }
        else if (PatternOptionBuilder.URL_VALUE == clazz)
        {
            return createURL(str);
        }
        else
        {
            return null;
        }
    }

    /**
      * <p>Create an Object from the classname and empty constructor.</p>
      *
      * @param str the argument value
      * @return the initialised object, or null if it couldn't create 
      * the Object.
      */
    public static Object createObject(String str)
    {
        Class cl = null;

        try
        {
            cl = Class.forName(str);
        }
        catch (ClassNotFoundException cnfe)
        {
            System.err.println("Unable to find: " + str);

            return null;
        }

        Object instance = null;

        try
        {
            instance = cl.newInstance();
        }
        catch (InstantiationException cnfe)
        {
            System.err.println("InstantiationException; Unable to create: "
                               + str);

            return null;
        }
        catch (IllegalAccessException cnfe)
        {
            System.err.println("IllegalAccessException; Unable to create: "
                               + str);

            return null;
        }

        return instance;
    }

    /**
     * <p>Create a number from a String. If a . is present, it creates a 
     *    Double, otherwise a Long. </p>
     *
     * @param str the value
     * @return the number represented by <code>str</code>, if <code>str</code>
     * is not a number, null is returned.
     */
    public static Number createNumber(String str)
    {
        try
        {
            return NumberUtils.createNumber(str);
        }
        catch (NumberFormatException nfe)
        {
            System.err.println(nfe.getMessage());
        }

        return null;
    }

    /**
     * <p>Returns the class whose name is <code>str</code>.</p>
     *
     * @param str the class name
     * @return The class if it is found, otherwise return null
     */
    public static Class createClass(String str)
    {
        try
        {
            return Class.forName(str);
        }
        catch (ClassNotFoundException cnfe)
        {
            System.err.println("Unable to find: " + str);

            return null;
        }
    }

    /**
     * <p>Returns the date represented by <code>str</code>.</p>
     *
     * @param str the date string
     * @return The date if <code>str</code> is a valid date string,
     * otherwise return null.
     */
    public static Date createDate(String str)
    {
        Date date = null;

        if (date == null)
        {
            System.err.println("Unable to parse: " + str);
        }

        return date;
    }

    /**
     * <p>Returns the URL represented by <code>str</code>.</p>
     *
     * @param str the URL string
     * @return The URL is <code>str</code> is well-formed, otherwise
     * return null.
     */
    public static URL createURL(String str)
    {
        try
        {
            return new URL(str);
        }
        catch (MalformedURLException mue)
        {
            System.err.println("Unable to parse: " + str);

            return null;
        }
    }

    /**
     * <p>Returns the File represented by <code>str</code>.</p>
     *
     * @param str the File location
     * @return The file represented by <code>str</code>.
     */
    public static File createFile(String str)
    {
        return new File(str);
    }

    /**
     * <p>Returns the File[] represented by <code>str</code>.</p>
     *
     * @param str the paths to the files
     * @return The File[] represented by <code>str</code>.
     */
    public static File[] createFiles(String str)
    {
        // to implement/port:
        //        return FileW.findFiles(str);
        return null;
    }
}
