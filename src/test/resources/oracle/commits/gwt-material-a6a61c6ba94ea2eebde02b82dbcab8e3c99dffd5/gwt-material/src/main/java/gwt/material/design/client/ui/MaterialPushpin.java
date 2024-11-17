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

import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.js.JsPushpinOptions;

import static gwt.material.design.client.js.JsMaterialElement.$;
//@formatter:off

/**
 * Pushpin is our fixed positioning plugin. You can check out our live examples: the fixed Table of Contents on the right.
 * <h3>UiBinder Usage:</h3>
 *
 * <pre>
 * {@code
 * MaterialPushpin.apply(target, source.getOffsetHeight() + 600);
}
 * </pre>
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!pushpin">Material PushPin</a>
 * @author kevzlou7979
 */
public class MaterialPushpin {

    public static void apply(Widget widget, double offset) {
        apply(widget, 0, 0, offset);
    }

    public static void apply(Widget widget, double top, double bottom, double offset) {
        JsPushpinOptions options = new JsPushpinOptions();
        options.top = top;
        options.offset = offset;
        options.bottom = bottom;

        $(widget.getElement()).pushpin(options);
    }
}
