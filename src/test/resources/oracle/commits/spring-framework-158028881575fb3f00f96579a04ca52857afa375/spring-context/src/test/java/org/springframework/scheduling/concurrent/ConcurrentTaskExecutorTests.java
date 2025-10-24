/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.concurrent;

import junit.framework.TestCase;

import org.springframework.core.task.NoOpRunnable;

/**
 * @author Rick Evans
 */
public class ConcurrentTaskExecutorTests extends TestCase {

	public void testZeroArgCtorResultsInDefaultTaskExecutorBeingUsed() throws Exception {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
		// must not throw a NullPointerException
		executor.execute(new NoOpRunnable());
	}

	public void testPassingNullExecutorToCtorResultsInDefaultTaskExecutorBeingUsed() throws Exception {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(null);
		// must not throw a NullPointerException
		executor.execute(new NoOpRunnable());
	}

	public void testPassingNullExecutorToSetterResultsInDefaultTaskExecutorBeingUsed() throws Exception {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
		executor.setConcurrentExecutor(null);
		// must not throw a NullPointerException
		executor.execute(new NoOpRunnable());
	}

}
