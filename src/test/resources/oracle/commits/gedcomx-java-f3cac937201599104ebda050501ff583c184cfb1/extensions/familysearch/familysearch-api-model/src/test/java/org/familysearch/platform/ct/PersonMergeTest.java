package org.familysearch.platform.ct;

import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Mike Gardiner
 */
public class PersonMergeTest {
  @Test
  public void testMerge() {
    Merge merge = new Merge();

    List<ResourceReference> copyReferences = new ArrayList<ResourceReference>();
    ResourceReference resourceReference = new ResourceReference(URI.create("copy1"));
    copyReferences.add(resourceReference);
    resourceReference = new ResourceReference(URI.create("copy2"));
    copyReferences.add(resourceReference);
    resourceReference = new ResourceReference(URI.create("copy3"));
    copyReferences.add(resourceReference);
    merge.setResourcesToCopy( copyReferences );

    List<ResourceReference> deleteReferences = new ArrayList<ResourceReference>();
    resourceReference = new ResourceReference(URI.create("delete1"));
    deleteReferences.add(resourceReference);
    resourceReference = new ResourceReference(URI.create("delete2"));
    deleteReferences.add(resourceReference);
    merge.setResourcesToDelete( deleteReferences );

    assertNotNull(merge.getResourcesToCopy());
    assertEquals(3, merge.getResourcesToCopy().size());
    assertEquals("copy1", merge.getResourcesToCopy().get(0).toString());
    assertEquals("copy2", merge.getResourcesToCopy().get(1).toString());
    assertEquals("copy3", merge.getResourcesToCopy().get(2).toString());

    assertNotNull( merge.getResourcesToDelete() );
    assertEquals(2, merge.getResourcesToDelete().size());
    assertEquals("delete1", merge.getResourcesToDelete().get( 0 ).toString());
    assertEquals("delete2", merge.getResourcesToDelete().get( 1 ).toString());
  }

  @Test
  public void testMergeAnalysis() {
    MergeAnalysis mergeAnalysis = new MergeAnalysis();
    mergeAnalysis.setSurvivor(new ResourceReference(URI.create("person1")));
    mergeAnalysis.setDuplicate( new ResourceReference( URI.create( "person2" ) ) );

    // Surviving Resource References
    List<ResourceReference> survivingReferences = new ArrayList<ResourceReference>();
    ResourceReference resourceReference = new ResourceReference(URI.create("surviving1"));
    survivingReferences.add(resourceReference);
    resourceReference = new ResourceReference(URI.create("surviving2"));
    survivingReferences.add(resourceReference);
    resourceReference = new ResourceReference(URI.create("surviving3"));
    survivingReferences.add(resourceReference);
    mergeAnalysis.setSurvivorResources( survivingReferences );

    // Non-surviving Resource References
    List<ResourceReference> nonSurvivingReferences = new ArrayList<ResourceReference>();
    resourceReference = new ResourceReference(URI.create("nonSurviving1"));
    nonSurvivingReferences.add(resourceReference);
    resourceReference = new ResourceReference(URI.create("nonSurviving2"));
    nonSurvivingReferences.add(resourceReference);
    mergeAnalysis.setDuplicateResources( nonSurvivingReferences );

    // Conflicting Resource References
    List<MergeConflict> conflictingReferences = new ArrayList<MergeConflict>();
    MergeConflict conflict = new MergeConflict(new ResourceReference(URI.create("person1")), new ResourceReference(URI.create("person2")));
    conflictingReferences.add(conflict);
    conflict = new MergeConflict();
    conflict.setSurvivorResource( new ResourceReference( URI.create( "person3" ) ) );
    conflict.setDuplicateResource( new ResourceReference( URI.create( "person4" ) ) );
    conflictingReferences.add(conflict);
    conflict = new MergeConflict(new ResourceReference(URI.create("person5")), new ResourceReference(URI.create("person6")));
    conflictingReferences.add(conflict);
    mergeAnalysis.setConflictingResources(conflictingReferences);

    assertNotNull(mergeAnalysis.getSurvivor());
    assertNotNull("person1", mergeAnalysis.getSurvivor().getResource().toString());

    assertNotNull(mergeAnalysis.getDuplicate());
    assertNotNull("person2", mergeAnalysis.getDuplicate().getResource().toString());

    assertNotNull(mergeAnalysis.getSurvivorResources());
    assertEquals(3, mergeAnalysis.getSurvivorResources().size());
    assertEquals("surviving1", mergeAnalysis.getSurvivorResources().get(0).toString());
    assertEquals("surviving2", mergeAnalysis.getSurvivorResources().get(1).toString());
    assertEquals("surviving3", mergeAnalysis.getSurvivorResources().get(2).toString());

    assertNotNull( mergeAnalysis.getDuplicateResources() );
    assertEquals(2, mergeAnalysis.getDuplicateResources().size());
    assertEquals("nonSurviving1", mergeAnalysis.getDuplicateResources().get(0).toString());
    assertEquals("nonSurviving2", mergeAnalysis.getDuplicateResources().get(1).toString());

    assertNotNull(mergeAnalysis.getConflictingResources());
    assertEquals(3, mergeAnalysis.getConflictingResources().size());
    assertEquals("person1", mergeAnalysis.getConflictingResources().get(0).getSurvivorResource().toString());
    assertEquals("person2", mergeAnalysis.getConflictingResources().get(0).getDuplicateResource().toString());
    assertEquals("person3", mergeAnalysis.getConflictingResources().get(1).getSurvivorResource().toString());
    assertEquals("person4", mergeAnalysis.getConflictingResources().get(1).getDuplicateResource().toString());
    assertEquals("person5", mergeAnalysis.getConflictingResources().get(2).getSurvivorResource().toString());
    assertEquals("person6", mergeAnalysis.getConflictingResources().get(2).getDuplicateResource().toString());
  }
}
