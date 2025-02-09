package org.familysearch.platform.ordinances;

import org.junit.Test;

import static org.junit.Assert.assertSame;

@SuppressWarnings ( "unchecked" )
public class OrdinanceAssigneeTest {

  @Test
  public void testToFromUri() throws Exception {
    assertSame(OrdinanceAssignee.LdsChurch, OrdinanceAssignee.fromQNameURI(OrdinanceAssignee.LdsChurch.toQNameURI()));
  }

}
