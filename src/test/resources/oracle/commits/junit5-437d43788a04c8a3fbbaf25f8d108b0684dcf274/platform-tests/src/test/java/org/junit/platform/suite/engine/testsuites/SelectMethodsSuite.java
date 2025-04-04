/*
 * Copyright 2015-2023 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.testsuites;

import org.junit.platform.suite.api.SelectMethod;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.engine.testcases.MultipleTestsTestCase;

/**
 * @since 1.10
 */
@Suite
@SelectMethod(clazz = MultipleTestsTestCase.class, name = "test")
public class SelectMethodsSuite {
}
