/**
 * Copyright 1999-2001,2004 The Apache Software Foundation.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** 
 * <p>Represents list of arguments parsed against
 * a {@link Options} descriptor.<p>
 *
 * <p>It allows querying of a boolean {@link #hasOption(String opt)},
 * in addition to retrieving the {@link #getOptionValue(String opt)}
 * for options requiring arguments.</p>
 *
 * <p>Additionally, any left-over or unrecognized arguments,
 * are available for further processing.</p>
 *
 * @author bob mcwhirter (bob @ werken.com)
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @author John Keyes (john at integralsource.com)
 */
public class CommandLine {

    /** the unrecognised options/arguments */
    private List args = new LinkedList();

    /** the processed options */
    private Map options = new HashMap();
    private Map names = new HashMap();

    /** Map of unique options for ease to get complete list of options */
//    private Set allOptions = new HashSet();
    private Map hashcodeMap = new HashMap();

    /**
     * Creates a command line.
     */
    CommandLine()
    {
        // nothing to do
    }

    /** 
     * Query to see if an option has been set.
     *
     * @param opt Short name of the option
     * @return true if set, false if not
     */
    public boolean hasOption(String opt)
    {
        return options.containsKey(opt);
    }

    /** 
     * Query to see if an option has been set.
     *
     * @param opt character name of the option
     * @return true if set, false if not
     */
    public boolean hasOption(char opt)
    {
        return hasOption(String.valueOf(opt));
    }

    /**
     * Return the <code>Object</code> type of this <code>Option</code>.
     *
     * @param opt the name of the option
     * @return the type of this <code>Option</code>
     */
    public Object getOptionObject(String opt)
    {
        String res = getOptionValue(opt);

        if (!options.containsKey(opt))
        {
            return null;
        }

        Object type = ((Option) options.get(opt)).getType();

        return (res == null)        ? null : TypeHandler.createValue(res, type);
    }

    /**
     * Return the <code>Object</code> type of this <code>Option</code>.
     *
     * @param opt the name of the option
     * @return the type of opt
     */
    public Object getOptionObject(char opt)
    {
        return getOptionObject(String.valueOf(opt));
    }

    /** 
     * Retrieve the argument, if any, of this option.
     *
     * @param opt the name of the option
     * @return Value of the argument if option is set, and has an argument,
     * otherwise null.
     */
    public String getOptionValue(String opt)
    {
        String[] values = getOptionValues(opt);

        return (values == null) ? null : values[0];
    }

    /** 
     * Retrieve the argument, if any, of this option.
     *
     * @param opt the character name of the option
     * @return Value of the argument if option is set, and has an argument,
     * otherwise null.
     */
    public String getOptionValue(char opt)
    {
        return getOptionValue(String.valueOf(opt));
    }

    /** 
     * Retrieves the array of values, if any, of an option.
     *
     * @param opt string name of the option
     * @return Values of the argument if option is set, and has an argument,
     * otherwise null.
     */
    public String[] getOptionValues(String opt)
    {
        opt = Util.stripLeadingHyphens(opt);

        String key = opt;
        if (names.containsKey(opt))

        {
            key = (String) names.get(opt);
        }

        if (options.containsKey(key))
        {
            return ((Option) options.get(key)).getValues();
        }
        return null;
        }

    /**
     * <p>Retrieves the option object given the long or short option as a String</p>
     * @param opt short or long name of the option
     * @return Canonicalized option
     */


    /** 
     * Retrieves the array of values, if any, of an option.
     *
     * @param opt character name of the option
     * @return Values of the argument if option is set, and has an argument,
     * otherwise null.
     */
    public String[] getOptionValues(char opt)
    {
        return getOptionValues(String.valueOf(opt));
    }

    /** 
     * Retrieve the argument, if any, of an option.
     *
     * @param opt name of the option
     * @param defaultValue is the default value to be returned if the option 
     * is not specified
     * @return Value of the argument if option is set, and has an argument,
     * otherwise <code>defaultValue</code>.
     */
    public String getOptionValue(String opt, String defaultValue)
    {
        String answer = getOptionValue(opt);

        return (answer != null) ? answer : defaultValue;
    }

    /** 
     * Retrieve the argument, if any, of an option.
     *
     * @param opt character name of the option
     * @param defaultValue is the default value to be returned if the option 
     * is not specified
     * @return Value of the argument if option is set, and has an argument,
     * otherwise <code>defaultValue</code>.
     */
    public String getOptionValue(char opt, String defaultValue)
    {
        return getOptionValue(String.valueOf(opt), defaultValue);
    }

    /** 
     * Retrieve any left-over non-recognized options and arguments
     *
     * @return remaining items passed in but not parsed as an array
     */
    public String[] getArgs()
    {
        String[] answer = new String[args.size()];

        args.toArray(answer);

        return answer;
    }

    /** 
     * Retrieve any left-over non-recognized options and arguments
     *
     * @return remaining items passed in but not parsed as a <code>List</code>.
     */
    public List getArgList()
    {
        return args;
    }

    /** 
     * jkeyes
     * - commented out until it is implemented properly
     * <p>Dump state, suitable for debugging.</p>
     *
     * @return Stringified form of this object
     */

    /*
    public String toString() {
        StringBuffer buf = new StringBuffer();
            
        buf.append("[ CommandLine: [ options: ");
        buf.append(options.toString());
        buf.append(" ] [ args: ");
        buf.append(args.toString());
        buf.append(" ] ]");
            
        return buf.toString();
    }
    */

    /**
     * Add left-over unrecognized option/argument.
     *
     * @param arg the unrecognised option/argument.
     */
    void addArg(String arg)
    {
        args.add(arg);
    }

    /**
     * Add an option to the command line.  The values of 
     * the option are stored.
     *
     * @param opt the processed option
     */
    void addOption(Option opt)
    {
        hashcodeMap.put(new Integer(opt.hashCode()), opt);
        String key = opt.getKey();
        if (key == null)
        {
            key = opt.getLongOpt();
        }
        else
        {
            names.put(opt.getLongOpt(), key);
        }
        options.put(key, opt);
    }

    /**
     * Returns an iterator over the Option members of CommandLine.
     *
     * @return an <code>Iterator</code> over the processed {@link Option} 
     * members of this {@link CommandLine}
     */
    public Iterator iterator()
    {
        return hashcodeMap.values().iterator();
    }

    /**
     * Returns an array of the processed {@link Option}s.
     *
     * @return an array of the processed {@link Option}s.
     */
    public Option[] getOptions()
    {
        Collection processed = options.values();

        // reinitialise array
        Option[] optionsArray = new Option[processed.size()];

        // return the array
        return (Option[]) processed.toArray(optionsArray);
    }
}
