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

import java.util.List;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.fest.reflect.Jedi;
import org.fest.reflect.Person;
import org.fest.reflect.reference.TypeRef;
import org.fest.test.CodeToTest;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.util.ExpectedFailures.*;
import static org.fest.util.Collections.list;

/**
 * Tests for the fluent interface for fields and static fields.
 *
 * @author Alex Ruiz
 */
@Test public class FieldTest {
  
  private Person person;
  
  @BeforeTest public void setUp() {
    person = new Person("Luke");
  }
  
  public void shouldThrowErrorIfFieldNameIsNullOrEmpty() {
    expectNullPointerException("The name of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName(null);
      }
    });
  }
  
  public void shouldThrowErrorIfFieldNameIsEmpty() {
    expectIllegalArgumentException("The name of the field to access should not be empty").on(new CodeToTest() {
      public void run() {
        new FieldName("");
      }
    });
  }

  public void shouldThrowErrorIfFieldTypeIsNull() {
    expectNullPointerException("The type of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName("name").ofType((Class<?>)null);
      }      
    });
  }
  
  public void shouldThrowErrorIfTargetIsNullWhenAccessingField() {
    expectNullPointerException("Target should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName("name").ofType(String.class).in(null);
      }      
    });
  }

  public void shouldGetFieldValue() {
    String personName = new FieldName("name").ofType(String.class).in(person).get();
    assertThat(personName).isEqualTo("Luke");
  }
  
  public void shouldSetFieldValue() {
    new FieldName("name").ofType(String.class).in(person).set("Leia");
    assertThat(person.getName()).isEqualTo("Leia");
  }
  
  public void shouldReturnFieldInfo() {
    java.lang.reflect.Field field = new FieldName("name").ofType(String.class).in(person).info();
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("name");
    assertThat(field.getType()).isEqualTo(String.class);
  }
  
  public void shouldThrowErrorIfWrongFieldTypeSpecified() {
    String message = 
      "The type of the field 'name' in org.fest.reflect.Person should be <java.lang.Integer> but was <java.lang.String>";
    expectReflectionError(message).on(new CodeToTest() {
      public void run()  {
        new FieldName("name").ofType(Integer.class).in(person).get();
      }
    });
  }

  public void shouldThrowErrorIfInvalidFieldName() {
    expectReflectionError("Unable to find field 'age' in org.fest.reflect.Person").on(new CodeToTest() {
      public void run()  {
        new FieldName("age").ofType(Integer.class).in(person);
      }
    });
  }
  
  public void shouldGetFieldInSuperType() {
    Jedi jedi = new Jedi("Yoda");
    String jediName = new FieldName("name").ofType(String.class).in(jedi).get();
    assertThat(jediName).isEqualTo("Yoda");
  }
  
  public void shouldThrowErrorIfFieldTypeReferenceIsNull() {
    expectNullPointerException("The type reference of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new FieldName("name").ofType((TypeRef<?>)null);
      }      
    });
  }

  public void shouldUseTypeReferenceWhenReadingField() {
    Jedi jedi = new Jedi("Yoda");
    jedi.addPower("heal");
    List<String> powers = new FieldName("powers").ofType(new TypeRef<List<String>>() {}).in(jedi).get();
    assertThat(powers).containsOnly("heal");
  }
  
  public void shouldUseTypeReferenceWhenWritingField() {
    Jedi jedi = new Jedi("Yoda");
    List<String> powers = list("heal");
    new FieldName("powers").ofType(new TypeRef<List<String>>() {}).in(jedi).set(powers);
    assertThat(jedi.powers()).containsOnly("heal");
  }

  public void shouldGetStaticFieldValue() {
    Person.setCount(6);
    int count = new StaticFieldName("count").ofType(int.class).in(Person.class).get();
    assertThat(count).isEqualTo(6);
  }
  
  public void shouldSetStaticFieldValue() {
    new StaticFieldName("count").ofType(int.class).in(Person.class).set(8);
    assertThat(Person.getCount()).isEqualTo(8);
  }
  
  public void shouldReturnStaticFieldInfo() {
    java.lang.reflect.Field field = new StaticFieldName("count").ofType(int.class).in(Person.class).info();
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("count");
    assertThat(field.getType()).isEqualTo(int.class);
  }
  
  public void shouldThrowErrorIfStaticFieldNameIsNullOrEmpty() {
    expectNullPointerException("The name of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new StaticFieldName(null);
      }
    });
  }

  public void shouldThrowErrorIfStaticFieldNameIsEmpty() {
    expectIllegalArgumentException("The name of the field to access should not be empty").on(new CodeToTest() {
      public void run() {
        new StaticFieldName("");
      }
    });
  }

  public void shouldThrowErrorIfStaticFieldTypeIsNull() {
    expectNullPointerException("The type of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new StaticFieldName("name").ofType((Class<?>)null);
      }      
    });
  }

  public void shouldThrowErrorIfWrongStaticFieldTypeSpecified() {
    String message = "The type of the field 'count' in org.fest.reflect.Person should be <java.lang.Float> but was <int>";
    expectReflectionError(message).on(new CodeToTest() {
      public void run()  {
        new StaticFieldName("count").ofType(Float.class).in(Person.class).get();
      }
    });
  }

  public void shouldThrowErrorIfInvalidStaticFieldName() {
    expectReflectionError("Unable to find field 'age' in org.fest.reflect.Person").on(new CodeToTest() {
      public void run()  {
        new StaticFieldName("age").ofType(int.class).in(Person.class);
      }
    });
  }
  
  public void shouldGetStaticFieldInSuperType() {
    Person.setCount(8);
    int count = new StaticFieldName("count").ofType(int.class).in(Person.class).get();
    assertThat(count).isEqualTo(8);
  }

  public void shouldThrowErrorIfStaticFieldTypeReferenceIsNull() {
    expectNullPointerException("The type reference of the field to access should not be null").on(new CodeToTest() {
      public void run() {
        new StaticFieldName("name").ofType((TypeRef<?>)null);
      }      
    });
  }

  public void shouldUseTypeReferenceWhenReadingStaticField() {
    Jedi.addCommonPower("jump");
    List<String> powers = new StaticFieldName("commonPowers").ofType(new TypeRef<List<String>>() {}).in(Jedi.class).get();
    assertThat(powers).containsOnly("jump");
  }
  
  public void shouldUseTypeReferenceWhenWritingStaticField() {
    List<String> powers = list("jump");
    new StaticFieldName("commonPowers").ofType(new TypeRef<List<String>>() {}).in(Jedi.class).set(powers);
    assertThat(Jedi.commonPowers()).containsOnly("jump");
  }

  public void shouldThrowErrorIfTargetIsNullWhenAccessingStaticField() {
    expectNullPointerException("Target should not be null").on(new CodeToTest() {
      public void run() {
        new StaticFieldName("age").ofType(int.class).in(null);
      }      
    });
  }
}
