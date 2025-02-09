package de.pbauerochse.worklogviewer.version;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Test
    void testVersions() {
        Version versionA = Version.fromVersionString("1.1.1");
        Version versionB = Version.fromVersionString("1.1.1");

        assertFalse(versionA.isNewerThan(versionB));
        assertFalse(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("1.1.2");
        assertFalse(versionA.isNewerThan(versionB));
        assertTrue(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("0.1.2");
        assertTrue(versionA.isNewerThan(versionB));
        assertFalse(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("1.2.0");
        assertFalse(versionA.isNewerThan(versionB));
        assertTrue(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("2.0.0");
        assertFalse(versionA.isNewerThan(versionB));
        assertTrue(versionB.isNewerThan(versionA));
    }

    @Test
    void testComparison() {
        List<Version> versions = new ArrayList<>();
        versions.add(new Version(2018, 2, 0));
        versions.add(new Version(2017, 4, 0));
        versions.add(new Version(7, 5, 32));
        versions.add(new Version(2018, 1, 0));
        versions.add(new Version(2018, 1, 4));

        // when
        versions.sort(Comparator.naturalOrder());

        // then
        assertThat(versions.get(0).toString(), is("v7.5.32"));
        assertThat(versions.get(1).toString(), is("v2017.4.0"));
        assertThat(versions.get(2).toString(), is("v2018.1.0"));
        assertThat(versions.get(3).toString(), is("v2018.1.4"));
        assertThat(versions.get(4).toString(), is("v2018.2.0"));
    }

}
