package gwt.material.design.client.base;

/*
 * #%L
 * GwtMaterial
 * %%
 * Copyright (C) 2015 - 2016 GwtMaterialDesign
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
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import gwt.material.design.client.base.error.ErrorHandler;
import gwt.material.design.client.base.error.ErrorHandlerType;
import gwt.material.design.client.base.error.HasErrorHandler;
import gwt.material.design.client.base.mixin.ErrorHandlerMixin;
import gwt.material.design.client.base.mixin.ErrorMixin;
import gwt.material.design.client.base.mixin.ValidatorMixin;
import gwt.material.design.client.base.validator.BlankValidator;
import gwt.material.design.client.base.validator.HasValidators;
import gwt.material.design.client.base.validator.ValidationChangedEvent;
import gwt.material.design.client.base.validator.Validator;

import java.util.List;

public abstract class AbstractValueWidget<V> extends MaterialWidget implements HasValue<V>, Editor<V>,
        HasEditorErrors<V>, HasErrorHandler, HasError, HasValidators<V> {

    private boolean allowBlank = true;
    private BlankValidator<V> blankValidator;

    private AttachEvent.Handler attachHandler = new AttachEvent.Handler() {
        HandlerRegistration registration;
        @Override
        public void onAttachOrDetach(AttachEvent event) {
            if(registration != null) {
                registration.removeHandler();
            }
            AbstractValueWidget inputWidget = getValidatorMixin().getInputWidget();
            registration = inputWidget.addBlurHandler(blurEvent -> {
                validate(isValidateOnBlur());
            });
        }
    };

    private ValidatorMixin<AbstractValueWidget<V>, V> validatorMixin;

    private ErrorMixin<AbstractValueWidget, ?> errorMixin;
    private final ErrorHandlerMixin<V> errorHandlerMixin = new ErrorHandlerMixin<>(this);

    public AbstractValueWidget(Element element) {
        super(element);
    }

    public AbstractValueWidget(Element element, String... initialClass) {
        super(element, initialClass);
    }

    @Override
    public void setValue(V value) {
        setValue(value, false);
    }

    @Override
    public void setValue(V value, boolean fireEvents) {
        if (fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<V> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    @Override
    public void setError(String error) {
        getErrorMixin().setError(error);
    }

    @Override
    public void setSuccess(String success) {
        getErrorMixin().setSuccess(success);
    }

    @Override
    public void setHelperText(String helperText) {
        getErrorMixin().setHelperText(helperText);
    }

    @Override
    public void clearErrorOrSuccess() {
        getErrorMixin().clearErrorOrSuccess();
    }

    @Override
    public void showErrors(List<EditorError> errors) {
        errorHandlerMixin.showErrors(errors);
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandlerMixin.getErrorHandler();
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        errorHandlerMixin.setErrorHandler(errorHandler);
    }

    @Override
    public ErrorHandlerType getErrorHandlerType() {
        return errorHandlerMixin.getErrorHandlerType();
    }

    @Override
    public void setErrorHandlerType(ErrorHandlerType errorHandlerType) {
        errorHandlerMixin.setErrorHandlerType(errorHandlerType);
    }

    @Override
    public void addValidator(Validator<V> validator) {
        getValidatorMixin().addValidator(validator);
    }

    @Override
    public boolean isValidateOnBlur() {
        return getValidatorMixin().isValidateOnBlur();
    }

    @Override
    public boolean removeValidator(Validator<V> validator) {
        return getValidatorMixin().removeValidator(validator);
    }

    @Override
    public void reset() {
        getValidatorMixin().reset();
    }

    @Override
    public void setValidateOnBlur(boolean validateOnBlur) {
        getValidatorMixin().setValidateOnBlur(validateOnBlur);
    }

    @Override
    public void setValidators(@SuppressWarnings("unchecked") Validator<V>... validators) {
        getValidatorMixin().setValidators(validators);
    }

    @Override
    public boolean validate() {
        return getValidatorMixin().validate();
    }

    @Override
    public boolean validate(boolean show) {
        return getValidatorMixin().validate(show);
    }

    @Override
    public HandlerRegistration addValidationChangedHandler(ValidationChangedEvent.ValidationChangedHandler handler) {
        return (HandlerRegistration)getValidatorMixin().addValidationChangedHandler(handler);
    }

    public void setAllowBlank(boolean allowBlank) {
        this.allowBlank = allowBlank;

        // Setup the allow blank validation
        if(!allowBlank) {
            if (blankValidator == null) {
                blankValidator = createBlankValidator();
            }
            setupBlurValidation();
            addValidator(blankValidator);
        } else {
            removeValidator(blankValidator);
        }
    }

    public boolean isAllowBlank() {
        return allowBlank;
    }

    /**
     * Hook for custom blank validators.
     *
     * @return the blank validatorMixin
     */
    protected BlankValidator<V> createBlankValidator() {
        return new BlankValidator<>();
    }

    protected HandlerRegistration setupBlurValidation() {
        AbstractValueWidget inputWidget = getValidatorMixin().getInputWidget();
        if(!inputWidget.isAttached()) {
            return inputWidget.addAttachHandler(attachHandler);
        } else {
            return inputWidget.addBlurHandler(event -> validate(isValidateOnBlur()));
        }
    }

    protected ValidatorMixin<AbstractValueWidget<V>, V> getValidatorMixin() {
        if(validatorMixin == null) {
            validatorMixin = new ValidatorMixin<>(this, getErrorHandler());
        }
        return validatorMixin;
    }

    protected ErrorMixin<AbstractValueWidget, ?> getErrorMixin() {
        if(errorMixin == null) {
            errorMixin = new ErrorMixin<>(this);
        }
        return errorMixin;
    }
}
