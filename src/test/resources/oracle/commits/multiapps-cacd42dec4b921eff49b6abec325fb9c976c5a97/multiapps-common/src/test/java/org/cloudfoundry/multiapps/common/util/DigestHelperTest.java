package org.cloudfoundry.multiapps.common.util;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DigestHelperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testComputeFileChecksum() throws Exception {
        assertEquals("439B99DFFD0583200D5D21F4CD1BF035",
                     DigestHelper.computeFileChecksum(Paths.get("src/test/resources/org/cloudfoundry/multiapps/common/util/web.zip"), "MD5"));
    }

    @Test
    public void testComputeDirectoryChecksum() throws Exception {
        assertEquals("EDEF7E9885FED1A58597212AF0522614",
                     DigestHelper.computeDirectoryCheckSum(Paths.get("src/test/resources/org/cloudfoundry/multiapps/common/util"), "MD5"));
    }

}
