/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link FileSystemUtils}.
 */
@SuppressWarnings("deprecation") // testing deprecated class
public class FileSystemUtilsTest {

    static class MockFileSystemUtils extends FileSystemUtils {
        private final int exitCode;
        private final byte[] bytes;
        private final String cmd;

        public MockFileSystemUtils(final int exitCode, final String lines) {
            this(exitCode, lines, null);
        }

        public MockFileSystemUtils(final int exitCode, final String lines, final String cmd) {
            this.exitCode = exitCode;
            this.bytes = lines.getBytes();
            this.cmd = cmd;
        }

        @Override
        Process openProcess(final String[] params) {
            if (cmd != null) {
                assertEquals(cmd, params[params.length - 1]);
            }
            return new Process() {
                @Override
                public void destroy() {
                    // nnop
                }

                @Override
                public int exitValue() {
                    return exitCode;
                }

                @Override
                public InputStream getErrorStream() {
                    return null;
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public OutputStream getOutputStream() {
                    return null;
                }

                @Override
                public int waitFor() throws InterruptedException {
                    return exitCode;
                }
            };
        }
    }

    private static final Duration NEG_1_TIMEOUT = Duration.ofMillis(-1);

    static char[] getIllegalFileNameChars() {
        return FileSystem.getCurrent().getIllegalFileNameChars();
    }

    @Test
    public void testGetFreeSpace_String() throws Exception {
        // test coverage, as we can't check value
        if (File.separatorChar == '/') {
            // have to figure out UNIX block size
            final String[] cmd;
            String osName = System.getProperty("os.name");
            osName = osName.toLowerCase(Locale.ENGLISH);

            if (osName.contains("hp-ux") || osName.contains("aix")) {
                cmd = new String[]{"df", "-P", "/"};
            } else if (osName.contains("sunos") || osName.contains("sun os")
                    || osName.contains("solaris")) {
                cmd = new String[]{"/usr/xpg4/bin/df", "-P", "/"};
            } else {
                cmd = new String[]{"df", "/"};
            }
            final Process proc = Runtime.getRuntime().exec(cmd);
            boolean kilobyteBlock = true;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))){
                final String line = r.readLine();
                assertNotNull(line, "Unexpected null line");
                if (line.contains("512")) {
                    kilobyteBlock = false;
                }
            }

            // now perform the test
            final long free = FileSystemUtils.freeSpace("/");
            final long kb = FileSystemUtils.freeSpaceKb("/");
            // Assume disk space does not fluctuate
            // more than 1% between the above two calls;
            // this is also small enough to verify freeSpaceKb uses
            // kibibytes (1024) instead of SI kilobytes (1000)
            final double acceptableDelta = kb * 0.01d;
            if (kilobyteBlock) {
                assertEquals(free, kb, acceptableDelta);
            } else {
                assertEquals(free / 2d, kb, acceptableDelta);
            }
        } else {
            final long bytes = FileSystemUtils.freeSpace("");
            final long kb = FileSystemUtils.freeSpaceKb("");
            // Assume disk space does not fluctuate more than 1%
            final double acceptableDelta = kb * 0.01d;
            assertEquals((double) bytes / 1024, kb, acceptableDelta);
        }
    }

    @Test
    public void testGetFreeSpaceOS_String_InitError() throws Exception {
        final FileSystemUtils fsu = new FileSystemUtils();
        assertThrows(IllegalStateException.class, () -> fsu.freeSpaceOS("", -1, false, NEG_1_TIMEOUT));
        assertThrows(IllegalStateException.class, () -> fsu.freeSpaceOS("", -1, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceOS_String_NullPath() throws Exception {
        final FileSystemUtils fsu = new FileSystemUtils();
        assertThrows(NullPointerException.class, () -> fsu.freeSpaceOS(null, 1, false, NEG_1_TIMEOUT));
        assertThrows(NullPointerException.class, () -> fsu.freeSpaceOS(null, 1, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceOS_String_Other() throws Exception {
        final FileSystemUtils fsu = new FileSystemUtils();
        assertThrows(IllegalStateException.class, () -> fsu.freeSpaceOS("", 0, false, NEG_1_TIMEOUT));
        assertThrows(NullPointerException.class, () -> fsu.freeSpaceOS(null, 1, true, NEG_1_TIMEOUT));
        assertThrows(IllegalStateException.class, () -> fsu.freeSpaceOS("", 0, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceOS_String_Unix() throws Exception {
        final FileSystemUtils fsu = new FileSystemUtils() {
            @Override
            protected long freeSpaceUnix(final String path, final boolean kb, final boolean posix, final Duration timeout) throws IOException {
                return kb ? 12345L : 54321;
            }
        };
        assertEquals(54321L, fsu.freeSpaceOS("", 2, false, NEG_1_TIMEOUT));
        assertEquals(12345L, fsu.freeSpaceOS("", 2, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceOS_String_Windows() throws Exception {
        final FileSystemUtils fsu = new FileSystemUtils() {
            @Override
            protected long freeSpaceWindows(final String path, final Duration timeout) throws IOException {
                return 12345L;
            }
        };
        assertEquals(12345L, fsu.freeSpaceOS("", 1, false, NEG_1_TIMEOUT));
        assertEquals(12345L / 1024, fsu.freeSpaceOS("", 1, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_EmptyPath() throws Exception {
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "xxx:/home/users/s     14428928  12956424   1472504  90% /home/users/s";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IllegalArgumentException.class, () -> fsu.freeSpaceUnix("", false, false, NEG_1_TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> fsu.freeSpaceUnix("", true, false, NEG_1_TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> fsu.freeSpaceUnix("", true, true, NEG_1_TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> fsu.freeSpaceUnix("", false, true, NEG_1_TIMEOUT));
    }

    @Test

    public void testGetFreeSpaceUnix_String_EmptyResponse() {
        final String lines = "";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, true, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_InvalidResponse1() {
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "                      14428928  12956424       100";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, true, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_InvalidResponse2() {
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "xxx:/home/users/s     14428928  12956424   nnnnnnn  90% /home/users/s";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, true, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_InvalidResponse3() {
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "xxx:/home/users/s     14428928  12956424        -1  90% /home/users/s";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, true, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_InvalidResponse4() {
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "xxx-yyyyyyy-zzz:/home/users/s";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, false, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", false, true, NEG_1_TIMEOUT));
        assertThrows(IOException.class, () -> fsu.freeSpaceUnix("/home/users/s", true, true, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_LongResponse() throws Exception {
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "xxx-yyyyyyy-zzz:/home/users/s\n" +
                        "                      14428928  12956424   1472504  90% /home/users/s";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(1472504L, fsu.freeSpaceUnix("/home/users/s", false, false, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_LongResponseKb() throws Exception {
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "xxx-yyyyyyy-zzz:/home/users/s\n" +
                        "                      14428928  12956424   1472504  90% /home/users/s";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(1472504L, fsu.freeSpaceUnix("/home/users/s", true, false, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_NormalResponseFreeBSD() throws Exception {
        // from Apache 'FreeBSD 6.1-RELEASE (SMP-turbo)'
        final String lines =
                "Filesystem  1K-blocks      Used    Avail Capacity  Mounted on\n" +
                        "/dev/xxxxxx    128990    102902    15770    87%    /";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(15770L, fsu.freeSpaceUnix("/", false, false, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_NormalResponseKbFreeBSD() throws Exception {
        // from Apache 'FreeBSD 6.1-RELEASE (SMP-turbo)'
        // df and df -k are identical, but df -kP uses 512 blocks (not relevant as not used)
        final String lines =
                "Filesystem  1K-blocks      Used    Avail Capacity  Mounted on\n" +
                        "/dev/xxxxxx    128990    102902    15770    87%    /";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(15770L, fsu.freeSpaceUnix("/", true, false, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_NormalResponseKbLinux() throws Exception {
        // from Sourceforge 'GNU bash, version 2.05b.0(1)-release (i386-redhat-linux-gnu)'
        // df, df -k and df -kP are all identical
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "/dev/xxx                497944    308528    189416  62% /";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(189416L, fsu.freeSpaceUnix("/", true, false, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_NormalResponseKbSolaris() throws Exception {
        // from IO-91 - ' SunOS et 5.10 Generic_118822-25 sun4u sparc SUNW,Ultra-4'
        // non-kb response does not contain free space - see IO-91
        final String lines =
                "Filesystem            kbytes    used   avail capacity  Mounted on\n" +
                        "/dev/dsk/x0x0x0x0    1350955  815754  481163    63%";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(481163L, fsu.freeSpaceUnix("/dev/dsk/x0x0x0x0", true, false, NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceUnix_String_NormalResponseLinux() throws Exception {
        // from Sourceforge 'GNU bash, version 2.05b.0(1)-release (i386-redhat-linux-gnu)'
        final String lines =
                "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                        "/dev/xxx                497944    308528    189416  62% /";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(189416L, fsu.freeSpaceUnix("/", false, false, NEG_1_TIMEOUT));
    }

    @EnabledOnOs(value = OS.WINDOWS)
    @ParameterizedTest
    @MethodSource("getIllegalFileNameChars")
    public void testGetFreeSpaceWindows_IllegalFileName(final char illegalFileNameChar) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new FileSystemUtils().freeSpaceWindows("\\ \"" + illegalFileNameChar, NEG_1_TIMEOUT));
    }

    @EnabledOnOs(value = OS.WINDOWS)
    @Test
    public void testGetFreeSpaceWindows_IllegalFileNames() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new FileSystemUtils().freeSpaceWindows("\\ \"", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_EmptyMultiLineResponse() {
        final String lines = "\n\n";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceWindows("C:", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_EmptyPath() throws Exception {
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\Xxxx\n" +
                        "\n" +
                        "19/08/2005  22:43    <DIR>          .\n" +
                        "19/08/2005  22:43    <DIR>          ..\n" +
                        "11/08/2005  01:07                81 build.properties\n" +
                        "17/08/2005  21:44    <DIR>          Desktop\n" +
                        "               7 File(s)         180260 bytes\n" +
                        "              10 Dir(s)     41411551232 bytes free";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines, "dir /a /-c \"\"");
        assertEquals(41411551232L, fsu.freeSpaceWindows("", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_EmptyResponse() {
        final String lines = "";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceWindows("C:", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_InvalidTextResponse() {
        final String lines = "BlueScreenOfDeath";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceWindows("C:", NEG_1_TIMEOUT));
    }
    @Test
    public void testGetFreeSpaceWindows_String_NormalResponse() throws Exception {
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\Xxxx\n" +
                        "\n" +
                        "19/08/2005  22:43    <DIR>          .\n" +
                        "19/08/2005  22:43    <DIR>          ..\n" +
                        "11/08/2005  01:07                81 build.properties\n" +
                        "17/08/2005  21:44    <DIR>          Desktop\n" +
                        "               7 File(s)         180260 bytes\n" +
                        "              10 Dir(s)     41411551232 bytes free";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines, "dir /a /-c \"C:\"");
        assertEquals(41411551232L, fsu.freeSpaceWindows("C:", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_NoSuchDirectoryResponse() {
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\empty" +
                        "\n";
        final FileSystemUtils fsu = new MockFileSystemUtils(1, lines);
        assertThrows(IOException.class, () -> fsu.freeSpaceWindows("C:", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_ParseCommaFormatBytes() throws Exception {
        // this is the format of response when calling dir /c
        // we have now switched to dir /-c, so we should never get this
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\Xxxx\n" +
                        "\n" +
                        "19/08/2005  22:43    <DIR>          .\n" +
                        "19/08/2005  22:43    <DIR>          ..\n" +
                        "11/08/2005  01:07                81 build.properties\n" +
                        "17/08/2005  21:44    <DIR>          Desktop\n" +
                        "               7 File(s)        180,260 bytes\n" +
                        "              10 Dir(s)  41,411,551,232 bytes free";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(41411551232L, fsu.freeSpaceWindows("", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_ParseCommaFormatBytes_Big() throws Exception {
        // test with very large free space
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\Xxxx\n" +
                        "\n" +
                        "19/08/2005  22:43    <DIR>          .\n" +
                        "19/08/2005  22:43    <DIR>          ..\n" +
                        "11/08/2005  01:07                81 build.properties\n" +
                        "17/08/2005  21:44    <DIR>          Desktop\n" +
                        "               7 File(s)        180,260 bytes\n" +
                        "              10 Dir(s)  141,411,551,232 bytes free";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(141411551232L, fsu.freeSpaceWindows("", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_ParseCommaFormatBytes_Small() throws Exception {
        // test with very large free space
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\Xxxx\n" +
                        "\n" +
                        "19/08/2005  22:43    <DIR>          .\n" +
                        "19/08/2005  22:43    <DIR>          ..\n" +
                        "11/08/2005  01:07                81 build.properties\n" +
                        "17/08/2005  21:44    <DIR>          Desktop\n" +
                        "               7 File(s)        180,260 bytes\n" +
                        "              10 Dir(s)  1,232 bytes free";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines);
        assertEquals(1232L, fsu.freeSpaceWindows("", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_quoted() throws Exception {
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\Xxxx\n" +
                        "\n" +
                        "19/08/2005  22:43    <DIR>          .\n" +
                        "19/08/2005  22:43    <DIR>          ..\n" +
                        "11/08/2005  01:07                81 build.properties\n" +
                        "17/08/2005  21:44    <DIR>          Desktop\n" +
                        "               7 File(s)         180260 bytes\n" +
                        "              10 Dir(s)     41411551232 bytes free";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines, "dir /a /-c \"C:\\somedir\"");
        assertEquals(41411551232L, fsu.freeSpaceWindows("\"C:\\somedir\"", NEG_1_TIMEOUT));
    }

    @Test
    public void testGetFreeSpaceWindows_String_StripDrive() throws Exception {
        final String lines =
                " Volume in drive C is HDD\n" +
                        " Volume Serial Number is XXXX-YYYY\n" +
                        "\n" +
                        " Directory of C:\\Documents and Settings\\Xxxx\n" +
                        "\n" +
                        "19/08/2005  22:43    <DIR>          .\n" +
                        "19/08/2005  22:43    <DIR>          ..\n" +
                        "11/08/2005  01:07                81 build.properties\n" +
                        "17/08/2005  21:44    <DIR>          Desktop\n" +
                        "               7 File(s)         180260 bytes\n" +
                        "              10 Dir(s)     41411551232 bytes free";
        final FileSystemUtils fsu = new MockFileSystemUtils(0, lines, "dir /a /-c \"C:\\somedir\"");
        assertEquals(41411551232L, fsu.freeSpaceWindows("C:\\somedir", NEG_1_TIMEOUT));
    }

}
