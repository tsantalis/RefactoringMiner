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

import gwt.material.design.client.base.mixin.FontSizeMixin;
import gwt.material.design.client.base.mixin.IdMixin;
import gwt.material.design.client.constants.Display;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.ui.client.adapters.HasTextEditor;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Ben Dol
 */
public abstract class AbstractTextWidget extends Widget implements HasId, HasHTML, HasInlineStyle,
        IsEditor<LeafValueEditor<String>> {

    private final IdMixin<AbstractTextWidget> idMixin = new IdMixin<>(this);
    private LeafValueEditor<String> editor;

    private final FontSizeMixin<AbstractTextWidget> fontSizeMixin = new FontSizeMixin<>(this);

    protected AbstractTextWidget(final Element element) {
        setElement(element);
    }

    @Override
    public void setId(final String id) {
        idMixin.setId(id);
    }

    @Override
    public String getId() {
        return idMixin.getId();
    }

    @Override
    public void setText(final String text) {
        getElement().setInnerText(text);
    }

    @Override
    public String getText() {
        return getElement().getInnerText();
    }

    @Override
    public String getHTML() {
        return getElement().getInnerHTML();
    }

    @Override
    public void setHTML(final String html) {
        getElement().setInnerHTML(html);
    }

    @Override
    public void setMargin(double margin) {
        getElement().getStyle().setMargin(margin, Style.Unit.PX);
    }

    @Override
    public void setMarginTop(final double margin) {
        getElement().getStyle().setMarginTop(margin, Style.Unit.PX);
    }

    @Override
    public void setMarginLeft(final double margin) {
        getElement().getStyle().setMarginLeft(margin, Style.Unit.PX);
    }

    @Override
    public void setMarginRight(final double margin) {
        getElement().getStyle().setMarginRight(margin, Style.Unit.PX);
    }

    @Override
    public void setMarginBottom(final double margin) {
        getElement().getStyle().setMarginBottom(margin, Style.Unit.PX);
    }

    @Override
    public void setPadding(double padding) {
        getElement().getStyle().setPadding(padding, Style.Unit.PX);
    }

    @Override
    public void setPaddingTop(final double padding) {
        getElement().getStyle().setPaddingTop(padding, Style.Unit.PX);
    }

    @Override
    public void setPaddingLeft(final double padding) {
        getElement().getStyle().setPaddingLeft(padding, Style.Unit.PX);
    }

    @Override
    public void setPaddingRight(final double padding) {
        getElement().getStyle().setPaddingRight(padding, Style.Unit.PX);
    }

    @Override
    public void setPaddingBottom(final double padding) {
        getElement().getStyle().setPaddingBottom(padding, Style.Unit.PX);
    }

    @Override
    public void setBackgroundColor(String bgColor) {
        getElement().getStyle().setBackgroundColor(bgColor);
    }

    @Override
    public String getBackgroundColor() {
        return getElement().getStyle().getBackgroundColor();
    }

    @Override
    public void setTextColor(String textColor) {
        getElement().getStyle().setColor(textColor);
    }

    @Override
    public String getTextColor() {
        return getElement().getStyle().getColor();
    }

    @Override
    public void setFontSize(String fontSize) {
        fontSizeMixin.setFontSize(fontSize);
    }

    @Override
    public String getFontSize() {
        return fontSizeMixin.getFontSize();
    }

    @Override
    public void setFontSize(double fontSize, Style.Unit unit) {
        fontSizeMixin.setFontSize(fontSize, unit);
    }

    @Override
    public void setOpacity(double opacity) {
        getElement().getStyle().setOpacity(opacity);
    }

    @Override
    public double getOpacity() {
        return Double.parseDouble(getElement().getStyle().getOpacity());
    }

    @Override
    public void setDisplay(Display display) {
        getElement().getStyle().setProperty("display", display.getCssName());
    }

    @Override
    public LeafValueEditor<String> asEditor() {
        if (editor == null) { editor = HasTextEditor.of(this); }
        return editor;
    }
}
