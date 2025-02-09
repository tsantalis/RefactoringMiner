package org.gedcomx.examples;

import org.gedcomx.Gedcomx;
import org.gedcomx.common.Qualifier;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.*;
import org.gedcomx.rt.DataURIUtil;
import org.gedcomx.rt.SerializationUtil;
import org.gedcomx.types.FactQualifierType;
import org.gedcomx.types.FactType;
import org.gedcomx.types.RelationshipType;
import org.junit.Test;

/**
 * @author Ryan Heaton
 */
public class MiscellaneousFactsExampleTest {

  @Test
  public void testCensusAndResidenceLikeFacts() throws Exception {
    Person person = new Person()
      .fact(new Fact(FactType.Census, "...", "..."))
      .fact(new Fact(FactType.Emigration, "...", "..."))
      .fact(new Fact(FactType.Immigration, "...", "..."))
      .fact(new Fact(FactType.LandTransaction, "...", "..."))
      .fact(new Fact(FactType.MoveTo, "...", "..."))
      .fact(new Fact(FactType.MoveFrom, "...", "..."))
      .fact(new Fact(FactType.Residence, "...", "..."));
    Gedcomx gx = new Gedcomx().person(person);
    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  @Test
  public void testMilitaryServiceFacts() throws Exception {
    Person person = new Person()
      .fact(new Fact(FactType.MilitaryAward, "...", "..."))
      .fact(new Fact(FactType.MilitaryDischarge, "...", "..."))
      .fact(new Fact(FactType.MilitaryDraftRegistration, "...", "..."))
      .fact(new Fact(FactType.MilitaryInduction, "...", "..."))
      .fact(new Fact(FactType.MilitaryService, "...", "..."));
    Gedcomx gx = new Gedcomx().person(person);
    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  @Test
  public void testEducationAndOccupationFacts() throws Exception {
    Person person = new Person()
      .fact(new Fact(FactType.Apprenticeship, "...", "..."))
      .fact(new Fact(FactType.Education, "...", "..."))
      .fact(new Fact(FactType.Occupation, "...", "..."))
      .fact(new Fact(FactType.Retirement, "...", "..."));
    Gedcomx gx = new Gedcomx().person(person);
    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  @Test
  public void testReligiousOrCulturalFacts() throws Exception {
    Person person = new Person()
      .fact(new Fact(FactType.AdultChristening, "...", "..."))
      .fact(new Fact(FactType.Baptism, "...", "..."))
      .fact(new Fact(FactType.BarMitzvah, "...", "..."))
      .fact(new Fact(FactType.BatMitzvah, "...", "..."))
      .fact(new Fact(FactType.Caste, "...", "..."))
      .fact(new Fact(FactType.Christening, "...", "..."))
      .fact(new Fact(FactType.Circumcision, "...", "..."))
      .fact(new Fact(FactType.Clan, "...", "..."))
      .fact(new Fact(FactType.Confirmation, "...", "..."))
      .fact(new Fact(FactType.Excommunication, "...", "..."))
      .fact(new Fact(FactType.FirstCommunion, "...", "..."))
      .fact(new Fact(FactType.Nationality, "...", "..."))
      .fact(new Fact(FactType.Ordination, "...", "..."))
      .fact(new Fact(FactType.Religion, "...", "..."))
      .fact(new Fact(FactType.Yahrzeit, "...", "..."));
    Gedcomx gx = new Gedcomx().person(person);
    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  @Test
  public void testFactQualifiers() throws Exception {
    Person person = new Person()
      .fact(new Fact(FactType.Christening, "...", "...").qualifier(new Qualifier(FactQualifierType.Religion, "Catholic")))
      .fact(new Fact(FactType.Census, "...", "...").qualifier(new Qualifier(FactQualifierType.Age, "44")))
      .fact(new Fact(FactType.Death, "...", "...").qualifier(new Qualifier(FactQualifierType.Cause, "Heart failure")));
    Gedcomx gx = new Gedcomx().person(person);
    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  @Test
  public void testCustomFact() throws Exception {
    Person person = new Person()
      .fact(new Fact().type(URI.create(DataURIUtil.encodeDataURI("Eagle Scout").toString())).place(new PlaceReference().original("...")).date(new Date().original("...")));
    Gedcomx gx = new Gedcomx().person(person);
    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  @Test
  public void testRelationshipFacts() throws Exception {
    Relationship couple = new Relationship()
      .type(RelationshipType.Couple)
      .fact(new Fact(FactType.CivilUnion, "...", "..."))
      .fact(new Fact(FactType.DomesticPartnership, "...", "..."))
      .fact(new Fact(FactType.Divorce, "...", "..."))
      .fact(new Fact(FactType.Marriage, "...", "..."))
      .fact(new Fact(FactType.MarriageBanns, "...", "..."))
      .fact(new Fact(FactType.MarriageContract, "...", "..."))
      .fact(new Fact(FactType.MarriageLicense, "...", "..."));

    Relationship parentChild = new Relationship()
      .type(RelationshipType.ParentChild)
      .fact(new Fact(FactType.AdoptiveParent, "...", "..."))
      .fact(new Fact(FactType.BiologicalParent, "...", "..."))
      .fact(new Fact(FactType.FosterParent, "...", "..."))
      .fact(new Fact(FactType.GuardianParent, "...", "..."))
      .fact(new Fact(FactType.StepParent, "...", "..."));

    Gedcomx gx = new Gedcomx().relationship(couple).relationship(parentChild);
    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

}
