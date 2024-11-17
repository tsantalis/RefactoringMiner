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
package gwt.material.design.client.ui;

import gwt.material.design.client.base.MaterialWidget;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.ui.html.Div;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HasVisibility;
import com.google.gwt.user.client.ui.Widget;

//@formatter:off
/**
 * An initial screen that act as a loading screen
 * in order for your apps to load fully.
 *
 * <h3>UiBinder Usage:</h3>
 * <pre>
 *{@code

<m:MaterialSplashScreen backgroundColor="blue" textColor="white" textAlign="CENTER">
    <m:MaterialImage resource="{res.ic_splash}" width="300px"/>
    <m:MaterialTitle title="gwt-material" description="Material Design Look and Feel for GWT Apps" />
</m:MaterialSplashScreen>

 * }</pre>
 * <h3>Java Usage:</h3>
 * <pre>
 *{@code
 *
@UiField MaterialSplashScreen splash;
splash.show();
Timer t = new Timer() {
    @Override
    public void run() {
        splash.hide();
    }
};
t.schedule(3000);

 * }</pre>
 * @author kevzlou7979
 * @author Ben Dol
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!showcase">Material Splashscreen</a>
 */
//@formatter:on
public class MaterialSplashScreen extends MaterialWidget {

    private Div div = new Div();
    private MaterialProgress progress = new MaterialProgress();

    public MaterialSplashScreen() {
        super(Document.get().createDivElement(), "splash-screen");
        setDisplay(Display.NONE);

        div.setWidth("100%");
        div.getElement().getStyle().setMarginTop(15, Style.Unit.PCT);

        super.add(div);
        super.add(progress);
    }

    @Override
    public void add(Widget child) {
        div.add(child);
    }

    public void show() {
        getElement().getStyle().setDisplay(Display.BLOCK.getGwtDisplay());
    }

    public void hide() {
        getElement().getStyle().setDisplay(Display.NONE.getGwtDisplay());
    }
}
