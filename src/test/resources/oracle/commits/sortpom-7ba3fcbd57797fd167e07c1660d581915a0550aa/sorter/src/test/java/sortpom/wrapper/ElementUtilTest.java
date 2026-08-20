package sortpom.wrapper;

import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;
import refutils.ReflectionHelper;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author bjorn
 * @since 2016-06-22
 */
public class ElementUtilTest {

    private Element parent;
    private Element child;

    @Before
    public void setUp() {
        parent = new Element("Parent");
        child = new Element("Child");
        parent.addContent(child);
    }

    @Test
    public void testConstructor() {
        ElementUtil elementUtil = ReflectionHelper.instantiatePrivateConstructor(ElementUtil.class);
        assertThat(elementUtil, not(nullValue()));
    }

    @Test
    public void parentElementNameShouldBeMatched() {
        assertThat(ElementUtil.isElementParentName(child, "Parent"), is(true));
        assertThat(ElementUtil.isElementParentName(child, "Gurka"), is(false));
        assertThat(ElementUtil.isElementParentName(parent, "Parent"), is(false));
    }
}