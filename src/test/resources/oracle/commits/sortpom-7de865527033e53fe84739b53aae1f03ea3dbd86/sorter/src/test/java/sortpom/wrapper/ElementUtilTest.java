package sortpom.wrapper;

import org.jdom.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import refutils.ReflectionHelper;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author bjorn
 * @since 2016-06-22
 */
class ElementUtilTest {

    private Element parent;
    private Element child;

    @BeforeEach
    void setUp() {
        parent = new Element("Parent");
        child = new Element("Child");
        parent.addContent(child);
    }

    @Test
    void testConstructor() {
        ElementUtil elementUtil = ReflectionHelper.instantiatePrivateConstructor(ElementUtil.class);
        assertThat(elementUtil, not(nullValue()));
    }

    @Test
    void parentElementNameShouldBeMatched() {
        assertThat(ElementUtil.isElementParentName(child, "Parent"), is(true));
        assertThat(ElementUtil.isElementParentName(child, "Gurka"), is(false));
        assertThat(ElementUtil.isElementParentName(parent, "Parent"), is(false));
    }
}
