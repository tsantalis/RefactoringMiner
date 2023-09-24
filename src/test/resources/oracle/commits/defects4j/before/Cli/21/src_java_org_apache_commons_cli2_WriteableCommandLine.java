/**
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
package org.apache.commons.cli2;

import java.util.List;

/**
 * A CommandLine that detected values and options can be written to.
 */
public interface WriteableCommandLine extends CommandLine {

    /**
     * Adds an Option to the CommandLine
     * @param option the Option to add
     */
    void addOption(final Option option);

    /**
     * Adds a value to an Option in the CommandLine.
     * @param option the Option to add to
     * @param value the value to add
     */
    void addValue(final Option option, final Object value);

    /**
     * Retrieves the Argument values specified on the command line for the
     * specified Option, this doesn't return any values supplied
     * programmatically as defaults.
     *
     * @param option the Option associated with the values
     * @return a list of values or an empty List if none are found
     */
    List getUndefaultedValues(final Option option);

    /**
     * Sets the default values for an Option in the CommandLine
     * @param option the Option to add to
     * @param defaultValues the defaults for the option
     */
    void setDefaultValues(final Option option, final List defaultValues);

    /**
     * Adds a switch value to an Option in the CommandLine.
     * @param option the Option to add to
     * @param value the switch value to add
     * @throws IllegalStateException if the switch has already been added
     */
    void addSwitch(final Option option, final boolean value) throws IllegalStateException;

    /**
     * Sets the default state for a Switch in the CommandLine.
     * @param option the Option to add to
     * @param defaultSwitch the defaults state for ths switch
     */
    void setDefaultSwitch(final Option option, final Boolean defaultSwitch);

    /**
     * Adds a property value to a name in the CommandLine.
     * Replaces any existing value for the property.
     *
     * @param option the Option to add to
     * @param property the name of the property
     * @param value the value of the property
     */
    void addProperty(final Option option, final String property, final String value);

    /**
     * Adds a property value to the default property set.
     * Replaces any existing value for the property.
     *
     * @param property the name of the property
     * @param value the value of the property
     */
    void addProperty(final String property, final String value);

    /**
     * Detects whether the argument looks like an Option trigger
     * @param argument the argument to test
     * @return true if the argument looks like an Option trigger
     */
    boolean looksLikeOption(final String argument);

    /**
     * Returns the option that is currently processed.
     *
     * @return the current option
     */

    /**
     * Sets the current option. This method is called by concrete option
     * implementations during command line processing. It enables the command
     * line to keep track about the option that is currently processed.
     *
     * @param currentOption the new current option
     */
}
