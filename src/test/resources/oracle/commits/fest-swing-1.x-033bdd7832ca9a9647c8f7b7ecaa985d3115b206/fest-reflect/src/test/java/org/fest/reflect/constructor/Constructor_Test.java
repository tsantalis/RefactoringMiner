/*
 * Created on May 17, 2007
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
 * Copyright @2007-2009 the original author or authors.
 */
package org.fest.reflect.constructor;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.fest.reflect.Person;
import org.fest.reflect.exception.ReflectionError;
import org.junit.Test;

/**
 * Tests for the fluent interface for constructors.
 *
 * @author Alex Ruiz
 * @author Yvonne Wang
 */
public class Constructor_Test {

  @Test
  public void should_create_new_instance_with_default_constructor() {
    Person person = new TargetType().in(Person.class).newInstance();
    assertThat(person).isNotNull();
    assertThat(person.getName()).isNull();
  }

  @Test
  public void should_create_new_instance_using_given_constructor_parameters() {
    Person person = new TargetType().withParameterTypes(String.class).in(Person.class).newInstance("Yoda");
    assertThat(person).isNotNull();
    assertThat(person.getName()).isEqualTo("Yoda");
  }

  @Test
  public void should_return_real_constructor() {
    Constructor<Person> constructor = new TargetType().withParameterTypes(String.class).in(Person.class).info();
    assertThat(constructor).isNotNull();
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    assertThat(parameterTypes).hasSize(1);
    assertThat(parameterTypes[0]).isEqualTo(String.class);
  }

  @Test(expected = ReflectionError.class)
  public void should_throw_error_if_constructor_was_not_found() {
    Class<Integer> illegalType = Integer.class;
    new TargetType().withParameterTypes(illegalType).in(Person.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_error_if_instance_was_not_created() {
    int illegalArg = 8;
    new TargetType().withParameterTypes(String.class).in(Person.class).newInstance(illegalArg);
  }
}
