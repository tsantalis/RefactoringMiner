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

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.cli2.DisplaySetting;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.WriteableCommandLine;
import org.apache.commons.cli2.resource.ResourceConstants;
import org.apache.commons.cli2.resource.ResourceHelper;

/**
 * A base implementation of Option providing limited ground work for further
 * Option implementations.
 */
public abstract class OptionImpl implements Option {
    private final int id;
    private final boolean required;

    /**
     * Creates an OptionImpl with the specified id
     * @param id the unique id of this Option
     * @param required true iff this Option must be present
     */
    public OptionImpl(final int id,
                      final boolean required) {
        this.id = id;
        this.required = required;
    }

    public boolean canProcess(final WriteableCommandLine commandLine,
                              final ListIterator arguments) {
        if (arguments.hasNext()) {
            final String argument = (String) arguments.next();
            arguments.previous();

            return canProcess(commandLine, argument);
        } else {
            return false;
        }
    }

    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        appendUsage(buffer, DisplaySetting.ALL, null);

        return buffer.toString();
    }

    public int getId() {
        return id;
    }

    public boolean equals(final Object thatObj) {
        if (thatObj instanceof OptionImpl) {
            final OptionImpl that = (OptionImpl) thatObj;

            return (getId() == that.getId()) &&
                   equals(getPreferredName(), that.getPreferredName()) &&
                   equals(getDescription(), that.getDescription()) &&
                   equals(getPrefixes(), that.getPrefixes()) &&
                   equals(getTriggers(), that.getTriggers());
        } else {
            return false;
        }
    }

    private boolean equals(Object left,
                           Object right) {
        if ((left == null) && (right == null)) {
            return true;
        } else if ((left == null) || (right == null)) {
            return false;
        } else {
            return left.equals(right);
        }
    }

    public int hashCode() {
        int hashCode = getId();
        if (getPreferredName() != null) {
            hashCode = (hashCode * 37) + getPreferredName().hashCode();
        }

        if (getDescription() != null) {
            hashCode = (hashCode * 37) + getDescription().hashCode();
        }

        hashCode = (hashCode * 37) + getPrefixes().hashCode();
        hashCode = (hashCode * 37) + getTriggers().hashCode();

        return hashCode;
    }

    public Option findOption(String trigger) {
        if (getTriggers().contains(trigger)) {
            return this;
        } else {
            return null;
        }
    }

    public boolean isRequired() {
        return required;
    }

    public void defaults(final WriteableCommandLine commandLine) {
        // nothing to do normally
    }



    protected void checkPrefixes(final Set prefixes) {
        // nothing to do if empty prefix list
        if (prefixes.isEmpty()) {
            return;
        }

        // check preferred name
        checkPrefix(prefixes, getPreferredName());

        // check triggers
        this.getTriggers();

        for (final Iterator i = getTriggers().iterator(); i.hasNext();) {
            checkPrefix(prefixes, (String) i.next());
        }
    }

    private void checkPrefix(final Set prefixes,
                             final String trigger) {
        for (final Iterator i = prefixes.iterator(); i.hasNext();) {
            String prefix = (String) i.next();

            if (trigger.startsWith(prefix)) {
                return;
            }
        }

        final ResourceHelper helper = ResourceHelper.getResourceHelper();
        final String message =
            helper.getMessage(ResourceConstants.OPTION_TRIGGER_NEEDS_PREFIX, trigger,
                              prefixes.toString());
        throw new IllegalArgumentException(message);
    }
}
