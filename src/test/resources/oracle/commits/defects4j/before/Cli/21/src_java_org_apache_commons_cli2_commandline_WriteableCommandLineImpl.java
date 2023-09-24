/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.cli2.commandline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.WriteableCommandLine;
import org.apache.commons.cli2.option.PropertyOption;
import org.apache.commons.cli2.resource.ResourceConstants;
import org.apache.commons.cli2.resource.ResourceHelper;

/**
 * A WriteableCommandLine implementation allowing Options to write their
 * processed information to a CommandLine.
 */
public class WriteableCommandLineImpl
    extends CommandLineImpl implements WriteableCommandLine {
    private final Map optionToProperties = new HashMap();
//    private final Properties properties = new Properties();
    private final List options = new ArrayList();
    private final Map nameToOption = new HashMap();
    private final Map values = new HashMap();
    private final Map switches = new HashMap();
    private final Map defaultValues = new HashMap();
    private final Map defaultSwitches = new HashMap();
    private final List normalised;
    private final Set prefixes;

    /**
     * Creates a new WriteableCommandLineImpl rooted on the specified Option, to
     * hold the parsed arguments.
     *
     * @param rootOption the CommandLine's root Option
     * @param arguments the arguments this CommandLine represents
     */
    public WriteableCommandLineImpl(final Option rootOption,
                                    final List arguments) {
        this.prefixes = rootOption.getPrefixes();
        this.normalised = arguments;
    }



    public void addOption(Option option) {
        options.add(option);
        nameToOption.put(option.getPreferredName(), option);

        for (Iterator i = option.getTriggers().iterator(); i.hasNext();) {
            nameToOption.put(i.next(), option);
        }

        // ensure that all parent options are also added
        Option parent = option.getParent();
        while (parent != null && !options.contains(parent)) {
            options.add(parent);
            parent = parent.getParent();
        }
    }

    public void addValue(final Option option,
                         final Object value) {
        if (option instanceof Argument) {
            addOption(option);
        }

        List valueList = (List) values.get(option);

        if (valueList == null) {
            valueList = new ArrayList();
            values.put(option, valueList);
        }

        valueList.add(value);
    }

    public void addSwitch(final Option option,
                          final boolean value) {
        addOption(option);

        if (switches.containsKey(option)) {
            throw new IllegalStateException(ResourceHelper.getResourceHelper().getMessage(ResourceConstants.SWITCH_ALREADY_SET));
        } else {
            switches.put(option, value ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    public boolean hasOption(final Option option) {
        final boolean present = options.contains(option);

        return present;
    }

    public Option getOption(final String trigger) {
        return (Option) nameToOption.get(trigger);
    }

    public List getValues(final Option option,
                          List defaultValues) {
        // initialize the return list
        List valueList = (List) values.get(option);

        // grab the correct default values
        if (defaultValues == null || defaultValues.isEmpty()) {
            defaultValues = (List) this.defaultValues.get(option);
        }

        // augment the list with the default values
        if (defaultValues != null && !defaultValues.isEmpty()) {
            if (valueList == null || valueList.isEmpty()) {
                valueList = defaultValues;
            } else {
                // if there are more default values as specified, add them to
                // the list.
                if (defaultValues.size() > valueList.size()) {
                    // copy the list first
                    valueList = new ArrayList(valueList);
                    for (int i=valueList.size(); i<defaultValues.size(); i++) {
                        valueList.add(defaultValues.get(i));
                    }
                }
            }
        }

        return valueList == null ? Collections.EMPTY_LIST : valueList;
    }

    public List getUndefaultedValues(Option option) {
      // First grab the command line values
      List valueList = (List) values.get(option);

      // Finally use an empty list
      if (valueList == null) {
        valueList = Collections.EMPTY_LIST;
      }

      return valueList;
    }

    public Boolean getSwitch(final Option option,
                             final Boolean defaultValue) {
        // First grab the command line values
        Boolean bool = (Boolean) switches.get(option);

        // Secondly try the defaults supplied to the method
        if (bool == null) {
            bool = defaultValue;
        }

        // Thirdly try the option's default values
        if (bool == null) {
            bool = (Boolean) this.defaultSwitches.get(option);
        }

        return bool;
    }

    public String getProperty(final String property) {
        return getProperty(new PropertyOption(), property);
    }

    public void addProperty(final Option option,
                            final String property,
                            final String value) {
        Properties properties = (Properties) optionToProperties.get(option);
        if (properties == null) {
            properties = new Properties();
            optionToProperties.put(option, properties);
        }
        properties.setProperty(property, value);
    }

    public void addProperty(final String property, final String value) {
        addProperty(new PropertyOption(), property, value);
    }

    public String getProperty(final Option option,
                              final String property,
                              final String defaultValue) {
        Properties properties = (Properties) optionToProperties.get(option);
        if (properties == null) {
            return defaultValue;
        }
        return properties.getProperty(property, defaultValue);
    }

    public Set getProperties(final Option option) {
        Properties properties = (Properties) optionToProperties.get(option);
        if (properties == null) {
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableSet(properties.keySet());
    }

    public Set getProperties() {
        return getProperties(new PropertyOption());
    }

    /**
     * Tests whether the passed in trigger looks like an option. This
     * implementation first checks whether the passed in string starts with a
     * prefix that indicates an option. If this is the case, it is also checked
     * whether an option of this name is known for the current option. (This can
     * lead to reentrant invocations of this method, so care has to be taken
     * about this.)
     *
     * @param trigger the command line element to test
     * @return a flag whether this element seems to be an option
     */
    public boolean looksLikeOption(final String trigger)
    {
            // this is a reentrant call

            for (final Iterator i = prefixes.iterator(); i.hasNext();)
            {
                final String prefix = (String) i.next();

                if (trigger.startsWith(prefix))
                {
                        return true;
                }
            }
            return false;
    }

    public String toString() {
        final StringBuffer buffer = new StringBuffer();

        // need to add group header
        for (final Iterator i = normalised.iterator(); i.hasNext();) {
            final String arg = (String) i.next();

            if (arg.indexOf(' ') >= 0) {
                buffer.append("\"").append(arg).append("\"");
            } else {
                buffer.append(arg);
            }

            if (i.hasNext()) {
                buffer.append(' ');
            }
        }

        return buffer.toString();
    }

    public List getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Set getOptionTriggers() {
        return Collections.unmodifiableSet(nameToOption.keySet());
    }

    public void setDefaultValues(final Option option,
                                 final List defaults) {
        if (defaults == null) {
            defaultValues.remove(option);
        } else {
            defaultValues.put(option, defaults);
        }
    }

    public void setDefaultSwitch(final Option option,
                                 final Boolean defaultSwitch) {
        if (defaultSwitch == null) {
            defaultSwitches.remove(option);
        } else {
            defaultSwitches.put(option, defaultSwitch);
        }
    }

    public List getNormalised() {
        return Collections.unmodifiableList(normalised);
    }
}
