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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.base.MaterialWidget;
import gwt.material.design.client.ui.MaterialCollapsible.HasCollapsibleParent;
import gwt.material.design.client.ui.html.ListItem;
import gwt.material.design.client.ui.html.UnorderedList;

//@formatter:off

/**
* CollapsibleItem element to define the body 
* @author kevzlou7979
* @author Ben Dol
* @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!collapsible">Material Collapsibles</a>
*/
//@formatter:on
public class MaterialCollapsibleBody extends MaterialWidget implements HasCollapsibleParent {

    private MaterialCollapsible parent;

    /**
     * Creates empty collapsible body.
     */
    public MaterialCollapsibleBody() {
        super(Document.get().createDivElement(), "collapsible-body");
    }

    /**
     * Add other components into collapsible body.
     */
    public MaterialCollapsibleBody(final Widget... widgets) {
        this();
        for(Widget w : widgets) {
            add(w);
        }
    }

    @Override
    public void setParent(MaterialCollapsible parent) {
        this.parent = parent;

        for(Widget child : this) {
            checkActiveState(child);

            if(child instanceof HasCollapsibleParent) {
                ((HasCollapsibleParent) child).setParent(parent);
            }
        }
    }

    @Override
    public void add(final Widget child) {
        if(child instanceof UnorderedList) {
            for(Widget w : (UnorderedList) child) {
                if(w instanceof ListItem) {
                    w.getElement().getStyle().setDisplay(Style.Display.BLOCK);

                    provideActiveClickHandler(w);
                }
            }
        } else if(child instanceof ListItem) {
            child.getElement().getStyle().setDisplay(Style.Display.BLOCK);

            provideActiveClickHandler(child);
        }
        super.add(child);
    }

    @Override
    public boolean remove(Widget w) {
        if(w instanceof HasCollapsibleParent) {
            ((HasCollapsibleParent)w).setParent(null);
        }
        return super.remove(w);
    }

    public void makeActive(Widget child) {
        parent.clearActive();

        // mark the collapsible item as active
        MaterialCollapsibleItem item = findCollapsibleItemParent(child);
        if(item != null) {
            item.expand();
        }

        child.addStyleName("active");
    }

    protected void provideActiveClickHandler(final Widget child) {
        // Active click handler
        child.addDomHandler(event -> makeActive(child), ClickEvent.getType());
    }

    /**
     * Checks if this child holds the current active state.
     * If the child is or contains the active state it is applied.
     */
    protected void checkActiveState(Widget child) {
        // Check if this widget has a valid href
        String href = child.getElement().getAttribute("href");
        String url = Window.Location.getHref();
        int pos = url.indexOf("#");
        String location = pos >= 0 ? url.substring(pos, url.length()) : "";

        if(!href.isEmpty() && location.startsWith(href)) {
            ListItem li = findListItemParent(child);
            if(li != null) {
                makeActive(li);
            }
        } else if(child instanceof HasWidgets) {
            // Recursive check
            for(Widget w : (HasWidgets)child) {
                checkActiveState(w);
            }
        }
    }

    protected MaterialCollapsibleItem findCollapsibleItemParent(Widget widget) {
        if(widget != null) {
            if (widget instanceof MaterialCollapsibleItem) {
                return (MaterialCollapsibleItem) widget;
            } else {
                return findCollapsibleItemParent(widget.getParent());
            }
        }
        return null;
    }

    protected ListItem findListItemParent(Widget widget) {
        if(widget != null) {
            if (widget instanceof ListItem) {
                return (ListItem) widget;
            } else {
                return findListItemParent(widget.getParent());
            }
        }
        return null;
    }
}
