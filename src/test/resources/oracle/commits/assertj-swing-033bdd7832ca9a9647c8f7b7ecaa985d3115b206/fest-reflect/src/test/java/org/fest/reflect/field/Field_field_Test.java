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
package org.fest.reflect.field;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.util.ExpectedFailures.*;
import static org.fest.util.Collections.list;

import java.util.List;

import org.fest.reflect.Jedi;
import org.fest.reflect.Person;
import org.fest.reflect.reference.TypeRef;
import org.fest.test.CodeToTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the fluent interface for accessing fields.
 *
 * @author Alex Ruiz
 */
public class Field_field_Test {

  private Person person;

  @Before
  public void setUp() {
    person = new Person("Luke");
  }

  @Test
  public void should_throw_error_if_field_name_is_null() {
    expectNullPointerException("The name of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName(null);
      }
    });
  }

  @Test
  public void should_throw_error_if_field_name_is_empty() {
    expectIllegalArgumentException("The name of the field to access should not be empty").on(new CodeToTest() {
      public void run() {
        new FieldName("");
      }
    });
  }

  @Test
  public void should_throw_error_if_field_type_is_null() {
    expectNullPointerException("The type of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName("name").ofType((Class<?>)null);
      }
    });
  }

  @Test
  public void should_throw_error_if_target_is_null() {
    expectNullPointerException("Target should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName("name").ofType(String.class).in(null);
      }
    });
  }

  @Test
  public void should_get_field_value() {
    String personName = new FieldName("name").ofType(String.class).in(person).get();
    assertThat(personName).isEqualTo("Luke");
  }

  @Test
  public void should_set_field_value() {
    new FieldName("name").ofType(String.class).in(person).set("Leia");
    assertThat(person.getName()).isEqualTo("Leia");
  }

  @Test
  public void should_return_real_field() {
    java.lang.reflect.Field field = new FieldName("name").ofType(String.class).in(person).info();
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("name");
    assertThat(field.getType()).isEqualTo(String.class);
  }

  @Test
  public void should_throw_error_if_wrong_field_type_was_specified() {
    String message =
      "The type of the field 'name' in org.fest.reflect.Person should be <java.lang.Integer> but was <java.lang.String>";
    expectReflectionError(message).on(new CodeToTest() {
      public void run()  {
        new FieldName("name").ofType(Integer.class).in(person).get();
      }
    });
  }

  @Test
  public void should_throw_error_if_field_name_is_invalid() {
    expectReflectionError("Unable to find field 'age' in org.fest.reflect.Person").on(new CodeToTest() {
      public void run()  {
        new FieldName("age").ofType(Integer.class).in(person);
      }
    });
  }

  @Test
  public void should_get_field_in_super_type() {
    Jedi jedi = new Jedi("Yoda");
    String jediName = new FieldName("name").ofType(String.class).in(jedi).get();
    assertThat(jediName).isEqualTo("Yoda");
  }

  @Test
  public void should_throw_error_if_TypeRef_is_null() {
    expectNullPointerException("The type reference of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName("name").ofType((TypeRef<?>)null);
      }
    });
  }

  @Test
  public void should_use_TypeRef_to_read_field() {
    Jedi jedi = new Jedi("Yoda");
    jedi.addPower("heal");
    List<String> powers = new FieldName("powers").ofType(new TypeRef<List<String>>() {}).in(jedi).get();
    assertThat(powers).containsOnly("heal");
  }

  @Test
  public void should_use_TypeRef_to_write_field() {
    Jedi jedi = new Jedi("Yoda");
    List<String> powers = list("heal");
    new FieldName("powers").ofType(new TypeRef<List<String>>() {}).in(jedi).set(powers);
    assertThat(jedi.powers()).containsOnly("heal");
  }
}
