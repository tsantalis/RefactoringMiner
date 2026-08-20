// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link Tag2Link}
 */
class Tag2LinkTest {

    List<String> links = new ArrayList<>();

    void addLink(String name, String url, ImageResource icon) {
        links.add(name + " // " + url);
    }

    void checkLinks(String... expected) {
        Assert.assertEquals(Arrays.asList(expected), links);
    }

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of function {@link Tag2Link#initialize()}.
     */
    @Test
    void testInitialize() {
        Tag2Link.initialize();
        Assert.assertTrue("obtains at least 40 rules", Tag2Link.wikidataRules.size() > 40);
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testName() {
        Tag2Link.getLinksForTag("name", "foobar", this::addLink);
        checkLinks("Search on duckduckgo.com // https://duckduckgo.com/?q=foobar",
                "Search on google.com // https://www.google.com/search?q=foobar");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testWebsite() {
        Tag2Link.getLinksForTag("website", "http://www.openstreetmap.org/", this::addLink);
        checkLinks("Open openstreetmap.org // http://www.openstreetmap.org/");
        links.clear();
        Tag2Link.getLinksForTag("website", "https://www.openstreetmap.org/", this::addLink);
        checkLinks("Open openstreetmap.org // https://www.openstreetmap.org/");
        links.clear();
        Tag2Link.getLinksForTag("website", "www.openstreetmap.org", this::addLink);
        checkLinks("Open openstreetmap.org // http://www.openstreetmap.org");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testWikipedia() {
        Tag2Link.getLinksForTag("wikipedia", "de:Wohnhausgruppe Herderstraße", this::addLink);
        checkLinks("View Wikipedia article // https://de.wikipedia.org/wiki/Wohnhausgruppe_Herderstraße");
        links.clear();
        Tag2Link.getLinksForTag("wikipedia", "de:Stadtbahn Köln#Innenstadttunnel", this::addLink);
        checkLinks("View Wikipedia article // https://de.wikipedia.org/wiki/Stadtbahn_Köln#Innenstadttunnel");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testImageCommonsImage() {
        Tag2Link.getLinksForTag("image", "File:Witten Brücke Gasstraße.jpg", this::addLink);
        checkLinks("View image on Wikimedia Commons // https://commons.wikimedia.org/wiki/File%3AWitten_Br%C3%BCcke_Gasstra%C3%9Fe.jpg");
        links.clear();
        // non-regression test for #19754
        Tag2Link.getLinksForTag("image", "File:Foo.jpg;File:Bar.jpg", this::addLink);
        checkLinks("View image on Wikimedia Commons // https://commons.wikimedia.org/wiki/File%3AFoo.jpg",
                "View image on Wikimedia Commons // https://commons.wikimedia.org/wiki/File%3ABar.jpg");
        links.clear();
        // non-regression test for #19771
        Tag2Link.getLinksForTag("image", "File:Côte de granite rose - Trégastel à Ploumanac'h - 20190723 - 025.jpg", this::addLink);
        checkLinks("View image on Wikimedia Commons // " +
                "https://commons.wikimedia.org/wiki/" +
                "File%3AC%C3%B4te_de_granite_rose_-_Tr%C3%A9gastel_%C3%A0_Ploumanac%27h_-_20190723_-_025.jpg");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testImageCommonsCategory() {
        Tag2Link.getLinksForTag("image", "category:JOSM", this::addLink);
        checkLinks("View category on Wikimedia Commons // https://commons.wikimedia.org/wiki/category%3AJOSM");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testBrandWikidata() {
        Tag2Link.getLinksForTag("brand:wikidata", "Q259340", this::addLink);
        checkLinks("View Wikidata item // https://www.wikidata.org/wiki/Q259340");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testArchipelagoWikidata() {
        Tag2Link.getLinksForTag("archipelago:wikidata", "Q756987;Q756988", this::addLink);
        checkLinks("View Wikidata item // https://www.wikidata.org/wiki/Q756987",
                "View Wikidata item // https://www.wikidata.org/wiki/Q756988");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     *
     * Non-regression test for https://josm.openstreetmap.de/ticket/19754#comment:9
     */
    @Test
    void testMultipleSources() {
        Tag2Link.getLinksForTag("source", "https://foo.com/; https://bar.com/; https://baz.com/", this::addLink);
        checkLinks("Open foo.com // https://foo.com/",
                "Open bar.com // https://bar.com/",
                "Open baz.com // https://baz.com/");
    }

}
