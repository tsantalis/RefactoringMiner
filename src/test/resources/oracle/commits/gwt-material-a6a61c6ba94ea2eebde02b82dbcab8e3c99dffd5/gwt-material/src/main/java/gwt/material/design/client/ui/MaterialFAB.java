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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import gwt.material.design.client.base.HasAxis;
import gwt.material.design.client.base.HasType;
import gwt.material.design.client.base.MaterialWidget;
import gwt.material.design.client.base.mixin.CssNameMixin;
import gwt.material.design.client.base.mixin.CssTypeMixin;
import gwt.material.design.client.constants.Axis;
import gwt.material.design.client.constants.FABType;

import static gwt.material.design.client.js.JsMaterialElement.$;

//@formatter:off

/**
 * Floating action buttons are used for a promoted action. They
 * are distinguished by a circled icon floating above the UI and
 * have motion behaviors that include morphing, launching, and a
 * transferring anchor point.
 *
 * <h3>UiBinder Usage:</h3>
 * <pre>
 *{@code
 *<m:MaterialFAB>
 *   <m:MaterialButton type="FLOATING" backgroundColor="blue" iconType="POLYMER" size="LARGE"/>
 *   <m:MaterialFABList>
 *     <m:MaterialButton type="FLOATING" backgroundColor="red" iconType="POLYMER"/>
 *     <m:MaterialButton type="FLOATING" backgroundColor="orange" iconType="POLYMER"/>
 *     <m:MaterialButton type="FLOATING" backgroundColor="white" iconType="POLYMER" iconColor="black"/>
 *   </m:MaterialFABList>
 * </m:MaterialFAB>}
 * </pre>
 * </p>
 *
 * @author kevzlou7979
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!buttons">Material FAB</a>
 */
//@formatter:on
public class MaterialFAB extends MaterialWidget implements HasType<FABType>, HasAxis {

    private final CssTypeMixin<FABType, MaterialFAB> typeMixin = new CssTypeMixin<>(this);
    private final CssNameMixin<MaterialFAB, Axis> axisMixin = new CssNameMixin<>(this);
    private boolean toggle = true;

    public MaterialFAB() {
        super(Document.get().createDivElement(), "fixed-action-btn");
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        if(getType() == FABType.CLICK_ONLY) {
            addClickHandler(clickEvent -> {
                if(toggle) {
                    openFAB();
                    toggle = false;
                } else {
                    closeFAB();
                    toggle = true;
                }
            });
        }
    }

    @Override
    public void setType(FABType type) {
        typeMixin.setType(type);
    }

    @Override
    public FABType getType() {
        return typeMixin.getType();
    }

    @Override
    public void setAxis(Axis axis) {
        axisMixin.setCssName(axis);
    }

    @Override
    public Axis getAxis() {
        return axisMixin.getCssName();
    }

    /**
     * Open the FAB programmatically
     */
    public void openFAB() {
        $(getElement()).openFAB();
    }

    /**
     * Close the FAB programmatically
     */
    public void closeFAB() {
        $(getElement()).closeFAB();
    }
}
