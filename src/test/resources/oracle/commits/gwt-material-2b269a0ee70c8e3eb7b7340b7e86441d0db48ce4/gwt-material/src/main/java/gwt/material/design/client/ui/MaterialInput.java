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
import com.google.gwt.text.shared.Parser;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.testing.PassthroughParser;
import com.google.gwt.text.shared.testing.PassthroughRenderer;
import com.google.gwt.uibinder.client.UiConstructor;
import gwt.material.design.client.constants.InputType;
import gwt.material.design.client.base.HasInputType;
import gwt.material.design.client.base.ValueBoxBase;

public class  MaterialInput extends ValueBoxBase<String> implements HasInputType {

    private static final String MIN = "min";
    private static final String MAX = "max";

    public MaterialInput() {
        this(PassthroughRenderer.instance(), PassthroughParser.instance());
    }

    public MaterialInput(Renderer<String> renderer, Parser<String> parser) {
        super(Document.get().createElement("input"), renderer, parser);
    }

    @UiConstructor
    public MaterialInput(final InputType type) {
        this();
        setType(type);
    }

    public void setMin(final String min) {
        getElement().setAttribute(MIN, min);
    }

    public void setMax(final String max) {
        getElement().setAttribute(MAX, max);
    }

    @Override
    public void setType(final InputType inputType) {
        getElement().setAttribute(TYPE, inputType.getType());
    }

    @Override
    public InputType getType() {
        if (getElement().getAttribute(TYPE) == null || getElement().getAttribute(TYPE).isEmpty()) { return null; }
        return InputType.valueOf(getElement().getAttribute(TYPE));
    }

    public void setRequired(boolean required) {
        getElement().removeAttribute("required");
        if(required) {
            getElement().setAttribute("required", "");
        }
    }

    public boolean isRequired() {
        return getElement().hasAttribute("required");
    }
}