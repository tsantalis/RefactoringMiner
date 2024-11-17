package gwt.material.design.client.base;

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

import com.google.gwt.dom.client.Element;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.text.shared.Parser;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.web.bindery.event.shared.HandlerRegistration;
import gwt.material.design.client.base.error.ErrorHandler;
import gwt.material.design.client.base.error.ErrorHandlerType;
import gwt.material.design.client.base.error.HasErrorHandler;
import gwt.material.design.client.base.mixin.*;
import gwt.material.design.client.base.validator.HasBlankValidator;
import gwt.material.design.client.base.validator.HasValidators;
import gwt.material.design.client.base.validator.ValidationChangedEvent.ValidationChangedHandler;
import gwt.material.design.client.base.validator.Validator;

import java.util.List;

public class ValueBoxBase<T> extends com.google.gwt.user.client.ui.ValueBoxBase<T> implements HasId, HasColors,
        HasGrid, HasPlaceholder, HasEnabled, HasEditorErrors<T>, HasErrorHandler, HasValidators<T>, HasBlankValidator {

    private final IdMixin<ValueBoxBase<T>> idMixin = new IdMixin<>(this);
    private final ColorsMixin<ValueBoxBase<T>> colorsMixin = new ColorsMixin<>(this);
    private final GridMixin<ValueBoxBase<T>> gridMixin = new GridMixin<>(this);
    private final EnabledMixin<ValueBoxBase<T>> enabledMixin = new EnabledMixin<>(this);
    private final ErrorHandlerMixin<T> errorHandlerMixin = new ErrorHandlerMixin<>(this);
    private final BlankValidatorMixin<ValueBoxBase<T>, T> validatorMixin = new BlankValidatorMixin<>(this,
        errorHandlerMixin.getErrorHandler());

    public ValueBoxBase(Element elem, Renderer<T> renderer, Parser<T> parser) {
        super(elem, renderer, parser);
    }

    @Override
    public void setId(String id) {
        idMixin.setId(id);
    }

    @Override
    public String getId() {
        return idMixin.getId();
    }

    @Override
    public void setBackgroundColor(String bgColor) {
        colorsMixin.setBackgroundColor(bgColor);
    }

    @Override
    public String getBackgroundColor() {
        return colorsMixin.getBackgroundColor();
    }

    @Override
    public void setTextColor(String textColor) {
        colorsMixin.setTextColor(textColor);
    }

    @Override
    public String getTextColor() {
        return colorsMixin.getTextColor();
    }

    @Override
    public void setGrid(String grid) {
        gridMixin.setGrid(grid);
    }

    @Override
    public void setOffset(String offset) {
        gridMixin.setOffset(offset);
    }

    @Override
    public void setPlaceholder(final String placeHolder) {
        getElement().setAttribute("placeholder", placeHolder != null ? placeHolder : "");
    }

    @Override
    public String getPlaceholder() {
        return getElement().getAttribute("placeholder");
    }

    @Override
    public void setEnabled(boolean enabled) {
        enabledMixin.setEnabled(enabled);
    }

    @Override
    public boolean isAllowBlank() {
        return validatorMixin.isAllowBlank();
    }

    @Override
    public void setAllowBlank(boolean allowBlank) {
        validatorMixin.setAllowBlank(allowBlank);
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
    public void addValidator(Validator<T> validator) {
        validatorMixin.addValidator(validator);
    }

    @Override
    public boolean isValidateOnBlur() {
        return validatorMixin.isValidateOnBlur();
    }

    @Override
    public boolean removeValidator(Validator<T> validator) {
        return validatorMixin.removeValidator(validator);
    }

    @Override
    public void reset() {
        validatorMixin.reset();
    }

    @Override
    public void setValidateOnBlur(boolean validateOnBlur) {
        validatorMixin.setValidateOnBlur(validateOnBlur);
    }

    @Override
    public void setValidators(@SuppressWarnings("unchecked") Validator<T>... validators) {
        validatorMixin.setValidators(validators);
    }

    @Override
    public boolean validate() {
        return validatorMixin.validate();
    }

    @Override
    public boolean validate(boolean show) {
        return validatorMixin.validate(show);
    }

    @Override
    public HandlerRegistration addValidationChangedHandler(ValidationChangedHandler handler) {
        return validatorMixin.addValidationChangedHandler(handler);
    }
}
