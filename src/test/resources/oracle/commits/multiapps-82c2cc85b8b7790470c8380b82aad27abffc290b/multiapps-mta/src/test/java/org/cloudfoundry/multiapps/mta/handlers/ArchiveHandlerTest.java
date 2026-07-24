package org.cloudfoundry.multiapps.mta.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.junit.jupiter.api.Test;

class ArchiveHandlerTest {

    private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final long MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024L;
    private static final long MAX_MANIFEST_SIZE = 1024 * 1024L;
    private static final long MAX_RESOURCE_FILE_SIZE = 1024 * 1024 * 1024L;

    @Test
    void testGetManifest() throws Exception {
        Manifest manifest = ArchiveHandler.getManifest(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_MTAR), MAX_MANIFEST_SIZE);
        Map<String, Attributes> entries = manifest.getEntries();
        assertEquals(4, entries.size());
        assertTrue(entries.containsKey("web/web-server.zip"));
        assertTrue(entries.containsKey("applogic/pricing.zip"));
        assertTrue(entries.containsKey("db/pricing-db.zip"));
        assertTrue(entries.containsKey("META-INF/mtad.yaml"));
    }

    @Test
    void testGetManifestExceedsSizeLimit() throws Exception {
        assertThrows(ContentException.class,
                     () -> ArchiveHandler.getManifest(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_MTAR), 512L));
    }

    @Test
    void testGetDescriptor() throws Exception {
        String descriptor = ArchiveHandler.getDescriptor(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_MTAR),
                                                         MAX_MTA_DESCRIPTOR_SIZE);
        assertTrue(descriptor.contains("com.sap.mta.sample"));
    }

    @Test
    void testGetDescriptorExceedsSize() throws Exception {
        assertThrows(ContentException.class,
                     () -> ArchiveHandler.getDescriptor(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_MTAR), 1024L));
    }

    @Test
    void testGetModuleContent() throws Exception {
        byte[] moduleContent = ArchiveHandler.getFileContent(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_MTAR),
                                                             "web/web-server.zip", 2 * 1024 * 1024);

        InputStream entryStream = getEntryStream(moduleContent, "readme.txt");
        String readmeContent = IOUtils.toString(entryStream, StandardCharsets.UTF_8);
        assertEquals("App router code will be packaged in this archive", readmeContent);
    }

    @Test
    void testGetModuleContentExceedsSize() throws Exception {
        assertThrows(ContentException.class, () -> ArchiveHandler.getFileContent(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_MTAR),
                                                                                 "web/web-server.zip", 128L));
    }

    @Test
    void testGetManifestFlat() throws Exception {
        Manifest manifest = ArchiveHandler.getManifest(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_FLAT_MTAR), MAX_MANIFEST_SIZE);
        Map<String, Attributes> entries = manifest.getEntries();
        assertEquals(4, entries.size());
        assertTrue(entries.containsKey("web/"));
        assertTrue(entries.containsKey("applogic/"));
        assertTrue(entries.containsKey("db/"));
        assertTrue(entries.containsKey("META-INF/mtad.yaml"));
    }

    @Test
    void testGetDescriptorFlat() throws Exception {
        String descriptor = ArchiveHandler.getDescriptor(ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_FLAT_MTAR),
                                                         MAX_MTA_DESCRIPTOR_SIZE);
        assertTrue(descriptor.contains("com.sap.mta.sample"));
    }

    @Test
    void testGetModuleContentFlat() throws Exception {
        InputStream mtarStream = ArchiveHandlerTest.class.getResourceAsStream(SAMPLE_FLAT_MTAR);
        try (InputStream is = ArchiveHandler.getInputStream(mtarStream, "web/", MAX_RESOURCE_FILE_SIZE)) {
            ZipInputStream zis = (ZipInputStream) is;
            for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
                if (e.getName()
                     .equals("web/readme.txt")) {
                    String readmeContent = IOUtils.toString(zis, StandardCharsets.UTF_8);
                    assertEquals("App router code will be packaged in this archive", readmeContent);
                }
            }
        }
    }

    private InputStream getEntryStream(byte[] content, String entryName) throws IOException {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(content));
        for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
            if (e.getName()
                 .equals(entryName)) {
                return zis;
            }
        }
        return null;
    }

}
