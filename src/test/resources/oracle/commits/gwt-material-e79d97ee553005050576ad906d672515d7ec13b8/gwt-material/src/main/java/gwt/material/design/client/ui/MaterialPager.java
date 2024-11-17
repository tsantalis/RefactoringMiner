package gwt.material.design.client.ui;
/*
 * #%L
 * GwtMaterial
 * %%
 * Copyright (C) 2015 GwtMaterialDesign
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import gwt.material.design.client.base.MaterialWidget;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.constants.WavesType;
import gwt.material.design.client.events.PageSelectionEvent;
import gwt.material.design.client.ui.html.ListItem;

import static gwt.material.design.client.events.PageSelectionEvent.PageSelectionHandler;
import static gwt.material.design.client.events.PageSelectionEvent.TYPE;

//@formatter:off

/**
 * Material Pager with page event
 * <h3>UiBinder Usage:</h3>
 * <pre>
 * {@code<m:MaterialPager  ui:field='pager' />}
 * </pre>
 *
 * @author Guaido79
 */
public class MaterialPager extends MaterialWidget {

    private int total;
    private int pageSize = 10;
    private int currentPage = 1;
    private int maxPageLinksShown = 10;
    private boolean enableIndicator;
    private String indicatorTemplate = "Page {page} of {total}";

    private int calcTotalPages;
    private int calcShowingPageFrom;
    private int calcShowingPageTo;
    private boolean calcInitialized;

    private PagerListItem linkLeft;
    private PagerListItem linkRight;

    private MaterialChip indicator;

    public MaterialPager() {
        super(Document.get().createULElement(), "pagination");
        setWaves(WavesType.DEFAULT);
        removeStyleName("waves-effect");
    }

    public MaterialPager(int total, int pageSize) {
        this();
        this.total = total;
        this.pageSize = pageSize;
    }

    public MaterialPager(int total, int pageSize, int currentPage) {
        this(total, pageSize);
        this.currentPage = currentPage;
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        init();
    }

    private void init() {
        if (!calcInitialized) {
            calcTotalPages = total / pageSize + (((double) total % (double) pageSize) > 0 ? 1 : 0);

            add(getOrCreateLiElementLeft());
            moveNextPagesRange();
            add(getOrCreateLiElementRight());
            if (enableIndicator) {
                add(createLiElementIndicator());
            }
            onPageSelection(1);

            calcInitialized = true;
        }
    }

    protected void moveNextPagesRange() {
        calcShowingPageFrom = currentPage;
        calcShowingPageTo = Math.min(currentPage + maxPageLinksShown - 1, calcTotalPages);
        createPageNumberLinks();
    }

    protected void movePreviousPagesRange() {
        calcShowingPageFrom = currentPage - maxPageLinksShown + 1;
        calcShowingPageTo = currentPage;
        createPageNumberLinks();
    }

    protected void createPageNumberLinks() {

        for (int i = 0; i < getWidgetCount(); i++) {
            final PagerListItem widget = (PagerListItem) getWidget(i);
            if (!widget.isFixed()) {
                Scheduler.get().scheduleDeferred(() -> {
                    widget.removeFromParent();
                });
            }
        }
        int insertionIndex = 1;
        for (int i = calcShowingPageFrom; i <= calcShowingPageTo; i++) {
            final PagerListItem liElementForPage = createLiElementForPage(i);

            Scheduler.get().scheduleDeferred(new InsertElementAtPositionCommand(insertionIndex++) {
                @Override
                public void execute() {
                    insert(liElementForPage, insertionIndex);
                }
            });

        }
    }

    protected PagerListItem createLiElementForPage(final int page) {
        final PagerListItem pageLiElement = new PagerListItem();
        pageLiElement.setFixed(false);
        pageLiElement.add(createLinkPage(page));

        addPageSelectionHandler(event -> {
            pageLiElement.setActive(event.getPageTo() == page);
        });

        pageLiElement.addHandler(event -> {
            onPageSelection(page);
            event.preventDefault();
            event.stopPropagation();
        }, ClickEvent.getType());

        return pageLiElement;
    }

    protected PagerListItem getOrCreateLiElementLeft() {
        linkLeft = new PagerListItem();
        linkLeft.setFixed(true);
        MaterialLink mLink = createLinkLeft();
        linkLeft.addHandler(event -> {
            if (linkLeft.isEnabled())
            onPageSelection(currentPage - 1);
            event.preventDefault();
            event.stopPropagation();
        }, ClickEvent.getType());
        this.linkLeft.add(mLink);

        addPageSelectionHandler(event ->  {
            MaterialPager.this.linkLeft.setEnabled(event.getPageTo() > 1);
        });

        return this.linkLeft;
    }

    protected PagerListItem getOrCreateLiElementRight() {
        linkRight = new PagerListItem();
        linkRight.setFixed(true);
        MaterialLink mLink = createLinkRight();
        linkRight.addHandler(event -> {
            if (linkRight.isEnabled())
            onPageSelection(currentPage + 1);
            event.stopPropagation();
            event.preventDefault();
        }, ClickEvent.getType());
        this.linkRight.add(mLink);

        addPageSelectionHandler(event -> {
            MaterialPager.this.linkRight.setEnabled(event.getPageTo() < calcTotalPages);
        });

        return this.linkRight;
    }

    protected PagerListItem createLiElementIndicator() {

        PagerListItem indicatorLi = new PagerListItem(false);
        indicatorLi.setFixed(true);
        indicatorLi.add(getOrCreateIndicator());
        return indicatorLi;
    }

    protected MaterialChip getOrCreateIndicator() {
        indicator = new MaterialChip();
        indicator.getElement().getStyle().setBackgroundColor("inherit");
        addPageSelectionHandler(event -> {
            indicator.setText(
                    indicatorTemplate
                            .replaceAll("\\{page\\}", String.valueOf(event.getPageTo()))
                    .replaceAll("\\{total\\}", String.valueOf(event.getTotalPage()))
                    );
        });

        return indicator;
    }

    protected MaterialLink createLinkPage(final int page) {
        MaterialLink link = new MaterialLink(String.valueOf(page));

        return link;
    }

    protected MaterialLink createLinkLeft() {
        final MaterialLink linkLeft = new MaterialLink(IconType.CHEVRON_LEFT);
        linkLeft.setIconPosition(IconPosition.NONE);
        return linkLeft;
    }

    protected MaterialLink createLinkRight() {
        final MaterialLink linkRight = new MaterialLink(IconType.CHEVRON_RIGHT);
        linkRight.setIconPosition(IconPosition.NONE);
        return linkRight;
    }

    protected void onPageSelection(int page) {
        this.currentPage = page;

        if (this.currentPage > calcShowingPageTo) {
            moveNextPagesRange();
        }
        if (this.currentPage < calcShowingPageFrom) {
            movePreviousPagesRange();
        }

        PageSelectionEvent event = new PageSelectionEvent();
        event.setPageFrom(this.currentPage);
        event.setPageTo(page);
        event.setTotalPage(this.calcTotalPages);

        fireEvent(event);
    }

    public void addPageSelectionHandler(PageSelectionHandler handler) {
        this.addHandler(handler, TYPE);
    }

    public boolean isEnableIndicator() {
        return enableIndicator;
    }

    public void setEnableIndicator(boolean enableIndicator) {
        this.enableIndicator = enableIndicator;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(final int total) {
        boolean needToClear = total != this.total;
        this.total = total;
        currentPage = 1;
        if (calcInitialized && needToClear) {
            this.clear();
            init();
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getMaxPageLinksShown() {
        return maxPageLinksShown;
    }

    public void setMaxPageLinksShown(int maxPageLinksShown) {
        this.maxPageLinksShown = maxPageLinksShown;
    }

    public String getIndicatorTemplate() {
        return indicatorTemplate;
    }

    /**
     * Set the paging indicator label with a custom template
     * <ul>
     * <li><strong>{page}</strong> is the current page</li>
     * <li><strong>{total}</strong> is the total page</li>
     * </ul>
     * Example
     * <pre>
     *{@code
     * Page {page} of {total}
     * }</pre>
     *
     * @param indicatorTemplate
     */
    public void setIndicatorTemplate(String indicatorTemplate) {
        this.indicatorTemplate = indicatorTemplate;
    }

    static abstract class InsertElementAtPositionCommand implements Scheduler.ScheduledCommand {
        protected int insertionIndex;

        InsertElementAtPositionCommand(int insertionIndex) {
            this.insertionIndex = insertionIndex;
        }
    }

    static class PagerListItem extends ListItem {

        private boolean fixed;
        private boolean enabled;

        public PagerListItem() {
            this(true);
        }

        public PagerListItem(boolean clickable) {
            if (clickable) {
                addStyleName("waves-effect");
                sinkEvents(Event.ONCLICK | Event.TOUCHEVENTS);
            }
        }

        public boolean isFixed() {
            return fixed;
        }

        public void setFixed(boolean fixed) {
            this.fixed = fixed;
        }

        public void setActive(boolean active) {
            if (active) {
                addStyleName("active");
            } else {
                removeStyleName("active");
            }
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (!enabled) {
                addStyleName("disabled");
            } else {
                removeStyleName("disabled");
            }
        }
    }
}
