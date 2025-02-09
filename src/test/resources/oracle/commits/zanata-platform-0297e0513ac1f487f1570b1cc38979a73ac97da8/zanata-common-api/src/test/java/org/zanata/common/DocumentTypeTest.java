package org.zanata.common;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.zanata.common.DocumentType.*;

import java.util.List;

public class DocumentTypeTest {

    @Test
    public void typeForExtantType() {
        assertThat(typeFor("txt"), is(PLAIN_TEXT));
    }

    @Test
    public void typeForNonExistentTypeCurrentBehaviour() {
        assertThat(typeFor("unknown"), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore
    public void typeForNonExistentTypeBetterBehaviour() {
        typeFor("unknown");
    }

    @Test
    public void typeForKnownTypeAfterDot() {
        assertThat(typeFor(".txt"), is(nullValue()));
    }

    @Test
    public void typeForKnownTypeWithPrefix() {
        assertThat(typeFor("foo.txt"), is(nullValue()));
    }

    @Test
    public void getAllExtensionsNotEmpty() {
        List<String> allExtensions = getAllExtensions();
        assertThat(allExtensions, not(empty()));
        assertThat(
                allExtensions,
                containsInAnyOrder("po", "pot", "txt", "dtd", "idml", "html",
                        "htm", "odt", "fodt", "odp", "fodp", "ods", "fods",
                        "odg", "fodg", "odb", "odf", "srt", "sbt", "sub",
                        "vtt"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAllExtensionsReadOnlyCannotAdd() {
        List<String> allExtensions = getAllExtensions();
        allExtensions.add("newExtension");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAllExtensionsReadOnlyCannotClear() {
        List<String> allExtensions = getAllExtensions();
        allExtensions.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAllExtensionsReadOnlyCannotRemove() {
        List<String> allExtensions = getAllExtensions();
        allExtensions.remove(0);
    }

    @Test
    public void getExtensionsHasCorrectValues() {
        // given: HTML has extensions "html" and "htm"
        assertThat(HTML.getExtensions().contains("html"), is(true));
        assertThat(HTML.getExtensions().contains("htm"), is(true));
        assertThat(HTML.getExtensions().contains("idml"), is(false));
    }
}
