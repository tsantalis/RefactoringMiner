/*
 * Created on Nov 23, 2009
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
package org.fest.reflect.method;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.util.ExpectedFailures.*;

import java.util.List;

import org.fest.reflect.Jedi;
import org.fest.reflect.reference.TypeRef;
import org.fest.test.CodeToTest;
import org.junit.Test;

/**
 * Tests for the fluent interface for accessing static methods.
 *
 * @author Yvonne Wang
 * @author Alex Ruiz
 */
public class Method_staticMethod_Test {

  @Test
  public void should_throw_error_if_static_method_name_is_null() {
    expectNullPointerException("The name of the method to access should not be null").on(new CodeToTest() {
      public void run() {
        new StaticMethodName(null);
      }
    });
  }

  @Test
  public void should_throw_error_if_static_method_name_is_empty() {
    expectIllegalArgumentException("The name of the method to access should not be empty").on(new CodeToTest() {
      public void run() {
        new StaticMethodName("");
      }
    });
  }

  @Test
  public void should_throw_error_if_static_method_return_type_is_null() {
    expectNullPointerException("The return type of the method to access should not be null").on(new CodeToTest() {
      public void run() {
        new StaticMethodName("commonPowerCount").withReturnType((Class<?>)null);
      }
    });
  }

  @Test
  public void should_throw_error_if_static_method_return_TypeRef_is_null() {
    expectNullPointerException("The return type reference of the method to access should not be null").on(
      new CodeToTest() {
        public void run() {
          new StaticMethodName("commonPowerCount").withReturnType((TypeRef<?>)null);
        }
      });
  }

  @Test
  public void should_throw_error_if_static_method_parameter_array_is_null() {
    expectNullPointerException("The array of parameter types should not be null").on(new CodeToTest() {
      public void run() {
        Class<?>[] parameterTypes = null;
        new StaticMethodName("commonPowerCount").withParameterTypes(parameterTypes);
      }
    });
  }

  @Test
  public void should_throw_error_if_static_method_target_is_null() {
    expectNullPointerException("Target should not be null").on(new CodeToTest() {
      public void run() {
        new StaticMethodName("commonPowerCount").in(null);
      }
    });
  }

  @Test
  public void should_call_static_method_with_no_args_and_return_value() {
    Jedi.addCommonPower("Jump");
    int count = new StaticMethodName("commonPowerCount").withReturnType(int.class).in(Jedi.class).invoke();
    assertThat(count).isEqualTo(Jedi.commonPowerCount());
  }

  @Test
  public void should_call_static_method_with_args_and_return_value() {
    Jedi.addCommonPower("Jump");
    String power =
      new StaticMethodName("commonPowerAt").withReturnType(String.class)
                                           .withParameterTypes(int.class).in(Jedi.class).invoke(0);
    assertThat(power).isEqualTo("Jump");
  }

  @Test
  public void should_call_static_method_with_no_args_and_return_TypeRef() {
    Jedi.addCommonPower("jump");
    List<String> powers = new StaticMethodName("commonPowers").withReturnType(new TypeRef<List<String>>() {})
                                                              .in(Jedi.class).invoke();
    assertThat(powers).containsOnly("jump");
  }

  @Test
  public void should_call_static_method_with_args_and_return_TypeRef() {
    Jedi.addCommonPower("jump");
    List<String> powers =
      new StaticMethodName("commonPowersThatStartWith").withReturnType(new TypeRef<List<String>>() {})
                                                       .withParameterTypes(String.class).in(Jedi.class).invoke("ju");
    assertThat(powers).containsOnly("jump");
  }

  @Test
  public void should_call_static_method_with_args_and_no_return_value() {
    new StaticMethodName("addCommonPower").withParameterTypes(String.class).in(Jedi.class).invoke("Jump");
    assertThat(Jedi.commonPowerAt(0)).isEqualTo("Jump");
  }

  @Test
  public void should_call_static_method_with_no_args_and_no_return_value() {
    Jedi.addCommonPower("Jump");
    assertThat(Jedi.commonPowerCount()).isEqualTo(1);
    assertThat(Jedi.commonPowerAt(0)).isEqualTo("Jump");
    new StaticMethodName("clearCommonPowers").in(Jedi.class).invoke();
    assertThat(Jedi.commonPowerCount()).isEqualTo(0);
  }

  @Test
  public void should_throw_error_if_static_method_name_is_invalid() {
    String message = "Unable to find method 'powerSize' in org.fest.reflect.Jedi with parameter type(s) []";
    expectReflectionError(message).on(new CodeToTest() {
      public void run() {
        String invalidName = "powerSize";
        new StaticMethodName(invalidName).in(Jedi.class);
      }
    });
  }

  @Test
  public void should_throw_error_if_args_for_static_method_are_invalid() {
    expectIllegalArgumentException("argument type mismatch").on(new CodeToTest() {
      public void run() {
        int invalidArg = 8;
        new StaticMethodName("addCommonPower").withParameterTypes(String.class).in(Jedi.class).invoke(invalidArg);
      }
    });
  }
}
