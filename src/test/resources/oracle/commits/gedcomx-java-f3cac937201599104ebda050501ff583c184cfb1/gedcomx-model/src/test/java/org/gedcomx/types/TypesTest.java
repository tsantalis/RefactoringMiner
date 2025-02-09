/**
 * Copyright Intellectual Reserve, Inc. All Rights reserved.
 */
package org.gedcomx.types;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 */
public class TypesTest {
  @Test
  public void testToQNameURI() throws Exception {
    // NOTE: not a full test, but gets some code coverage

    assertEquals(ConfidenceLevel.fromQNameURI(ConfidenceLevel.Low.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Low");
    assertEquals(DocumentType.fromQNameURI(DocumentType.Analysis.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Analysis");
    assertEquals(EventRoleType.fromQNameURI(EventRoleType.Principal.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Principal");
    assertEquals(EventType.fromQNameURI(EventType.Burial.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Burial");
    assertEquals(FactType.fromQNameURI(FactType.Marriage.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Marriage");
    assertEquals(FactType.fromQNameURI(FactType.Birth.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Birth");
    assertEquals(FactType.fromQNameURI(FactType.Baptism.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Baptism");
    assertEquals(GenderType.fromQNameURI(GenderType.Male.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Male");
    assertEquals(IdentifierType.fromQNameURI(IdentifierType.Primary.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Primary");
    assertEquals(NamePartQualifierType.fromQNameURI(NamePartQualifierType.Primary.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Primary");
    assertEquals(NamePartType.fromQNameURI(NamePartType.Given.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Given");
    assertEquals(NameType.fromQNameURI(NameType.FormalName.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/FormalName");
    assertEquals(RelationshipType.fromQNameURI(RelationshipType.Couple.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Couple");
    assertEquals(FieldValueStatusType.fromQNameURI(FieldValueStatusType.Unreadable.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Unreadable");
    assertEquals(ResourceStatusType.fromQNameURI(ResourceStatusType.Deprecated.toQNameURI()).toQNameURI().toString(), "http://gedcomx.org/Deprecated");
  }

  @Test
  public void testFactTypeIsLike() throws Exception {
    // NOTE: not a full test, but gets some code coverage

    assertTrue(FactType.Christening.isBirthLike());
    assertTrue(FactType.Burial.isDeathLike());
    assertTrue(FactType.MarriageBanns.isMarriageLike());
    assertTrue(FactType.DivorceFiling.isDivorceLike());
    assertTrue(FactType.Naturalization.isMigrationLike());
  }

  @Test
  public void testFactTypeInnerClasses() throws Exception {
    // NOTE: not a full test, but gets some code coverage

    assertTrue(FactType.Person.isApplicable(FactType.Will));
    assertTrue(FactType.Couple.isApplicable(FactType.Separation));
    assertTrue(FactType.ParentChild.isApplicable(FactType.GuardianParent));
  }

}
