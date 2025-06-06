// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.testrunner;

import java.io.Closeable;
import fitnesse.testsystems.TestSystemListener;

public interface ResultsListener extends TestsRunnerListener, TestSystemListener<WikiTestPage>, Closeable {

}
