package org.familysearch.platform.rt;

import org.familysearch.platform.FamilySearchPlatform;
import org.familysearch.platform.ct.ChildAndParentsRelationship;
import org.familysearch.platform.ct.Merge;
import org.familysearch.platform.ct.MergeAnalysis;
import org.familysearch.platform.discussions.Comment;
import org.familysearch.platform.discussions.Discussion;
import org.gedcomx.agent.Agent;
import org.gedcomx.conclusion.*;
import org.gedcomx.links.Link;
import org.gedcomx.source.SourceDescription;
import org.junit.Test;


import java.util.ArrayList;

import static org.junit.Assert.*;

public class FamilySearchPlatformModelVisitorBaseTest {
  @Test
  public void testNullVisitor() throws Exception {
    try {
      FamilySearchPlatform fsp = new FamilySearchPlatform();
      fsp.accept(null);
      fail("Expected: NullPointerException");
    } catch (NullPointerException ex) {
      assertTrue(true);
    }
  }

  @Test
  public void testVisitFeed() throws Exception {
    FamilySearchPlatformModelVisitorBase visitor = new FamilySearchPlatformModelVisitorBase();
    assertNotNull(visitor.getContextStack());
    assertEquals(visitor.getContextStack().size(), 0);

    FamilySearchPlatform fsp = new FamilySearchPlatform();

    // visit empty feed
    fsp.accept(visitor);

    ArrayList<Discussion> discussions;
    ArrayList<MergeAnalysis> mergeAnalyses;
    ArrayList<Merge> merges;
    ArrayList<ChildAndParentsRelationship> childAndParentsRelationships;

    // re-visit feed; empty lists
    discussions = new ArrayList<Discussion>();
    mergeAnalyses = new ArrayList<MergeAnalysis>();
    merges = new ArrayList<Merge>();
    childAndParentsRelationships = new ArrayList<ChildAndParentsRelationship>();
    fsp.setAgents( new ArrayList<Agent>() );
    fsp.setDiscussions( discussions );
    fsp.setDocuments( new ArrayList<Document>() );
    fsp.setEvents( new ArrayList<Event>() );
    fsp.setExtensionElements( new ArrayList<Object>() );
    fsp.setLinks( new ArrayList<Link>() );
    fsp.setMerges( merges );
    fsp.setMergeAnalyses( mergeAnalyses );
    fsp.setChildAndParentsRelationships( childAndParentsRelationships );
    fsp.setPersons( new ArrayList<Person>() );
    fsp.setPlaces( new ArrayList<PlaceDescription>() );
    fsp.setRelationships( new ArrayList<Relationship>() );
    fsp.setSourceDescriptions( new ArrayList<SourceDescription>() );

    // re-visit feed; populate content; add element to authors and contributors
    discussions.add(new Discussion());
    mergeAnalyses.add( new MergeAnalysis() );
    merges.add( new Merge() );
    childAndParentsRelationships.add(new ChildAndParentsRelationship());
    fsp.accept(visitor);

    // re-visit feed; add empty lists to discussions and parent-child relationships
    discussions.get(0).setComments(new ArrayList<Comment>());
    childAndParentsRelationships.get(0).setFatherFacts(new ArrayList<Fact>());
    childAndParentsRelationships.get(0).setMotherFacts(new ArrayList<Fact>());
    fsp.accept(visitor);

    // re-visit feed; add single element to comments and facts lists
    discussions.get(0).getComments().add(new Comment());
    childAndParentsRelationships.get(0).getFatherFacts().add(new Fact());
    childAndParentsRelationships.get(0).getMotherFacts().add(new Fact());
    fsp.accept(visitor);
  }
}
