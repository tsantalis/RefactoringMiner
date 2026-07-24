package org.cloudfoundry.multiapps.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class DigestHelperTest {

    @Test
    void testComputeFileChecksum() throws Exception {
        assertEquals("439B99DFFD0583200D5D21F4CD1BF035",
                     DigestHelper.computeFileChecksum(Paths.get("src/test/resources/org/cloudfoundry/multiapps/common/util/web.zip"),
                                                      "MD5"));
    }

    @Test
    void testComputeDirectoryChecksum() throws Exception {
        assertEquals("EDEF7E9885FED1A58597212AF0522614",
                     DigestHelper.computeDirectoryCheckSum(Paths.get("src/test/resources/org/cloudfoundry/multiapps/common/util"), "MD5"));
    }

}
