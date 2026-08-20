package sortpom.wrapper.content;

import org.jdom.Content;
import org.jdom.Element;

/**
 * En wrapper lets its element be sorted in alphabetical order
 *
 * @author Bjorn
 */
public class AlphabeticalSortedWrapper implements Wrapper<Element> {
    private final Element element;

    public AlphabeticalSortedWrapper(final Element element) {
        this.element = element;
    }

    @Override
    public Element getContent() {
        return element;
    }

    @Override
    public boolean isBefore(final Wrapper<? extends Content> wrapper) {
        return wrapper instanceof AlphabeticalSortedWrapper
                && isBeforeAlphabeticalSortedWrapper((AlphabeticalSortedWrapper) wrapper);
    }

    private boolean isBeforeAlphabeticalSortedWrapper(AlphabeticalSortedWrapper wrapper) {
        return wrapper.getContent().getName().compareTo(getContent().getName()) > 0;
    }

    @Override
    public String toString() {
        return "AlphabeticalSortedWrapper{" +
                "element=" +
                element +
                '}';
    }
}
