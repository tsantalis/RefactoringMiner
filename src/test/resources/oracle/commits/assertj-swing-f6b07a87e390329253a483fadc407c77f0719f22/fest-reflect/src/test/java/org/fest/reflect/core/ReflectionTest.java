/*
 * Created on May 18, 2007
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
package org.fest.reflect.core;

import org.testng.annotations.Test;

import org.fest.reflect.constructor.TargetType;
import org.fest.reflect.field.FieldName;
import org.fest.reflect.field.StaticFieldName;
import org.fest.reflect.method.MethodName;
import org.fest.reflect.method.StaticMethodName;
import org.fest.reflect.type.Type;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for <code>{@link Reflection}</code>.
 *
 * @author Alex Ruiz
 * @author Yvonne Wang
 */
@Test public class ReflectionTest {

  public void shouldReturnConstructorFluentInterface() {
    assertThat(Reflection.constructor()).isInstanceOf(TargetType.class);
  }
  
  public void shouldReturnFieldFluentInterface() {
    assertThat(Reflection.field("field")).isInstanceOf(FieldName.class);
  }

  public void shouldReturnStaticFieldFluentInterface() {
    assertThat(Reflection.staticField("field")).isInstanceOf(StaticFieldName.class);
  }

  public void shouldReturnMethodFluentInterface() {
    assertThat(Reflection.method("method")).isInstanceOf(MethodName.class);
  }

  public void shouldReturnStaticMethodFluentInterface() {
    assertThat(Reflection.staticMethod("method")).isInstanceOf(StaticMethodName.class);
  }
  
  public void shouldReturnTypeFluentInterface() {
    assertThat(Reflection.type("type")).isInstanceOf(Type.class);
  }
}
