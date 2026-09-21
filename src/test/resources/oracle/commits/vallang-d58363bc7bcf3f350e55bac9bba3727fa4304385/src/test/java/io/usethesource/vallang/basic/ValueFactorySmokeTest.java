/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueFactoryProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public final class ValueFactorySmokeTest {
  private TypeFactory ft = TypeFactory.getInstance();

  public static class ValueFactoryAndIntegersProvider implements ArgumentsProvider {
      @Override
      public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
          return Stream.of(
                  io.usethesource.vallang.impl.reference.ValueFactory.getInstance(),
                  io.usethesource.vallang.impl.persistent.ValueFactory.getInstance()
                 ).map(vf -> {
                     Stream<IInteger> integers = Stream.iterate(0, i -> i + 1).map(j -> vf.integer(j)).limit(100);
                     return Arguments.of(vf, integers.toArray(IInteger[]::new)); 
                 });
      }
  }
  
  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testRelationNamedType(IValueFactory vf) {
    try {
      ISet r = vf.set();

      if (!r.getType().isRelation()) {
        fail("relation does not have a relation type");
      }
    } catch (FactTypeUseException e) {
      fail("type error on the construction of a valid relation: " + e);
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testRealZeroDotFromString(IValueFactory vf) {
    assertTrue(vf.real("0.").isEqual(vf.real("0")));
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testZeroRealRepresentation(IValueFactory vf) {
    IReal real = vf.real("0");

    assertTrue(real.toString().equals("0."));
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testRelationTupleType(IValueFactory vf) {
    ISet r = vf.set();

    if (r.size() != 0) {
      fail("empty set is not empty");
    }

    if (!r.getType().isSubtypeOf(ft.relTypeFromTuple(ft.tupleType(ft.integerType())))) {
      fail("should be a rel of unary int tuples");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testRelationWith(IValueFactory vf) {
    ISet[] relations = new ISet[7];
    ITuple[] tuples = new ITuple[7];

    for (int i = 0; i < 7; i++) {
      tuples[i] = vf.tuple(vf.integer(i), vf.real(i));
    }

    try {
      relations[0] = vf.set(tuples[0]);
      relations[1] = vf.set(tuples[0], tuples[1]);
      relations[2] = vf.set(tuples[0], tuples[1], tuples[2]);
      relations[3] = vf.set(tuples[0], tuples[1], tuples[2], tuples[3]);
      relations[4] = vf.set(tuples[0], tuples[1], tuples[2], tuples[3], tuples[4]);
      relations[5] = vf.set(tuples[0], tuples[1], tuples[2], tuples[3], tuples[4], tuples[5]);
      relations[6] =
          vf.set(tuples[0], tuples[1], tuples[2], tuples[3], tuples[4], tuples[5], tuples[6]);

      for (int i = 0; i < 7; i++) {
        for (int j = 0; j < i; j++) {
          if (!relations[i].contains(tuples[j])) {
            fail("tuple creation is weird");
          }
        }
      }
    } catch (FactTypeUseException e) {
      System.err.println(e);
      fail("this should all be type correct");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testSetNamedType(IValueFactory vf) {
    ISet l;
    try {
      TypeStore typeStore = new TypeStore();
      l = vf.set(vf.integer(1));

      if (!l.getType()
          .isSubtypeOf(ft.aliasType(typeStore, "mySet", ft.setType(ft.integerType())))) {
        fail("named types should be aliases");
      }

      if (!l.getElementType().isSubtypeOf(ft.integerType())) {
        fail("elements should be integers");
      }

      if (l.size() != 1) {
        fail("??");
      }
    } catch (FactTypeUseException e1) {
      fail("this was a correct type");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testSetType(IValueFactory vf) {
    ISet s = vf.set();

    if (s.size() != 0) {
      fail("empty set is not empty");
    }

    if (!s.getType().isSubtypeOf(ft.setType(ft.realType()))) {
      fail("should be a list of reals");
    }

    if (!s.getElementType().isSubtypeOf(ft.realType())) {
      fail("should be a list of reals");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryAndIntegersProvider.class)
  public void testSetWith(IValueFactory vf, IInteger[] integers) {
    ISet[] sets = new ISet[7];

    sets[0] = vf.set(integers[0]);
    sets[1] = vf.set(integers[0], integers[1]);
    sets[2] = vf.set(integers[0], integers[1], integers[2]);
    sets[3] = vf.set(integers[0], integers[1], integers[2], integers[3]);
    sets[4] = vf.set(integers[0], integers[1], integers[2], integers[3], integers[4]);
    sets[5] = vf.set(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5]);
    sets[6] = vf.set(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5],
        integers[6]);

    try {
      for (int i = 0; i < 7; i++) {
        for (int j = 0; j <= i; j++) {
          if (!sets[i].contains(integers[j])) {
            fail("set creation is weird");
          }
        }
        for (int j = 8; j < 100; j++) {
          if (sets[i].contains(integers[j])) {
            fail("set creation contains weird values");
          }
        }
      }
    } catch (FactTypeUseException e) {
      System.err.println(e);
      fail("this should all be type correct");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testListNamedType(IValueFactory vf) {
    IList l;
    try {
      TypeStore ts = new TypeStore();
      l = vf.list(vf.integer(1));

      if (!l.getType().isSubtypeOf(ft.aliasType(ts, "myList", ft.listType(ft.integerType())))) {
        fail("named types should be aliases");
      }

      if (!l.getElementType().isSubtypeOf(ft.integerType())) {
        fail("elements should be integers");
      }

      if (l.length() != 1) {
        fail("???");
      }
    } catch (FactTypeUseException e1) {
      fail("this was a correct type");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testListType(IValueFactory vf) {
    IList l = vf.list();

    if (l.length() != 0) {
      fail("empty list is not empty");
    }

    if (!l.getElementType().isSubtypeOf(ft.realType())) {
      fail("should be a list of reals");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryAndIntegersProvider.class)
  public void testListWith(IValueFactory vf, IInteger[] integers) {
    IList[] lists = new IList[7];

    lists[0] = vf.list(integers[0]);
    lists[1] = vf.list(integers[0], integers[1]);
    lists[2] = vf.list(integers[0], integers[1], integers[2]);
    lists[3] = vf.list(integers[0], integers[1], integers[2], integers[3]);
    lists[4] = vf.list(integers[0], integers[1], integers[2], integers[3], integers[4]);
    lists[5] =
        vf.list(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5]);
    lists[6] = vf.list(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5],
        integers[6]);

    for (int i = 0; i < 7; i++) {
      for (int j = 0; j <= i; j++) {
        if (lists[i].get(j) != integers[j]) {
          fail("list creation is weird");
        }
      }
    }

  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryAndIntegersProvider.class)
  public void testTupleIValue(IValueFactory vf, IInteger[] integers) {
    ITuple[] tuples = new ITuple[7];

    tuples[0] = vf.tuple(integers[0]);
    tuples[1] = vf.tuple(integers[0], integers[1]);
    tuples[2] = vf.tuple(integers[0], integers[1], integers[2]);
    tuples[3] = vf.tuple(integers[0], integers[1], integers[2], integers[3]);
    tuples[4] = vf.tuple(integers[0], integers[1], integers[2], integers[3], integers[4]);
    tuples[5] =
        vf.tuple(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5]);
    tuples[6] = vf.tuple(integers[0], integers[1], integers[2], integers[3], integers[4],
        integers[5], integers[6]);

    for (int i = 0; i < 7; i++) {
      for (int j = 0; j <= i; j++) {
        if (tuples[i].get(j) != integers[j]) {
          fail("tuple creation is weird");
        }
      }
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testInteger(IValueFactory vf) {
    assertTrue(vf.integer(42).toString().equals("42"));
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testDubble(IValueFactory vf) {
    assertTrue(vf.real(84.5).toString().equals("84.5"));
  }

  @ParameterizedTest @ArgumentsSource(ValueFactoryProvider.class)
  public void testString(IValueFactory vf) {
    assertTrue(vf.string("hello").getValue().equals("hello"));
    assertTrue(vf.string(0x1F35D).getValue().equals(""));
    assertTrue(vf.string(new int[] {0x1F35D, 0x1F35D}).getValue().equals(""));
  }
}
