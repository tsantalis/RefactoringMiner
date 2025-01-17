/*
 * Created on Jan 23, 2009
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
package org.fest.reflect.type;

import org.fest.reflect.Jedi;
import org.fest.reflect.Person;
import org.fest.reflect.exception.ReflectionError;
import org.fest.test.CodeToTest;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.util.ExpectedFailures.*;

/**
 * Tests for <code>{@link Type}</code>.
 *
 * @author Alex Ruiz
 */
public class Type_Test {

  @Test
  public void should_throw_error_if__type_name_is_null() {
    expectNullPointerException("The name of the class to load should not be null").on(new CodeToTest() {
      public void run() {
        new Type(null);
      }
    });
  }

  @Test
  public void should_throw_error_if__type_name_is_empty() {
    expectIllegalArgumentException("The name of the class to load should not be empty").on(new CodeToTest() {
      public void run() {
        new Type("");
      }
    });
  }

  @Test
  public void should_throw_error_if_subtype_is_mull() {
    expectNullPointerException("The given type should not be null").on(new CodeToTest() {
      public void run() {
        new Type("hello").loadAs(null);
      }
    });
  }

  @Test
  public void should_load_class() {
    Class<Jedi> expected = Jedi.class;
    Class<?> type = new Type(expected.getName()).load();
    assertThat(type).isEqualTo(expected);
  }

  @Test
  public void should_load_class_with_given_ClassLoader() {
    Class<Jedi> expected = Jedi.class;
    Class<?> type = new Type(expected.getName()).withClassLoader(getClass().getClassLoader()).load();
    assertThat(type).isEqualTo(expected);
  }

  @Test
  public void should_throw_error_if_Classloader_is_null() {
    expectNullPointerException("The given class loader should not be null").on(new CodeToTest() {
      public void run() {
        new Type("hello").withClassLoader(null);
      }
    });
  }

  @Test
  public void should_wrap_any_Exception_thrown_when_loading_class() {
    try {
      new Type("org.fest.reflect.NonExistingType").load();
    } catch (ReflectionError expected) {
      assertThat(expected.getMessage()).contains(
          "Unable to load class 'org.fest.reflect.NonExistingType' using class loader ");
      assertThat(expected.getCause()).isInstanceOf(ClassNotFoundException.class);
    }
  }

  @Test
  public void should_load_class_as_given_type() {
    Class<? extends Person> type = new Type(Jedi.class.getName()).loadAs(Person.class);
    assertThat(type).isEqualTo(Jedi.class);
  }

  @Test
  public void should_load_class_as_given_type_with_given_ClassLoader() {
    Class<? extends Person> type = new Type(Jedi.class.getName()).withClassLoader(getClass().getClassLoader())
                                                                 .loadAs(Person.class);
    assertThat(type).isEqualTo(Jedi.class);
  }

  @Test
  public void should_wrap_any_Exception_thrown_when_loading_class_as_given_type() {
    try {
      new Type("org.fest.reflect.NonExistingType").loadAs(Jedi.class);
    } catch (ReflectionError expected) {
      assertThat(expected.getMessage()).contains(
          "Unable to load class 'org.fest.reflect.NonExistingType' as org.fest.reflect.Jedi using class loader ");
      assertThat(expected.getCause()).isInstanceOf(ClassNotFoundException.class);
    }
  }
}
