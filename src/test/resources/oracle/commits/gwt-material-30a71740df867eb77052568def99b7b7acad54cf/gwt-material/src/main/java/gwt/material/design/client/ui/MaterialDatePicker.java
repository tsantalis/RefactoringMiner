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
package gwt.material.design.client.ui;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import gwt.material.design.client.base.AbstractValueWidget;
import gwt.material.design.client.base.HasIcon;
import gwt.material.design.client.base.HasOrientation;
import gwt.material.design.client.base.HasPlaceholder;
import gwt.material.design.client.base.mixin.ErrorMixin;
import gwt.material.design.client.constants.*;
import gwt.material.design.client.js.JsDatePickerOptions;
import gwt.material.design.client.ui.html.DateInput;
import gwt.material.design.client.ui.html.Label;

import java.util.Date;

import static gwt.material.design.client.js.JsMaterialElement.$;

//@formatter:off

/**
 * Material Date Picker will provide a visual calendar to your apps.
 * <p/>
 * <h3>UiBinder Usage:</h3>
 * {@code
 * <m:MaterialDatePicker ui:field="datePicker">
 * }
 * <h3>Java Usage:</h3>
 * {@code
 * datePicker.setDate(new Date());
 * }
 *
 * @author kevzlou7979
 * @author Ben Dol
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!pickers">Material Date Picker</a>
 */
//@formatter:on
public class MaterialDatePicker extends AbstractValueWidget<Date> implements HasOrientation, HasPlaceholder,
        HasOpenHandlers<MaterialDatePicker>, HasCloseHandlers<MaterialDatePicker>, HasIcon {

    /**
     * Enum for identifying various selection types for the picker.
     */
    public enum MaterialDatePickerType {
        DAY,
        MONTH_DAY,
        YEAR_MONTH_DAY,
        YEAR
    }

    private String placeholder;
    private Date date;
    private Date dateMin;
    private Date dateMax;
    private Date dateTemp;
    private String format = "dd mmmm yyyy";
    private DateInput dateInput;
    private Label label = new Label();
    private MaterialLabel lblName = new MaterialLabel();
    private Element pickatizedDateInput;
    private MaterialLabel lblError = new MaterialLabel();
    private DatePickerLanguage language;

    private Orientation orientation = Orientation.PORTRAIT;
    private MaterialDatePickerType selectionType = MaterialDatePickerType.DAY;

    private boolean initialize = false;
    private MaterialIcon icon = new MaterialIcon();

    private ErrorMixin<AbstractValueWidget, MaterialLabel> errorMixin = new ErrorMixin<>(this, lblError, dateInput);

    public MaterialDatePicker() {
        super(Document.get().createDivElement(), "input-field");

        dateInput = new DateInput();
        add(dateInput);

        label.add(lblName);
        add(label);
        add(lblError);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (!initialize) {
            addStyleName(orientation.getCssName());
            initialize();
        } else {
            reinitialize();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();

        dateTemp = getValue();
        stop();
    }

    @Override
    public void clear() {
        if (initialize) {
            clearErrorOrSuccess();
            label.removeStyleName("active");
            dateInput.removeStyleName("valid");
            dateInput.clear();
        }
    }

    public void removeErrorModifiers() {
        dateInput.addStyleName("valid");
        dateInput.removeStyleName("invalid");
        lblName.removeStyleName("green-text");
        lblName.removeStyleName("red-text");
    }

    /**
     * Sets the type of selection options (date, month, year,...).
     *
     * @param type if <code>null</code>, {@link MaterialDatePickerType#DAY} will be used as fallback.
     */
    public void setDateSelectionType(MaterialDatePickerType type) {
        if (type != null) {
            this.selectionType = type;
        }
    }

    protected void initHandlers(Element picker) {
        JsDatePickerOptions events = new JsDatePickerOptions();
        events.open = () -> {
            onOpen();
        };

        events.close = () -> {
            onClose();
            $(picker).blur();
        };

        events.set = thing -> {
            if (thing.hasOwnProperty("clear")) {
                onClear();
            } else if (thing.hasOwnProperty("select")) {
                onSelect();
            }
        };

        $(picker).pickadate("picker").on(events);
    }

    protected void onClose() {
        CloseEvent.fire(this, this);
    }

    protected void onOpen() {
        label.addStyleName("active");
        dateInput.setFocus(true);
        OpenEvent.fire(this, this);
    }

    protected void onSelect() {
        label.addStyleName("active");
        dateInput.addStyleName("valid");

        // Ensure the value change event is
        // triggered on selecting a date.
        ValueChangeEvent.fire(this, getValue());
    }

    protected void onClear() {
        clear();
    }

    public String getPickerId() {
        return $(pickatizedDateInput).pickadate("picker").get("id").toString();
    }

    protected void initialize() {
        JsDatePickerOptions options = new JsDatePickerOptions();
        options.container = "body";
        options.format = getFormat();
        switch (getSelectionType()) {
            case MONTH_DAY:
                options.selectMonths = true;
                break;
            case YEAR_MONTH_DAY:
                options.selectYears = true;
                options.selectMonths = true;
                break;
            case YEAR:
                options.selectMonths = true;
                break;
        }
        pickatizedDateInput = $(dateInput.getElement()).pickadate(options).asElement();
        initialize = true;
        label.getElement().setAttribute("for", getPickerId());
        setDate(this.date);
        setDateMin(dateMin);
        setDateMax(dateMax);
        setPlaceholder(this.placeholder);
        initHandlers(pickatizedDateInput);
    }

    /**
     * Sets the current date of the picker.
     *
     * @param date - must not be <code>null</code>
     */
    public void setDate(Date date) {
        setValue(date);
    }

    public Date getDateMin() {
        return dateMin;
    }

    public void setDateMin(Date dateMin) {
        this.dateMin = dateMin;
        if (initialize && dateMin != null) {
            $(pickatizedDateInput).pickadate("picker").set("min", JsDate.create((double) dateMin.getTime()));
        }
    }

    protected void setPickerDateMin(JsDate date, Element picker) {

    }

    public Date getDateMax() {
        return dateMax;
    }

    public void setDateMax(Date dateMax) {
        this.dateMax = dateMax;
        if (initialize && dateMax != null) {
            $(pickatizedDateInput).pickadate("picker").set("max", JsDate.create((double) dateMax.getTime()));
        }
    }

    public void setPickerDate(JsDate date, Element picker) {
        $(picker).pickadate("picker").set("select", date, () -> {
            DOM.createFieldSet().setPropertyObject("muted", true);
        });
    }

    public Date getDate() {
        return getPickerDate();
    }

    protected Date getPickerDate() {
        try {
            return $(pickatizedDateInput).pickadate("picker").get("select", getFormat());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Clears the values of the picker field.
     */
    public void clearValues() {
        if (pickatizedDateInput != null) {
            clearValues(pickatizedDateInput);
        }
    }

    public void clearValues(Element picker) {
        $(picker).pickadate("picker").clear();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;

        if (initialize && placeholder != null) {
            lblName.setText(placeholder);
        }
    }

    public MaterialDatePickerType getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(MaterialDatePickerType selectionType) {
        this.selectionType = selectionType;
    }

    /**
     * @return the orientation
     */
    @Override
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * @param orientation the orientation to set : can be Vertical or Horizontal
     */
    @Override
    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public void setError(String error) {
        super.setError(error);

        removeErrorModifiers();
        lblName.setStyleName("red-text");
        dateInput.addStyleName("invalid");

    }

    @Override
    public void setSuccess(String success) {
        super.setSuccess(success);

        lblName.setStyleName("green-text");
        dateInput.addStyleName("valid");
    }

    @Override
    public void setHelperText(String helperText) {
        super.setHelperText(helperText);
    }

    @Override
    public void clearErrorOrSuccess() {
        super.clearErrorOrSuccess();
        removeErrorModifiers();
    }

    public String getFormat() {
        return format;
    }

    /**
     * To call before initialization.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public Date getValue() {
        return getPickerDate();
    }

    @Override
    public void setValue(Date value, boolean fireEvents) {
        if (value == null) {
            return;
        }
        this.date = value;
        if (initialize) {
            setPickerDate(JsDate.create((double) value.getTime()), pickatizedDateInput);
            label.addStyleName("active");
        }
        super.setValue(value, fireEvents);
    }

    @Override
    public HandlerRegistration addCloseHandler(final CloseHandler<MaterialDatePicker> handler) {
        return addHandler(handler, CloseEvent.getType());
    }

    @Override
    public HandlerRegistration addOpenHandler(final OpenHandler<MaterialDatePicker> handler) {
        return addHandler(handler, OpenEvent.getType());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        dateInput.setEnabled(enabled);
    }

    public DatePickerLanguage getLanguage() {
        return language;
    }

    public void setLanguage(DatePickerLanguage language) {
        this.language = language;

        if (language.getJs() != null) {
            ScriptInjector.fromString(language.getJs().getText()).setWindow(ScriptInjector.TOP_WINDOW).inject();
        }
        stop();
        reinitialize();
    }

    /**
     * Re initialize the datepicker.
     */
    public void reinitialize() {
        Scheduler.get().scheduleDeferred(() -> {
            initialize();
            if (pickatizedDateInput != null) {
                if (dateTemp != null) {
                    $(pickatizedDateInput).pickadate("picker").set("select", dateTemp, () -> {
                        DOM.createFieldSet().setPropertyObject("muted", true);
                    });
                }
            }
        });

    }

    /**
     * Stop the datepicker instance
     */
    public void stop() {
        $(pickatizedDateInput).pickadate("picker").stop();
    }

    protected void start() {
        $(pickatizedDateInput).pickadate("picker").start();
    }

    @Override
    public MaterialIcon getIcon() {
        return icon;
    }

    @Override
    public void setIconType(IconType iconType) {
        icon.setIconType(iconType);
        icon.setIconPrefix(true);
        lblError.setPaddingLeft(44);
        insert(icon, 0);
    }

    @Override
    public void setIconPosition(IconPosition position) {
        icon.setIconPosition(position);
    }

    @Override
    public void setIconSize(IconSize size) {
        icon.setIconSize(size);
    }

    @Override
    public void setIconFontSize(double size, Style.Unit unit) {
        icon.setIconFontSize(size, unit);
    }

    @Override
    public void setIconColor(String iconColor) {
        icon.setIconColor(iconColor);
    }

    @Override
    public void setIconPrefix(boolean prefix) {
        icon.setIconPrefix(prefix);
    }

    @Override
    public boolean isIconPrefix() {
        return icon.isIconPrefix();
    }

    @Override
    protected ErrorMixin<AbstractValueWidget, MaterialLabel> getErrorMixin() {
        return errorMixin;
    }
}