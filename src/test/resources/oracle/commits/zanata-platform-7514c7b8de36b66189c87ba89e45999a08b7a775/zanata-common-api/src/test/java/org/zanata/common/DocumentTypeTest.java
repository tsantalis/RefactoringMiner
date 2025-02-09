package org.zanata.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.zanata.common.DocumentType.*;

import java.util.List;

import org.testng.annotations.Test;

@Test(groups = { "unit-tests" })
public class DocumentTypeTest {

    public void typeForExtantType() {
        assertThat(typeFor("txt"), is(PLAIN_TEXT));
    }

    @Test(enabled = true)
    public void typeForNonExistentTypeCurrentBehaviour() {
        assertThat(typeFor("unknown"), is(nullValue()));
    }

    @Test(enabled = false, expectedExceptions = IllegalArgumentException.class)
    public void typeForNonExistentTypeBetterBehaviour() {
        typeFor("unknown");
    }

    public void typeForKnownTypeAfterDot() {
        assertThat(typeFor(".txt"), is(nullValue()));
    }

    public void typeForKnownTypeWithPrefix() {
        assertThat(typeFor("foo.txt"), is(nullValue()));
    }

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

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void getAllExtensionsReadOnlyCannotAdd() {
        List<String> allExtensions = getAllExtensions();
        allExtensions.add("newExtension");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void getAllExtensionsReadOnlyCannotClear() {
        List<String> allExtensions = getAllExtensions();
        allExtensions.clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void getAllExtensionsReadOnlyCannotRemove() {
        List<String> allExtensions = getAllExtensions();
        allExtensions.remove(0);
    }

    public void getExtensionsHasCorrectValues() {
        // given: HTML has extensions "html" and "htm"
        assertThat(HTML.getExtensions().contains("html"), is(true));
        assertThat(HTML.getExtensions().contains("htm"), is(true));
        assertThat(HTML.getExtensions().contains("idml"), is(false));
    }
}
