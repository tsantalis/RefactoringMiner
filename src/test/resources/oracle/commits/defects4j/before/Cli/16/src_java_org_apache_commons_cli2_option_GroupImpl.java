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
package org.apache.commons.cli2.option;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.DisplaySetting;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.HelpLine;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.WriteableCommandLine;
import org.apache.commons.cli2.resource.ResourceConstants;

/**
 * An implementation of Group
 */
public class GroupImpl
    extends OptionImpl implements Group {
    private final String name;
    private final String description;
    private final List options;
    private final int minimum;
    private final int maximum;
    private final List anonymous;
    private final SortedMap optionMap;
    private final Set prefixes;

    /**
     * Creates a new GroupImpl using the specified parameters.
     *
     * @param options the Options and Arguments that make up the Group
     * @param name the name of this Group, or null
     * @param description a description of this Group
     * @param minimum the minimum number of Options for a valid CommandLine
     * @param maximum the maximum number of Options for a valid CommandLine
     */
    public GroupImpl(final List options,
                     final String name,
                     final String description,
                     final int minimum,
                     final int maximum) {
        super(0, false);

        this.name = name;
        this.description = description;
        this.minimum = minimum;
        this.maximum = maximum;

        // store a copy of the options to be used by the
        // help methods
        this.options = Collections.unmodifiableList(options);

        // anonymous Argument temporary storage
        final List newAnonymous = new ArrayList();

        // map (key=trigger & value=Option) temporary storage
        final SortedMap newOptionMap = new TreeMap(ReverseStringComparator.getInstance());

        // prefixes temporary storage
        final Set newPrefixes = new HashSet();

        // process the options
        for (final Iterator i = options.iterator(); i.hasNext();) {
            final Option option = (Option) i.next();

            if (option instanceof Argument) {
                i.remove();
                newAnonymous.add(option);
            } else {
                final Set triggers = option.getTriggers();

                for (Iterator j = triggers.iterator(); j.hasNext();) {
                    newOptionMap.put(j.next(), option);
                }

                // store the prefixes
                newPrefixes.addAll(option.getPrefixes());
            }
        }

        this.anonymous = Collections.unmodifiableList(newAnonymous);
        this.optionMap = Collections.unmodifiableSortedMap(newOptionMap);
        this.prefixes = Collections.unmodifiableSet(newPrefixes);
    }

    public boolean canProcess(final WriteableCommandLine commandLine,
                              final String arg) {
        if (arg == null) {
            return false;
        }

        // if arg does not require bursting
        if (optionMap.containsKey(arg)) {
            return true;
        }

        // filter
        final Map tailMap = optionMap.tailMap(arg);

        // check if bursting is required
        for (final Iterator iter = tailMap.values().iterator(); iter.hasNext();) {
            final Option option = (Option) iter.next();

            if (option.canProcess(commandLine, arg)) {
                return true;
            }
        }

        if (commandLine.looksLikeOption(arg)) {
            return false;
        }

        // anonymous argument(s) means we can process it
        if (anonymous.size() > 0) {
            return true;
        }

        return false;
    }

    public Set getPrefixes() {
        return prefixes;
    }

    public Set getTriggers() {
        return optionMap.keySet();
    }

    public void process(final WriteableCommandLine commandLine,
                        final ListIterator arguments)
        throws OptionException {
        String previous = null;

        // [START process each command line token
        while (arguments.hasNext()) {
            // grab the next argument
            final String arg = (String) arguments.next();

            // if we have just tried to process this instance
            if (arg == previous) {
                // rollback and abort
                arguments.previous();

                break;
            }

            // remember last processed instance
            previous = arg;

            final Option opt = (Option) optionMap.get(arg);

            // option found
            if (opt != null) {
                arguments.previous();
                opt.process(commandLine, arguments);
            }
            // [START option NOT found
            else {
                // it might be an anonymous argument continue search
                // [START argument may be anonymous
                if (commandLine.looksLikeOption(arg)) {
                    // narrow the search
                    final Collection values = optionMap.tailMap(arg).values();

                    boolean foundMemberOption = false;

                    for (Iterator i = values.iterator(); i.hasNext() && !foundMemberOption;) {
                        final Option option = (Option) i.next();

                        if (option.canProcess(commandLine, arg)) {
                            foundMemberOption = true;
                            arguments.previous();
                            option.process(commandLine, arguments);
                        }
                    }

                    // back track and abort this group if necessary
                    if (!foundMemberOption) {
                        arguments.previous();

                        return;
                    }
                } // [END argument may be anonymous

                // [START argument is NOT anonymous
                else {
                    // move iterator back, current value not used
                    arguments.previous();

                    // if there are no anonymous arguments then this group can't
                    // process the argument
                    if (anonymous.isEmpty()) {
                        break;
                    }

                    // TODO: why do we iterate over all anonymous arguments?
                    // canProcess will always return true?
                    for (final Iterator i = anonymous.iterator(); i.hasNext();) {
                        final Argument argument = (Argument) i.next();

                        if (argument.canProcess(commandLine, arguments)) {
                            argument.process(commandLine, arguments);
                        }
                    }
                } // [END argument is NOT anonymous
            } // [END option NOT found
        } // [END process each command line token
    }

    public void validate(final WriteableCommandLine commandLine)
        throws OptionException {
        // number of options found
        int present = 0;

        // reference to first unexpected option
        Option unexpected = null;

        for (final Iterator i = options.iterator(); i.hasNext();) {
            final Option option = (Option) i.next();

            // needs validation?
            boolean validate = option.isRequired() || option instanceof Group;

            // if the child option is present then validate it
            if (commandLine.hasOption(option)) {
                if (++present > maximum) {
                    unexpected = option;

                    break;
                }
                validate = true;
            }

            if (validate) {
                option.validate(commandLine);
            }
        }

        // too many options
        if (unexpected != null) {
            throw new OptionException(this, ResourceConstants.UNEXPECTED_TOKEN,
                                      unexpected.getPreferredName());
        }

        // too few option
        if (present < minimum) {
            throw new OptionException(this, ResourceConstants.MISSING_OPTION);
        }

        // validate each anonymous argument
        for (final Iterator i = anonymous.iterator(); i.hasNext();) {
            final Option option = (Option) i.next();
            option.validate(commandLine);
        }
    }

    public String getPreferredName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void appendUsage(final StringBuffer buffer,
                            final Set helpSettings,
                            final Comparator comp) {
        appendUsage(buffer, helpSettings, comp, "|");
    }

    public void appendUsage(final StringBuffer buffer,
                            final Set helpSettings,
                            final Comparator comp,
                            final String separator) {
        final Set helpSettingsCopy = new HashSet(helpSettings);

        final boolean optional =
            (minimum == 0) && helpSettingsCopy.contains(DisplaySetting.DISPLAY_OPTIONAL);

        final boolean expanded =
            (name == null) || helpSettingsCopy.contains(DisplaySetting.DISPLAY_GROUP_EXPANDED);

        final boolean named =
            !expanded ||
            ((name != null) && helpSettingsCopy.contains(DisplaySetting.DISPLAY_GROUP_NAME));

        final boolean arguments = helpSettingsCopy.contains(DisplaySetting.DISPLAY_GROUP_ARGUMENT);

        final boolean outer = helpSettingsCopy.contains(DisplaySetting.DISPLAY_GROUP_OUTER);

        helpSettingsCopy.remove(DisplaySetting.DISPLAY_GROUP_OUTER);

        final boolean both = named && expanded;

        if (optional) {
            buffer.append('[');
        }

        if (named) {
            buffer.append(name);
        }

        if (both) {
            buffer.append(" (");
        }

        if (expanded) {
            final Set childSettings;

            if (!helpSettingsCopy.contains(DisplaySetting.DISPLAY_GROUP_EXPANDED)) {
                childSettings = DisplaySetting.NONE;
            } else {
                childSettings = new HashSet(helpSettingsCopy);
                childSettings.remove(DisplaySetting.DISPLAY_OPTIONAL);
            }

            // grab a list of the group's options.
            final List list;

            if (comp == null) {
                // default to using the initial order
                list = options;
            } else {
                // sort options if comparator is supplied
                list = new ArrayList(options);
                Collections.sort(list, comp);
            }

            // for each option.
            for (final Iterator i = list.iterator(); i.hasNext();) {
                final Option option = (Option) i.next();

                // append usage information
                option.appendUsage(buffer, childSettings, comp);

                // add separators as needed
                if (i.hasNext()) {
                    buffer.append(separator);
                }
            }
        }

        if (both) {
            buffer.append(')');
        }

        if (optional && outer) {
            buffer.append(']');
        }

        if (arguments) {
            for (final Iterator i = anonymous.iterator(); i.hasNext();) {
                buffer.append(' ');

                final Option option = (Option) i.next();
                option.appendUsage(buffer, helpSettingsCopy, comp);
            }
        }

        if (optional && !outer) {
            buffer.append(']');
        }
    }

    public List helpLines(final int depth,
                          final Set helpSettings,
                          final Comparator comp) {
        final List helpLines = new ArrayList();

        if (helpSettings.contains(DisplaySetting.DISPLAY_GROUP_NAME)) {
            final HelpLine helpLine = new HelpLineImpl(this, depth);
            helpLines.add(helpLine);
        }

        if (helpSettings.contains(DisplaySetting.DISPLAY_GROUP_EXPANDED)) {
            // grab a list of the group's options.
            final List list;

            if (comp == null) {
                // default to using the initial order
                list = options;
            } else {
                // sort options if comparator is supplied
                list = new ArrayList(options);
                Collections.sort(list, comp);
            }

            // for each option
            for (final Iterator i = list.iterator(); i.hasNext();) {
                final Option option = (Option) i.next();
                helpLines.addAll(option.helpLines(depth + 1, helpSettings, comp));
            }
        }

        if (helpSettings.contains(DisplaySetting.DISPLAY_GROUP_ARGUMENT)) {
            for (final Iterator i = anonymous.iterator(); i.hasNext();) {
                final Option option = (Option) i.next();
                helpLines.addAll(option.helpLines(depth + 1, helpSettings, comp));
            }
        }

        return helpLines;
    }

    /**
     * Gets the member Options of thie Group.
     * Note this does not include any Arguments
     * @return only the non Argument Options of the Group
     */
    public List getOptions() {
        return options;
    }

    /**
     * Gets the anonymous Arguments of this Group.
     * @return the Argument options of this Group
     */
    public List getAnonymous() {
        return anonymous;
    }

    public Option findOption(final String trigger) {
        final Iterator i = getOptions().iterator();

        while (i.hasNext()) {
            final Option option = (Option) i.next();
            final Option found = option.findOption(trigger);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    public int getMinimum() {
        return minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    public boolean isRequired() {
        return getMinimum() > 0;
    }

    public void defaults(final WriteableCommandLine commandLine) {
        super.defaults(commandLine);

        for (final Iterator i = options.iterator(); i.hasNext();) {
            final Option option = (Option) i.next();
            option.defaults(commandLine);
        }

        for (final Iterator i = anonymous.iterator(); i.hasNext();) {
            final Option option = (Option) i.next();
            option.defaults(commandLine);
        }
    }
}


class ReverseStringComparator implements Comparator {
    private static final Comparator instance = new ReverseStringComparator();

    private ReverseStringComparator() {
        // just making sure nobody else creates one
    }

    /**
     * Gets a singleton instance of a ReverseStringComparator
     * @return the singleton instance
     */
    public static final Comparator getInstance() {
        return instance;
    }

    public int compare(final Object o1,
                       final Object o2) {
        final String s1 = (String) o1;
        final String s2 = (String) o2;

        return -s1.compareTo(s2);
    }
}
