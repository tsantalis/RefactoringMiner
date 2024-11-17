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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.base.HasActivates;
import gwt.material.design.client.base.HasWaves;
import gwt.material.design.client.base.MaterialWidget;
import gwt.material.design.client.base.helper.DOMHelper;
import gwt.material.design.client.constants.Alignment;
import gwt.material.design.client.js.JsDropdownOptions;
import gwt.material.design.client.ui.html.ListItem;
import gwt.material.design.client.ui.html.UnorderedList;

import java.util.ArrayList;
import java.util.List;

import static gwt.material.design.client.js.JsMaterialElement.$;

//@formatter:off

/**
 * You can add dropdown easily by specifying it's item
 * content and add a UiHandler on it to implement any event.
 *
 * <h3>UiBinder Usage:</h3>
 * <pre>
 *{@code
 *<m:MaterialDropDown>
 *   <m:MaterialLink text="First" />
 *   <m:MaterialLink text="Second" />
 *   <m:MaterialLink text="Third" />
 * </m:MaterialDropDown>
 * }
 * </pre>
 * @author kevzlou7979
 * @author Ben Dol
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!dropdown">Material DropDowns</a>
 */
//@formatter:on
public class MaterialDropDown extends UnorderedList implements HasSelectionHandlers<Widget> {

    private String activator;
    private Element activatorElem;

    // Options
    private int inDuration = 300;
    private int outDuration = 225;
    private boolean constrainWidth = true;
    private boolean hover = false;
    private boolean belowOrigin = false;
    private int gutter = 0;
    private String alignment = Alignment.LEFT.getCssName();
    private List<Widget> children = new ArrayList<>();

    public MaterialDropDown() {
        setInitialClasses("dropdown-content");
        setId(DOM.createUniqueId());
    }

    /**
     * Material Dropdown - adds a list item selection when button, link, icon button pressed.
     * @param activator - data-activates attribute name of your dropdown activator.
     */
    @UiConstructor
    public MaterialDropDown(String activator) {
        this();
        this.activator = activator;
        getElement().setId(this.activator);
    }

    public MaterialDropDown(Element activatorElem) {
        this();
        activatorElem.setAttribute("data-activates", getId());
        this.activatorElem = activatorElem;
    }

    public MaterialDropDown(UIObject activator) {
        this(activator.getElement());
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        initialize();
    }

    /**
     * The duration of the transition enter in milliseconds. Default: 300
     */
    public void setInDuration(int durationMillis) {
        this.inDuration = durationMillis;
    }

    public int getInDuration() {
        return inDuration;
    }

    /**
     * The duration of the transition out in milliseconds. Default: 225
     */
    public void setOutDuration(int durationMillis) {
        this.outDuration = durationMillis;
    }

    public int getOutDuration() {
        return outDuration;
    }

    /**
     * If true, constrainWidth to the size of the dropdown activator. Default: true
     */
    public void setConstrainWidth(boolean constrainWidth) {
        this.constrainWidth = constrainWidth;
    }

    public boolean isConstrainWidth() {
        return constrainWidth;
    }

    /**
     * If true, the dropdown will open on hover. Default: false
     */
    public void setHover(boolean hover) {
        this.hover = hover;
    }

    public boolean isHover() {
        return hover;
    }

    /**
     * This defines the spacing from the aligned edge. Default: 0
     */
    public void setGutter(int gutter) {
        this.gutter = gutter;
    }

    public int getGutter() {
        return gutter;
    }

    /**
     * If true, the dropdown will show below the activator. Default: false
     */
    public void setBelowOrigin(boolean belowOrigin) {
        this.belowOrigin = belowOrigin;
    }

    public boolean isBelowOrigin() {
        return belowOrigin;
    }

    /**
     * Defines the edge the menu is aligned to. Default: 'left'
     */
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment.getCssName();
    }

    public Alignment getAlignment() {
        return Alignment.fromStyleName(alignment);
    }

    /**
     * Get the unique activator set by material widget e.g links, icons, buttons to trigger the dropdown.
     */
    public String getActivator() {
        return activator;
    }

    /**
     * Set the unique activator of each dropdown component and it must be unique
     */
    public void setActivator(String activator) {
        this.activator = activator;
    }

    @Override
    public void add(final Widget child) {
        String tagName = child.getElement().getTagName();
        if(child instanceof ListItem || tagName.toLowerCase().startsWith("li")) {
            child.getElement().getStyle().setDisplay(Style.Display.BLOCK);
            add(child, (Element) getElement());
        } else {
            ListItem li = new ListItem(child);
            children.add(child);
            child.addDomHandler(event -> {
                SelectionEvent.fire(MaterialDropDown.this, child);
            }, ClickEvent.getType());

            // Checks if there are sub dropdown components

            if(child instanceof MaterialLink) {
                MaterialLink link = (MaterialLink) child;
                for(int i = 0; i < link.getWidgetCount(); i++) {
                    if(link.getWidget(i) instanceof MaterialDropDown) {
                        link.addClickHandler(event -> event.stopPropagation());
                        link.stopTouchStartEvent();
                    }
                }
            }

            if(child instanceof HasWaves) {
                li.setWaves(((HasWaves) child).getWaves());
                ((HasWaves) child).setWaves(null);
            }
            li.getElement().getStyle().setDisplay(Style.Display.BLOCK);
            add(li, (Element) getElement());
        }
    }

    protected void initialize() {
        Widget parent = getParent();
        if(parent instanceof HasActivates) {
            String uid = DOM.createUniqueId();
            ((HasActivates) parent).setActivates(uid);
            setId(uid);
            activatorElem = parent.getElement();
        }
        else if(activatorElem == null) {
            activatorElem = DOMHelper.getElementByAttribute("data-activates", activator);
            if (activatorElem == null) {
                throw new IllegalStateException("There is no activator element with id: '" + activator
                    + "' in the DOM, cannot instantiate MaterialDropDown without a data-activates.");
            }
        }

        initialize(activatorElem);
    }

    /**
     * Must be called after changing any options.
     */
    public void reinitialize() {
        remove(activatorElem);
        initialize(activatorElem);
    }

    protected void initialize(Element activator) {
        JsDropdownOptions options = new JsDropdownOptions();
        options.inDuration = inDuration;
        options.outDuration = outDuration;
        options.hover = hover;
        options.gutter = gutter;
        options.belowOrigin = belowOrigin;
        options.alignment = alignment;
        $(activator).dropdown(options);
    }

    protected void remove(Element activator) {
        $(activator).dropdown("remove");
    }

    @Override
    public HandlerRegistration addSelectionHandler(final SelectionHandler<Widget> handler) {
        return addHandler(new SelectionHandler<Widget>() {
            @Override
            public void onSelection(SelectionEvent<Widget> event) {
                Widget widget = event.getSelectedItem();
                if(widget instanceof HasEnabled) {
                    if(((HasEnabled) widget).isEnabled() && isEnabled()) {
                        handler.onSelection(event);
                    }
                }
            }
        }, SelectionEvent.getType());
    }

    public List<Widget> getItems() {
        return children;
    }
}
