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

import com.google.gwt.dom.client.Document;
//@formatter:off
/**
 * The section class is used for simple top and bottom padding. Just add
 * the section class to your div's containing large blocks of content.
 *
 * <h3>UiBinder Usage:</h3>
 * <pre>
 *{@code
 *<m:MaterialSection>
 *     <m:MaterialTitle title="Title" description="Description"/>
 * </m:MaterialSection>
 * }
 * </pre>
 *
 * @author kevzlou7979
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!grid">Material Section</a>
 */
//@formatter:on
public class MaterialSection extends MaterialWidget {

    public MaterialSection() {
        super(Document.get().createElement("div"), "section");
    }
}