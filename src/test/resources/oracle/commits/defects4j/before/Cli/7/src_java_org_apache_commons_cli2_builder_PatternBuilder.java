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
package org.apache.commons.cli2.builder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.validation.ClassValidator;
import org.apache.commons.cli2.validation.DateValidator;
import org.apache.commons.cli2.validation.FileValidator;
import org.apache.commons.cli2.validation.NumberValidator;
import org.apache.commons.cli2.validation.UrlValidator;
import org.apache.commons.cli2.validation.Validator;

/**
 * Builds Options using a String pattern
 */
//TODO Document and link to the acceptable patterns
public class PatternBuilder {

    private final GroupBuilder gbuilder;
    private final DefaultOptionBuilder obuilder;
    private final ArgumentBuilder abuilder;

    /**
     * Creates a new PatternBuilder
     */
    public PatternBuilder() {
        this(
            new GroupBuilder(),
            new DefaultOptionBuilder(),
            new ArgumentBuilder());
    }

    /**
     * Creates a new PatternBuilder
     * @param gbuilder the GroupBuilder to use
     * @param obuilder the DefaultOptionBuilder to use
     * @param abuilder the ArgumentBuilder to use
     */
    public PatternBuilder(
        final GroupBuilder gbuilder,
        final DefaultOptionBuilder obuilder,
        final ArgumentBuilder abuilder) {
        this.gbuilder = gbuilder;
        this.obuilder = obuilder;
        this.abuilder = abuilder;
    }

    private final Set options = new HashSet();

    /**
     * Creates a new Option instance.
     * @return a new Option instance
     */
    public Option create() {
        final Option option;

        if (options.size() == 1) {
            option = (Option)options.iterator().next();
        }
        else {
            gbuilder.reset();
            for (final Iterator i = options.iterator(); i.hasNext();) {
                gbuilder.withOption((Option)i.next());
            }
            option = gbuilder.create();
        }

        reset();

        return option;
    }

    /**
     * Resets this builder
     */
    public PatternBuilder reset() {
        options.clear();
        return this;
    }

    private void createOption(
        final char type,
        final boolean required,
        final char opt) {
        final Argument argument;
        if (type != ' ') {
            abuilder.reset();
            abuilder.withValidator(validator(type));
            if (required) {
                abuilder.withMinimum(1);
            }
            if (type != '*') {
                abuilder.withMaximum(1);
            }
            argument = abuilder.create();
        }
        else {
            argument = null;
        }

        obuilder.reset();
        obuilder.withArgument(argument);
        obuilder.withShortName(String.valueOf(opt));
        obuilder.withRequired(required);

        options.add(obuilder.create());
    }

    /**
     * Builds an Option using a pattern string.
     * @param pattern the pattern to build from
     */
    public void withPattern(final String pattern) {
        int sz = pattern.length();

        char opt = ' ';
        char ch = ' ';
        char type = ' ';
        boolean required = false;

        for (int i = 0; i < sz; i++) {
            ch = pattern.charAt(i);

            switch (ch) {
                case '!' :
                    required = true;
                    break;
                case '@' :
                case ':' :
                case '%' :
                case '+' :
                case '#' :
                case '<' :
                case '>' :
                case '*' :
                case '/' :
                    type = ch;
                    break;
                default :
                    if (opt != ' ') {
                        createOption(type, required, opt);
                        required = false;
                        type = ' ';
                    }

                    opt = ch;
            }
        }

        if (opt != ' ') {
            createOption(type, required, opt);
        }
    }

    private static Validator validator(final char c) {
        switch (c) {
            case '@' :
                final ClassValidator classv = new ClassValidator();
                classv.setInstance(true);
                return classv;
            case '+' :
                final ClassValidator instancev = new ClassValidator();
                return instancev;
                //case ':':// no validator needed for a string
            case '%' :
                return NumberValidator.getNumberInstance();
            case '#' :
                return DateValidator.getDateInstance();
            case '<' :
                final FileValidator existingv = new FileValidator();
                existingv.setExisting(true);
                existingv.setFile(true);
                return existingv;
            case '>' :
            case '*' :
                return new FileValidator();
            case '/' :
                return new UrlValidator();
            default :
                return null;
        }
    }
}
