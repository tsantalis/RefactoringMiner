package gwt.material.design.client.base.mixin;

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

import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.UIObject;
import gwt.material.design.client.base.HasError;

/**
 * @author Ben Dol
 */
public class ErrorMixin<T extends UIObject & HasError, H extends UIObject & HasText>
        extends AbstractMixin<T> implements HasError {
    
    private H textObject;
    private UIObject target;
    private String helperText;

    public ErrorMixin(final T widget) {
        this(widget, null);
    }

    public ErrorMixin(final T widget, final H textObject) {
        this(widget, textObject, widget);
    }

    public ErrorMixin(final T widget, final H textObject, UIObject target) {
        super(widget);

        this.textObject = textObject;
        this.target = target;
    }

    @Override
    public void setError(String error) {
        if(textObject != null) {
            textObject.setText(error);
            textObject.addStyleName("field-error-label");
            textObject.removeStyleName("field-helper-label");
            textObject.removeStyleName("field-success-label");
            textObject.setVisible(true);
        }
        if(target != null) {
            target.addStyleName("field-error");
            target.removeStyleName("field-success");
        }
    }

    @Override
    public void setSuccess(String success) {
        if(textObject != null) {
            textObject.setText(success);
            textObject.addStyleName("field-success-label");
            textObject.removeStyleName("field-helper-label");
            textObject.removeStyleName("field-error-label");
            textObject.setVisible(true);
        }
        if(target != null) {
            target.addStyleName("field-success");
            target.removeStyleName("field-error");
        }
    }
    
    @Override
    public void setHelperText(String helperText) {
        this.helperText = helperText;
        clearErrorOrSuccess();
    }

    @Override
    public void clearErrorOrSuccess() {
        if(textObject != null) {
            textObject.setText(helperText == null ? "" : helperText);
            if (helperText != null) {
                textObject.addStyleName("field-helper-label");
            } else {
                textObject.removeStyleName("field-helper-label");
            }
            textObject.removeStyleName("field-error-label");
            textObject.removeStyleName("field-success-label");
            textObject.setVisible(helperText != null);
        }
        if (target != null) {
            target.removeStyleName("field-error");
            target.removeStyleName("field-success");
        }
    }
}
