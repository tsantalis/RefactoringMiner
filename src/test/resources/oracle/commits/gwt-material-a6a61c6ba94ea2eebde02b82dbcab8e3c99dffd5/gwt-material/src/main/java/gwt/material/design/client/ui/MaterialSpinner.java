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


import gwt.material.design.client.base.MaterialWidget;
import gwt.material.design.client.ui.html.Div;

import com.google.gwt.dom.client.Document;

//@formatter:off
/**
* Material Spinner , is a circular loader for gwt material apps
* <h3>UiBinder Usage:</h3>
* 
* <pre>
* {@code 
* <m:MaterialSpinner color="red" />
}
* </pre>
* @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!loader">Material Progress</a>
* @author kevzlou7979
* @author Ben Dol
*/
//@formatter:on
public class MaterialSpinner extends MaterialWidget {

    private Div circleClipperLeft = new Div();
    private Div circleClipperRight = new Div();
    private Div circle1 = new Div();
    private Div circle2 = new Div();
    private Div circle3 = new Div();
    private Div gapPatch = new Div();

    public MaterialSpinner() {
        super(Document.get().createDivElement(), "spinner-layer");
        add(circleClipperLeft);
        circleClipperLeft.add(circle1);
        add(gapPatch);
        gapPatch.add(circle2);
        add(circleClipperRight);
        circleClipperRight.add(circle3);

        circleClipperLeft.setStyleName("circle-clipper left");
        gapPatch.setStyleName("gap-patch");
        circleClipperRight.setStyleName("circle-clipper right");

        circle1.setStyleName("circle");
        circle2.setStyleName("circle");
        circle3.setStyleName("circle");
    }

    public MaterialSpinner(String color) {
        this();
        setColor(color);
    }

    public void setColor(String color) {
        addStyleName("spinner-" + color);
    }
}
