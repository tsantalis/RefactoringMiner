package gwt.material.design.client.base.mixin;

/*
 * #%L
 * GwtBootstrap3
 * %%
 * Copyright (C) 2013 - 2015 GwtBootstrap3
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

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;
import gwt.material.design.client.base.error.ErrorHandler;
import gwt.material.design.client.base.validator.HasValidators;
import gwt.material.design.client.base.validator.ValidationChangedEvent;
import gwt.material.design.client.base.validator.ValidationChangedEvent.ValidationChangedHandler;
import gwt.material.design.client.base.validator.Validator;
import gwt.material.design.client.base.validator.ValidatorWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Default validator mixin. Contains all of the validation logic.
 *
 * @param <W> the generic type
 * @param <V> the value type
 * 
 * @author Steven Jardine
 */
public class ValidatorMixin<W extends Widget & HasValue<V> & Editor<V>, V> implements HasValidators<V> {

    protected ErrorHandler errorHandler;

    private EventBus eventBus;

    protected W inputWidget;

    private Boolean valid = null;

    private boolean validateOnBlur;

    protected Set<ValidatorWrapper<V>> validators = new TreeSet<>();

    /**
     * Instantiates a new abstract validator mixin.
     *
     * @param inputWidget the input widget
     * @param errorHandler the error handler
     */
    public ValidatorMixin(W inputWidget, ErrorHandler errorHandler) {
        this.inputWidget = inputWidget;
        this.errorHandler = errorHandler;
        eventBus = new SimpleEventBus();

        setupValueChangeValidation();
    }

    protected HandlerRegistration setupValueChangeValidation() {
        return inputWidget.addHandler(event -> {
            validate(false);
        }, ValueChangeEvent.getType());
    }

    @Override
    public HandlerRegistration addValidationChangedHandler(ValidationChangedHandler handler) {
        return eventBus.addHandler(ValidationChangedEvent.getType(), handler);
    }

    @Override
    public void addValidator(Validator<V> validator) {
        validators.add(new ValidatorWrapper<>(validator, validators.size()));
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        eventBus.fireEvent(event);
    }

    /**
     * @return the inputWidget
     */
    public W getInputWidget() {
        return inputWidget;
    }

    @Override
    public boolean isValidateOnBlur() {
        return validateOnBlur;
    }

    @Override
    public boolean removeValidator(Validator<V> validator) {
        for (ValidatorWrapper<V> wrapper : validators) {
            if (wrapper.getValidator().equals(validator)) { return validators.remove(wrapper); }
        }
        return false;
    }

    @Override
    public void reset() {
        if (errorHandler != null) {
            errorHandler.clearErrors();
        }
    }

    /**
     * Sets the error handler.
     *
     * @param errorHandler the new error handler
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void setValidateOnBlur(boolean vob) {
        validateOnBlur = vob;
    }

    @Override
    public void setValidators(@SuppressWarnings("unchecked") Validator<V>... newValidators) {
        validators.clear();
        for (Validator<V> validator : newValidators) {
            addValidator(validator);
        }
    }

    @Override
    public boolean validate() {
        return validate(true);
    }

    /** {@inheritDoc} */
    @Override
    public boolean validate(boolean show) {
        Boolean oldValid = valid;
        valid = true;
        if (errorHandler != null && !validators.isEmpty()) {
            List<EditorError> errors = new ArrayList<EditorError>();
            for (ValidatorWrapper<V> wrapper : validators) {
                Validator<V> validator = wrapper.getValidator();
                List<EditorError> result = validator.validate(inputWidget, inputWidget.getValue());
                if (result != null && !result.isEmpty()) {
                    errors.addAll(result);
                    valid = false;
                }
            }
            if (show) {
                if (errors.size() > 0) {
                    errorHandler.showErrors(errors);
                } else {
                    errorHandler.clearErrors();
                }
            }
        }
        if (valid != oldValid) {
            eventBus.fireEvent(new ValidationChangedEvent(valid));
        }
        return valid;
    }
}
