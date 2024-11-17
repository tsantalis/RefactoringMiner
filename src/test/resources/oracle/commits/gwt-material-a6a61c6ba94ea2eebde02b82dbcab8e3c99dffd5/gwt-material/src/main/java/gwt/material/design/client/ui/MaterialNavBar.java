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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.base.HasActivates;
import gwt.material.design.client.base.HasProgress;
import gwt.material.design.client.base.HasType;
import gwt.material.design.client.base.mixin.ActivatesMixin;
import gwt.material.design.client.base.mixin.CssTypeMixin;
import gwt.material.design.client.base.mixin.ProgressMixin;
import gwt.material.design.client.constants.*;
import gwt.material.design.client.js.JsMaterialElement;
import gwt.material.design.client.ui.html.Div;
import gwt.material.design.client.ui.html.Nav;

//@formatter:off

/**
 * Material NavBar represents as a app tool bar, that contains NavBrand,
 * NavSection and initialize Material Sidenav.
 *
 * <h3>UiBinder Usage:</h3>
 * <pre>
 * {@code
 * <m:MaterialNavBar backgroundColor="blue" >
 *     <m:MaterialNavBrand href="#Test" position="LEFT">Title</m:MaterialNavBrand>
 *     <m:MaterialNavSection position="RIGHT">
 *         <m:MaterialLink  iconType="ACCOUNT_CIRCLE" iconPosition="LEFT" text="Account"  textColor="white" waves="LIGHT"/>
 *         <m:MaterialLink  iconType="AUTORENEW" iconPosition="LEFT" text="Refresh" textColor="white" waves="LIGHT"/>
 *         <m:MaterialLink  iconType="SEARCH" tooltip="Menu" textColor="white" waves="LIGHT"/>
 *          <m:MaterialLink  iconType="MORE_VERT" tooltip="Starter" textColor="white" waves="LIGHT"/>
 *     </m:MaterialNavSection>
 * </m:MaterialNavBar>
 * }
 *<pre>
 * @author kevzlou7979
 * @author Ben Dol
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!navbar">Material Nav Bar</a>
 */
//@formatter:on
public class MaterialNavBar extends Nav implements HasActivates, HasProgress, HasType<NavBarType> {

    private Div div = new Div();

    private MaterialLink navMenu = new MaterialLink(IconType.MENU);

    private final CssTypeMixin<NavBarType, MaterialNavBar> typeMixin = new CssTypeMixin<>(this);
    private final ActivatesMixin<MaterialLink> activatesMixin = new ActivatesMixin<>(navMenu);
    private final ProgressMixin<MaterialNavBar> progressMixin = new ProgressMixin<>(this);

    public MaterialNavBar() {
        div.setStyleName("nav-wrapper");
        div.add(navMenu);
        super.add(div);
        navMenu.setFontSize(2.7, Style.Unit.EM);
        navMenu.addStyleName("button-collapse");
        navMenu.setHideOn(HideOn.HIDE_ON_LARGE);
        navMenu.getElement().getStyle().clearDisplay();
        navMenu.setCircle(true);
        navMenu.setWaves(WavesType.LIGHT);
        navMenu.setWidth("64px");
        navMenu.setTextAlign(TextAlign.CENTER);
        navMenu.setIconPosition(IconPosition.NONE);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        if (typeMixin.getType() != null) {
            applyType(typeMixin.getType().getCssName(), getElement());
        }
    }

    @Override
    public void add(Widget child) {
        div.add(child);
    }

    @Override
    public void clear() {
        div.clear();
    }

    @Override
    public void setType(NavBarType type) {
        typeMixin.setType(type);
    }

    protected void applyType(String type, Element element) {
        if(type.equals("navbar-shrink")) {
            JsMaterialElement.initShrink(element, 300);
        } else {
            GWT.log("Default type of navbar was applied");
        }
    }

    @Override
    public NavBarType getType() {
        return typeMixin.getType();
    }

    @Override
    public void showProgress(ProgressType type) {
        progressMixin.showProgress(type);
    }

    @Override
    public void setPercent(double percent) {
        progressMixin.setPercent(percent);
    }

    @Override
    public void hideProgress() {
        progressMixin.hideProgress();
    }

    @Override
    public void setActivates(String activates) {
        activatesMixin.setActivates(activates);
    }

    @Override
    public String getActivates() {
        return activatesMixin.getActivates();
    }
}
