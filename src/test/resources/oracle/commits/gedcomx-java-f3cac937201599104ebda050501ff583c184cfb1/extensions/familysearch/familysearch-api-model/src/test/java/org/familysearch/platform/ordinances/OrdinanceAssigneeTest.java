package org.familysearch.platform.ordinances;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertSame;

@Test
@SuppressWarnings ( "unchecked" )
public class OrdinanceAssigneeTest {

  @Test
  public void testToFromUri() throws Exception {
    assertSame(OrdinanceAssignee.LdsChurch, OrdinanceAssignee.fromQNameURI(OrdinanceAssignee.LdsChurch.toQNameURI()));
  }

}