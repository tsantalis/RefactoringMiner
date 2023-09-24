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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.DisplaySetting;
import org.apache.commons.cli2.HelpLine;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.WriteableCommandLine;
import org.apache.commons.cli2.resource.ResourceConstants;
import org.apache.commons.cli2.resource.ResourceHelper;
import org.apache.commons.cli2.validation.InvalidArgumentException;
import org.apache.commons.cli2.validation.Validator;

/**
 * An implementation of an Argument.
 */
public class ArgumentImpl
    extends OptionImpl implements Argument {
    private static final char NUL = '\0';

    /**
     * The default value for the initial separator char.
     */
    public static final char DEFAULT_INITIAL_SEPARATOR = NUL;

    /**
     * The default value for the subsequent separator char.
     */
    public static final char DEFAULT_SUBSEQUENT_SEPARATOR = NUL;

    /**
     * The default token to indicate that remaining arguments should be consumed
     * as values.
     */
    public static final String DEFAULT_CONSUME_REMAINING = "--";
    private final String name;
    private final String description;
    private final int minimum;
    private final int maximum;
    private final char initialSeparator;
    private final char subsequentSeparator;
    private final boolean subsequentSplit;
    private final Validator validator;
    private final String consumeRemaining;
    private final List defaultValues;
    private final ResourceHelper resources = ResourceHelper.getResourceHelper();

    /**
     * Creates a new Argument instance.
     *
     * @param name
     *            The name of the argument
     * @param description
     *            A description of the argument
     * @param minimum
     *            The minimum number of values needed to be valid
     * @param maximum
     *            The maximum number of values allowed to be valid
     * @param initialSeparator
     *            The char separating option from value
     * @param subsequentSeparator
     *            The char separating values from each other
     * @param validator
     *            The object responsible for validating the values
     * @param consumeRemaining
     *            The String used for the "consuming option" group
     * @param valueDefaults
     *            The values to be used if none are specified.
     * @param id
     *            The id of the option, 0 implies automatic assignment.
     *
     * @see OptionImpl#OptionImpl(int,boolean)
     */
    public ArgumentImpl(final String name,
                        final String description,
                        final int minimum,
                        final int maximum,
                        final char initialSeparator,
                        final char subsequentSeparator,
                        final Validator validator,
                        final String consumeRemaining,
                        final List valueDefaults,
                        final int id) {
        super(id, false);

        this.name = (name == null) ? "arg" : name;
        this.description = description;
        this.minimum = minimum;
        this.maximum = maximum;
        this.initialSeparator = initialSeparator;
        this.subsequentSeparator = subsequentSeparator;
        this.subsequentSplit = subsequentSeparator != NUL;
        this.validator = validator;
        this.consumeRemaining = consumeRemaining;
        this.defaultValues = valueDefaults;

        if (minimum > maximum) {
            throw new IllegalArgumentException(resources.getMessage(ResourceConstants.ARGUMENT_MIN_EXCEEDS_MAX));
        }

        if ((valueDefaults != null) && (valueDefaults.size() > 0)) {
            if (valueDefaults.size() < minimum) {
                throw new IllegalArgumentException(resources.getMessage(ResourceConstants.ARGUMENT_TOO_FEW_DEFAULTS));
            }

            if (valueDefaults.size() > maximum) {
                throw new IllegalArgumentException(resources.getMessage(ResourceConstants.ARGUMENT_TOO_MANY_DEFAULTS));
            }
        }
    }

    public String getPreferredName() {
        return name;
    }

    public void processValues(final WriteableCommandLine commandLine,
                              final ListIterator arguments,
                              final Option option)
        throws OptionException {
        // count of arguments processed for this option.
        int argumentCount = 0;

        while (arguments.hasNext() && (argumentCount < maximum)) {
            final String allValuesQuoted = (String) arguments.next();
            final String allValues = stripBoundaryQuotes(allValuesQuoted);

            // should we ignore things that look like options?
            if (allValuesQuoted.equals(consumeRemaining)) {
                while (arguments.hasNext() && (argumentCount < maximum)) {
                    ++argumentCount;
                    commandLine.addValue(option, arguments.next());
                }
            }
            // does it look like an option?
            else if (commandLine.looksLikeOption(allValuesQuoted)) {
                arguments.previous();

                break;
            }
            // should we split the string up?
            else if (subsequentSplit) {
                final StringTokenizer values =
                    new StringTokenizer(allValues, String.valueOf(subsequentSeparator));

                arguments.remove();

                while (values.hasMoreTokens() && (argumentCount < maximum)) {
                    ++argumentCount;

                    final String token = values.nextToken();
                    commandLine.addValue(option, token);
                    arguments.add(token);
                }

                if (values.hasMoreTokens()) {
                    throw new OptionException(option, ResourceConstants.ARGUMENT_UNEXPECTED_VALUE,
                                              values.nextToken());
                }
            }
            // it must be a value as it is
            else {
                ++argumentCount;
                commandLine.addValue(option, allValues);
            }
        }
    }

    public boolean canProcess(final WriteableCommandLine commandLine,
                              final String arg) {
        return true;
    }

    public Set getPrefixes() {
        return Collections.EMPTY_SET;
    }

    public void process(WriteableCommandLine commandLine,
                        ListIterator args)
        throws OptionException {
        processValues(commandLine, args, this);
    }

    public char getInitialSeparator() {
        return this.initialSeparator;
    }

    public char getSubsequentSeparator() {
        return this.subsequentSeparator;
    }

    public Set getTriggers() {
        return Collections.EMPTY_SET;
    }

    public String getConsumeRemaining() {
        return this.consumeRemaining;
    }

    public List getDefaultValues() {
        return this.defaultValues;
    }

    public Validator getValidator() {
        return this.validator;
    }

    public void validate(final WriteableCommandLine commandLine)
        throws OptionException {
        validate(commandLine, this);
    }

    public void validate(final WriteableCommandLine commandLine,
                         final Option option)
        throws OptionException {
        final List values = commandLine.getValues(option);

        if (values.size() < minimum) {
            throw new OptionException(option, ResourceConstants.ARGUMENT_MISSING_VALUES);
        }

        if (values.size() > maximum) {
            throw new OptionException(option, ResourceConstants.ARGUMENT_UNEXPECTED_VALUE,
                                      (String) values.get(maximum));
        }

        if (validator != null) {
            try {
                validator.validate(values);
            } catch (InvalidArgumentException ive) {
                throw new OptionException(option, ResourceConstants.ARGUMENT_UNEXPECTED_VALUE,
                                          ive.getMessage());
            }
        }
    }

    public void appendUsage(final StringBuffer buffer,
                            final Set helpSettings,
                            final Comparator comp) {
        // do we display the outer optionality
        final boolean optional = helpSettings.contains(DisplaySetting.DISPLAY_OPTIONAL);

        // allow numbering if multiple args
        final boolean numbered =
            (maximum > 1) && helpSettings.contains(DisplaySetting.DISPLAY_ARGUMENT_NUMBERED);

        final boolean bracketed = helpSettings.contains(DisplaySetting.DISPLAY_ARGUMENT_BRACKETED);

        // if infinite args are allowed then crop the list
        final int max = (maximum == Integer.MAX_VALUE) ? 2 : maximum;

        int i = 0;

        // for each argument
        while (i < max) {
            // if we're past the first add a space
            if (i > 0) {
                buffer.append(' ');
            }

            // if the next arg is optional
            if ((i >= minimum) && (optional || (i > 0))) {
                buffer.append('[');
            }

            if (bracketed) {
                buffer.append('<');
            }

            // add name
            buffer.append(name);
            ++i;

            // if numbering
            if (numbered) {
                buffer.append(i);
            }

            if (bracketed) {
                buffer.append('>');
            }
        }

        // if infinite args are allowed
        if (maximum == Integer.MAX_VALUE) {
            // append elipsis
            buffer.append(" ...");
        }

        // for each argument
        while (i > 0) {
            --i;

            // if the next arg is optional
            if ((i >= minimum) && (optional || (i > 0))) {
                buffer.append(']');
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public List helpLines(final int depth,
                          final Set helpSettings,
                          final Comparator comp) {
        final HelpLine helpLine = new HelpLineImpl(this, depth);

        return Collections.singletonList(helpLine);
    }

    public int getMaximum() {
        return maximum;
    }

    public int getMinimum() {
        return minimum;
    }

    /**
     * If there are any leading or trailing quotes remove them from the
     * specified token.
     *
     * @param token
     *            the token to strip leading and trailing quotes
     *
     * @return String the possibly modified token
     */
    public String stripBoundaryQuotes(String token) {
        if (!token.startsWith("\"") || !token.endsWith("\"")) {
            return token;
        }

        token = token.substring(1, token.length() - 1);

        return token;
    }

    public boolean isRequired() {
        return getMinimum() > 0;
    }

    public void defaults(final WriteableCommandLine commandLine) {
        super.defaults(commandLine);
        defaultValues(commandLine, this);
    }

    public void defaultValues(final WriteableCommandLine commandLine,
                              final Option option) {
        commandLine.setDefaultValues(option, defaultValues);
    }
}
