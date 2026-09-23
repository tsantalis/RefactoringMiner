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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.Setup;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

@RunWith(Parameterized.class)
public final class ValueFactorySmokeTest {

  @Parameterized.Parameters
  public static Iterable<? extends Object> data() {
    return Setup.valueFactories();
  }

  private final IValueFactory vf;

  public ValueFactorySmokeTest(final IValueFactory vf) {
    this.vf = vf;
  }

  private TypeFactory ft = TypeFactory.getInstance();
  private IValue[] integers;

  @Before
  public void setUp() throws Exception {

    integers = new IValue[100];
    for (int i = 0; i < integers.length; i++) {
      integers[i] = vf.integer(i);
    }
  }

  @Test
  public void testRelationNamedType() {
    try {
      ISet r = vf.set();

      if (!r.getType().isRelation()) {
        fail("relation does not have a relation type");
      }
    } catch (FactTypeUseException e) {
      fail("type error on the construction of a valid relation: " + e);
    }
  }

  @Test
  public void testRealZeroDotFromString() {
    assertTrue(vf.real("0.").isEqual(vf.real("0")));
  }

  @Test
  public void testZeroRealRepresentation() {
    IReal real = vf.real("0");

    assertTrue(real.toString().equals("0."));
  }

  @Test
  public void testRelationTupleType() {
    ISet r = vf.set();

    if (r.size() != 0) {
      fail("empty set is not empty");
    }

    if (!r.getType().isSubtypeOf(ft.relTypeFromTuple(ft.tupleType(ft.integerType())))) {
      fail("should be a rel of unary int tuples");
    }
  }

  @Test
  public void testRelationWith() {
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

  @Test
  public void testSetNamedType() {
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

  @Test
  public void testSetType() {
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

  @Test
  public void testSetWith() {
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

  @Test
  public void testListNamedType() {
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

  @Test
  public void testListType() {
    IList l = vf.list();

    if (l.length() != 0) {
      fail("empty list is not empty");
    }

    if (!l.getElementType().isSubtypeOf(ft.realType())) {
      fail("should be a list of reals");
    }
  }

  @Test
  public void testListWith() {
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

  @Test
  public void testTupleIValue() {
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

  @Test
  public void testInteger() {
    assertTrue(vf.integer(42).toString().equals("42"));
  }

  @Test
  public void testDubble() {
    assertTrue(vf.real(84.5).toString().equals("84.5"));
  }

  @Test
  public void testString() {
    assertTrue(vf.string("hello").getValue().equals("hello"));
    assertTrue(vf.string(0x1F35D).getValue().equals(""));
    assertTrue(vf.string(new int[] {0x1F35D, 0x1F35D}).getValue().equals(""));
  }

  // @Test
  // public void testSourceLocation() {
  // ISourceLocation sl;
  // try {
  // sl = vf.sourceLocation(new URL("file:///dev/null"), 1, 2, 3, 4, 5, 6);
  // if (!sl.getURL().getPath().equals("/dev/null")) {
  // fail("source location creation is weird");
  // }
  //
  // if (sl.getStartOffset() != 1 || sl.getLength() != 2
  // || sl.getStartColumn() != 5 || sl.getStartLine() != 3
  // || sl.getEndLine() != 4 || sl.getEndColumn() != 6) {
  // fail("source range creation is weird");
  // }
  // } catch (MalformedURLException e) {
  // fail();
  // }
  //
  // }

  @Test
  public void testToString() {
    // first we create a lot of values, and
    // then we check whether toString does the same
    // as StandardTextWriter
    ISetWriter extended;
    try {
      extended = createSomeValues();

      StandardTextWriter w = new StandardTextWriter();

      for (IValue o : extended.done()) {
        StringWriter out = new StringWriter();
        try {
          w.write(o, out);
          if (!out.toString().equals(o.toString())) {
            fail(out.toString() + " != " + o.toString());
          }
        } catch (IOException e) {
          fail(e.toString());
          e.printStackTrace();
        }
      }

    } catch (FactTypeUseException | MalformedURLException e1) {
      fail(e1.toString());
    }
  }

  @Test
  public void testStandardReaderWriter() {
    StandardTextWriter w = new StandardTextWriter();
    StandardTextReader r = new StandardTextReader();

    try {
      for (IValue o : createSomeValues().done()) {
        StringWriter out = new StringWriter();
        w.write(o, out);
        StringReader in = new StringReader(out.toString());
        IValue read = r.read(vf, in);
        if (!o.isEqual(read)) {
          fail(o + " != " + read + " " + o.isEqual(read));
        }
      }
    } catch (IOException e) {
      fail();
    }
  }

  private ISetWriter createSomeValues() throws FactTypeUseException, MalformedURLException {
    ISetWriter basicW = vf.setWriter();

    // TODO add tests for locations and constructors again
    basicW.insert(vf.integer(0), vf.real(0.0),
        // vf.sourceLocation(new URL("file:///dev/null"), 0, 0, 0, 0, 0, 0),
        vf.bool(true), vf.bool(false), vf.node("hello"));

    ISet basic = basicW.done();
    ISetWriter extended = vf.setWriter();

    // TypeStore ts = new TypeStore();
    // Type adt = ft.abstractDataType(ts, "E");
    // Type cons0 = ft.constructor(ts, adt, "cons");
    // Type cons1 = ft.constructor(ts, adt, "cons", ft.valueType(), "value");

    extended.insertAll(basic);
    for (IValue w : basic) {
      extended.insert(vf.list());
      extended.insert(vf.list(w));
      extended.insert(vf.set());
      extended.insert(vf.set(w));
      IMap map = vf.mapWriter().done();
      extended.insert(map.put(w, w));
      ITuple tuple = vf.tuple(w, w);
      extended.insert(tuple);
      extended.insert(vf.set(tuple, tuple));
      extended.insert(vf.node("hi", w));
      // extended.insert(vf.constructor(cons0));
      // extended.insert(vf.constructor(cons1, w));
    }
    return extended;
  }
}
