/*
 * Created on Mar 19, 2009
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright @2009 the original author or authors.
 */
package org.fest.reflect.core;

import static org.fest.reflect.core.Reflection.constructor;
import static org.fest.reflect.core.Reflection.method;

import org.testng.annotations.Test;

/**
 * Tests for <a href="http://jira.codehaus.org/browse/FEST-68" target="_blank">FEST-68</a>.
 *
 * @author Francis ANDRE
 * @author Alex Ruiz
 */
@Test public class FEST68_CatchingWrongExceptions {

  @Test(expectedExceptions = MyRuntimeException.class)
  public void shouldNotCatchRuntimeExceptionWhenCallingMethod() {
    Main main = new Main();
    method("set").in(main).invoke();
  }

  @Test(expectedExceptions = MyRuntimeException.class)
  public void shouldNotCatchRuntimeExceptionWhenCallingConstructor() {
    constructor().withParameterTypes(String.class).in(Main.class).newInstance("Hello");
  }

  static class MyRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public MyRuntimeException(String text) {
      super(text);
    }
  }

  public static class Main {
    public Main() {}

    public Main(String hello) {
      throw new MyRuntimeException("set");
    }

    public void set()  {
      throw new MyRuntimeException("set");
    }
  }
}
