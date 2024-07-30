package gui.webdiff.viewers.spv;

import gui.webdiff.dir.DirComparator;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

/* Created by pourya on 2024-07-22*/
public class SinglePageViewFromScratch extends AbstractSinglePageView implements Renderable {
    public SinglePageViewFromScratch(DirComparator comparator) {
        super(comparator);
    }

    @Override
    protected HtmlCanvas addJSMacros(HtmlCanvas html) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    protected void makeEachDiff(HtmlCanvas html, int i) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    protected void makeHead(HtmlCanvas html) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
