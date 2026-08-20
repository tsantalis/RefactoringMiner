package sortpom.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * @author bjorn
 * @since 2020-01-09
 */
class FileAttributeUtilTest {
    private final FileAttributeUtil fileAttributeUtil = new FileAttributeUtil();
    private File tempFile;
    private long oldTimestamp;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = File.createTempFile("temp", ".txt", null);
        oldTimestamp = fileAttributeUtil.getLastModifiedTimestamp(tempFile);
    }

    @Test
    void normalTimestampsShouldNotBeZero() {
        assertThat(oldTimestamp, greaterThan(0L));
    }

    @Test
    void settingNonZeroTimestampShouldWork() throws IOException {
        assertThat(oldTimestamp, greaterThan(10000L));

        fileAttributeUtil.setTimestamps(tempFile, 10000L);
        long timestamp = fileAttributeUtil.getLastModifiedTimestamp(tempFile);
        assertThat(timestamp, is(10000L));
    }
}
