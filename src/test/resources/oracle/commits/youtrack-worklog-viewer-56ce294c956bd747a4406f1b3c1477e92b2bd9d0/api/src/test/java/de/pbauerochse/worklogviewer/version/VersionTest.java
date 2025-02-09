package de.pbauerochse.worklogviewer.version;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by patrick on 01.11.15.
 */
public class VersionTest {

    @Test
    public void testVersions() {
        Version versionA = Version.fromVersionString("1.1.1");
        Version versionB = Version.fromVersionString("1.1.1");

        Assert.assertFalse(versionA.isNewerThan(versionB));
        Assert.assertFalse(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("1.1.2");
        Assert.assertFalse(versionA.isNewerThan(versionB));
        Assert.assertTrue(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("0.1.2");
        Assert.assertTrue(versionA.isNewerThan(versionB));
        Assert.assertFalse(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("1.2.0");
        Assert.assertFalse(versionA.isNewerThan(versionB));
        Assert.assertTrue(versionB.isNewerThan(versionA));

        versionB = Version.fromVersionString("2.0.0");
        Assert.assertFalse(versionA.isNewerThan(versionB));
        Assert.assertTrue(versionB.isNewerThan(versionA));
    }

    @Test
    public void testComparison() {
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
