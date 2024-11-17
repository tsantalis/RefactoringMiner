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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.AutoDirectionHandler;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.i18n.shared.HasDirectionEstimator;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.ValueBoxBase.TextAlignment;
import gwt.material.design.client.base.*;
import gwt.material.design.client.base.mixin.CounterMixin;
import gwt.material.design.client.base.mixin.ErrorMixin;
import gwt.material.design.client.base.mixin.FocusableMixin;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconSize;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.constants.InputType;
import gwt.material.design.client.events.*;
import gwt.material.design.client.events.DragEndEvent;
import gwt.material.design.client.events.DragEnterEvent;
import gwt.material.design.client.events.DragLeaveEvent;
import gwt.material.design.client.events.DragOverEvent;
import gwt.material.design.client.events.DragStartEvent;
import gwt.material.design.client.events.DropEvent;
import gwt.material.design.client.ui.html.Label;

//@formatter:off

/**
 * MaterialValueBox is an input field that accepts any text based string from user.
 * <h3>UiBinder Usage:</h3>
 * <pre>
 *{@code <m:MaterialTextBox placeholder="First Name" />}
 * </pre>
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#!forms">Material TextBox</a>
 * @author kevzlou7979
 * @author Ben Dol
 * @author paulux84
 */
//@formatter:on
public class MaterialValueBox<T> extends AbstractValueWidget<T> implements HasChangeHandlers, HasName,
        HasDirectionEstimator, HasText, AutoDirectionHandler.Target, IsEditor<ValueBoxEditor<T>>, HasIcon,
        HasInputType, HasPlaceholder, HasCounter {

    private String placeholder;
    private InputType type = InputType.TEXT;

    private ValueBoxEditor<T> editor;
    private Label label = new Label();
    private MaterialLabel lblName = new MaterialLabel();
    private MaterialLabel lblError = new MaterialLabel();
    private MaterialIcon icon = new MaterialIcon();

    @Editor.Ignore protected ValueBoxBase<T> valueBoxBase;

    private final CounterMixin<MaterialValueBox<T>> counterMixin = new CounterMixin<>(this);
    private final ErrorMixin<AbstractValueWidget, MaterialLabel> errorMixin = new ErrorMixin<>(this, lblError, valueBoxBase);

    private FocusableMixin<MaterialWidget> focusableMixin;

    public class MaterialValueBoxEditor<V> extends ValueBoxEditor<V> {
        private final ValueBoxBase<V> valueBoxBase;

        private MaterialValueBoxEditor(ValueBoxBase<V> valueBoxBase) {
            super(valueBoxBase);
            this.valueBoxBase = valueBoxBase;
        }

        @Override
        public void setValue(V value) {
            super.setValue(value);
            if (this.valueBoxBase.getText() != null && !this.valueBoxBase.getText().isEmpty()) {
                label.addStyleName("active");
            } else {
                label.removeStyleName("active");
            }
        }
    }

    protected MaterialValueBox() {
        super(Document.get().createDivElement(), "input-field");
    }

    public MaterialValueBox(ValueBoxBase<T> tValueBox) {
        this();
        initValueBox(tValueBox);
    }

    public void initValueBox(ValueBoxBase<T> tValueBox) {
        valueBoxBase = tValueBox;
        add(valueBoxBase);
    }

    @Deprecated
    @UiChild(limit = 1)
    public void addValueBox(ValueBoxBase<T> widget) {
        initValueBox(widget);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        String id = DOM.createUniqueId();
        valueBoxBase.getElement().setId(id);
        label.getElement().setAttribute("for", id);

        // Make valueBoxBase the primary focus target
        getFocusableMixin().setUiObject(new MaterialWidget(valueBoxBase.getElement()));
    }

    /**
     * Resets the textbox by removing its content and resetting visual state.
     */
    public void clear() {
        valueBoxBase.setText("");
        clearErrorOrSuccess();
        label.removeStyleName("active");
    }

    public void removeErrorModifiers() {
        valueBoxBase.getElement().removeClassName("valid");
        valueBoxBase.getElement().removeClassName("invalid");
        lblName.removeStyleName("green-text");
        lblName.removeStyleName("red-text");
    }

    @Override
    protected FocusableMixin<MaterialWidget> getFocusableMixin() {
        if(focusableMixin == null) { focusableMixin = new FocusableMixin<>(new MaterialWidget(valueBoxBase.getElement())); }
        return focusableMixin;
    }
    
    @Override
    public String getText() {
        return valueBoxBase.getText();
    }

    @Override
    public void setText(String text) {
        valueBoxBase.setText(text);

        if (text != null && !text.isEmpty()) {
            label.addStyleName("active");
        }
    }

    @Override
    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        if(getType() != InputType.SEARCH) {
            lblName.setText(placeholder);
        } else {
            valueBoxBase.getElement().setAttribute("placeholder", placeholder);
        }
    }

    @Override
    public InputType getType() {
        return type;
    }

    @Override
    public void setType(InputType type) {
        this.type = type;
        valueBoxBase.getElement().setAttribute("type", type.getType());
        if(getType() != InputType.SEARCH) {
            add(label);
            label.add(lblName);
            lblError.setVisible(false);
            add(lblError);
        }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<T> handler) {
        return valueBoxBase.addValueChangeHandler(event -> {
            if(isEnabled()){
                handler.onValueChange(event);
            }
        });
    }

    @Override
    public T getValue() {
        return valueBoxBase.getValue();
    }

    @Override
    public void setValue(T value, boolean fireEvents) {
        valueBoxBase.setValue(value, fireEvents);

        if (value != null && !value.toString().isEmpty()) {
            label.addStyleName("active");
        }
    }

    @Override
    public void setDirection(Direction direction) {
        valueBoxBase.setDirection(direction);
    }

    @Override
    public Direction getDirection() {
        return valueBoxBase.getDirection();
    }

    @Override
    public ValueBoxEditor<T> asEditor() {
        if (editor == null) {
            editor = new MaterialValueBoxEditor<>(valueBoxBase);
        }
        return editor;
    }

    @Override
    public DirectionEstimator getDirectionEstimator() {
        return valueBoxBase.getDirectionEstimator();
    }

    @Override
    public void setDirectionEstimator(boolean enabled) {
        valueBoxBase.setDirectionEstimator(enabled);
    }

    @Override
    public void setDirectionEstimator(DirectionEstimator directionEstimator) {
        valueBoxBase.setDirectionEstimator(directionEstimator);
    }

    @Override
    public void setName(String name) {
        valueBoxBase.setName(name);
    }

    @Override
    public String getName() {
        return valueBoxBase.getName();
    }

    @Override
    public HandlerRegistration addDragStartHandler(DragStartEvent.DragStartHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDragStart(event); }
        }, DragStartEvent.getType());
    }

    @Override
    public HandlerRegistration addDragMoveHandler(DragMoveEvent.DragMoveHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDragMove(event); }
        }, DragMoveEvent.getType());
    }

    @Override
    public HandlerRegistration addDragEndHandler(DragEndEvent.DragEndHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDragEnd(event); }
        }, DragEndEvent.getType());
    }

    @Override
    public HandlerRegistration addDropActivateHandler(DropActivateEvent.DropActivateHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDropActivate(event); }
        }, DropActivateEvent.getType());
    }

    @Override
    public HandlerRegistration addDragEnterHandler(DragEnterEvent.DragEnterHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDragEnter(event); }
        }, DragEnterEvent.getType());
    }

    @Override
    public HandlerRegistration addDragLeaveHandler(DragLeaveEvent.DragLeaveHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDragLeave(event); }
        }, DragLeaveEvent.getType());
    }

    @Override
    public HandlerRegistration addDragOverHandler(DragOverEvent.DragOverHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDragOver(event); }
        }, DragOverEvent.getType());
    }

    @Override
    public HandlerRegistration addDropDeactivateHandler(DropDeactivateEvent.DropDeactivateHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDropDeactivate(event); }
        }, DropDeactivateEvent.getType());
    }

    @Override
    public HandlerRegistration addDropHandler(DropEvent.DropHandler handler) {
        return valueBoxBase.addHandler(event -> {
            if(isEnabled()) { handler.onDrop(event); }
        }, DropEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyUpHandler(final KeyUpHandler handler) {
        return valueBoxBase.addDomHandler(event -> {
            if(isEnabled()){
                handler.onKeyUp(event);
            }
        }, KeyUpEvent.getType());
    }

    @Override
    public HandlerRegistration addChangeHandler(final ChangeHandler handler) {
        return valueBoxBase.addChangeHandler(event -> {
            if(isEnabled()){
                handler.onChange(event);
            }
        });
    }

    @Override
    public HandlerRegistration addFocusHandler(final FocusHandler handler) {
        return valueBoxBase.addFocusHandler(event -> {
            if(isEnabled()){
                handler.onFocus(event);
            }
        });
    }

    @Override
    public HandlerRegistration addBlurHandler(final BlurHandler handler) {
        return valueBoxBase.addBlurHandler(event -> {
            if(isEnabled()){
                handler.onBlur(event);
            }
        });
    }

    @Override
    public HandlerRegistration addGestureStartHandler(final GestureStartHandler handler) {
        return valueBoxBase.addGestureStartHandler(event -> {
            if(isEnabled()){
                handler.onGestureStart(event);
            }
        });
    }

    @Override
    public HandlerRegistration addGestureChangeHandler(final GestureChangeHandler handler) {
        return valueBoxBase.addGestureChangeHandler(event -> {
            if(isEnabled()){
                handler.onGestureChange(event);
            }
        });
    }

    @Override
    public HandlerRegistration addGestureEndHandler(final GestureEndHandler handler) {
        return valueBoxBase.addGestureEndHandler(event -> {
            if(isEnabled()){
                handler.onGestureEnd(event);
            }
        });
    }

    @Override
    public HandlerRegistration addKeyDownHandler(final KeyDownHandler handler) {
        return valueBoxBase.addKeyDownHandler(event -> {
            if(isEnabled()){
                handler.onKeyDown(event);
            }
        });
    }

    @Override
    public HandlerRegistration addKeyPressHandler(final KeyPressHandler handler) {
        return valueBoxBase.addKeyPressHandler(event -> {
            if(isEnabled()){
                handler.onKeyPress(event);
            }
        });
    }

    @Override
    public HandlerRegistration addMouseDownHandler(final MouseDownHandler handler) {
        return valueBoxBase.addMouseDownHandler(event -> {
            if(isEnabled()){
                handler.onMouseDown(event);
            }
        });
    }

    @Override
    public HandlerRegistration addMouseUpHandler(final MouseUpHandler handler) {
        return valueBoxBase.addMouseUpHandler(event -> {
            if(isEnabled()){
                handler.onMouseUp(event);
            }
        });
    }

    @Override
    public HandlerRegistration addMouseOutHandler(final MouseOutHandler handler) {
        return valueBoxBase.addMouseOutHandler(event -> {
            if(isEnabled()){
                handler.onMouseOut(event);
            }
        });
    }

    @Override
    public HandlerRegistration addMouseOverHandler(final MouseOverHandler handler) {
        return valueBoxBase.addMouseOverHandler(event -> {
            if(isEnabled()){
                handler.onMouseOver(event);
            }
        });
    }

    @Override
    public HandlerRegistration addMouseMoveHandler(final MouseMoveHandler handler) {
        return valueBoxBase.addMouseMoveHandler(event -> {
            if(isEnabled()){
                handler.onMouseMove(event);
            }
        });
    }

    @Override
    public HandlerRegistration addMouseWheelHandler(final MouseWheelHandler handler) {
        return valueBoxBase.addMouseWheelHandler(event -> {
            if(isEnabled()){
                handler.onMouseWheel(event);
            }
        });
    }

    @Override
    public HandlerRegistration addTouchStartHandler(final TouchStartHandler handler) {
        return valueBoxBase.addTouchStartHandler(event -> {
            if(isEnabled()){
                handler.onTouchStart(event);
            }
        });
    }

    @Override
    public HandlerRegistration addTouchMoveHandler(final TouchMoveHandler handler) {
        return valueBoxBase.addTouchMoveHandler(event -> {
            if(isEnabled()){
                handler.onTouchMove(event);
            }
        });
    }

    @Override
    public HandlerRegistration addTouchEndHandler(final TouchEndHandler handler) {
        return valueBoxBase.addTouchEndHandler(event -> {
            if(isEnabled()){
                handler.onTouchEnd(event);
            }
        });
    }

    @Override
    public HandlerRegistration addTouchCancelHandler(final TouchCancelHandler handler) {
        return valueBoxBase.addTouchCancelHandler(event -> {
            if(isEnabled()) { handler.onTouchCancel(event); }
        });
    }

    @Override
    public HandlerRegistration addDoubleClickHandler(final DoubleClickHandler handler) {
        return valueBoxBase.addDoubleClickHandler(event -> {
            if(isEnabled()) { handler.onDoubleClick(event); }
        });
    }

    @Override
    public HandlerRegistration addClickHandler(final ClickHandler handler) {
        return valueBoxBase.addClickHandler(event -> {
            if(isEnabled()) { handler.onClick(event); }
        });
    }

    @Override
    public void setError(String error) {
        super.setError(error);

        removeErrorModifiers();
        lblName.setStyleName("red-text");
        valueBoxBase.getElement().addClassName("invalid");
    }

    @Override
    public void setSuccess(String success) {
        super.setSuccess(success);

        removeErrorModifiers();
        lblName.setStyleName("green-text");
        valueBoxBase.getElement().addClassName("valid");
    }

    @Override
    public void clearErrorOrSuccess() {
        super.clearErrorOrSuccess();
        removeErrorModifiers();
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
    public void setLength(int length) {
        counterMixin.setLength(length);
    }

    @Override
    public int getLength() {
        return counterMixin.getLength();
    }

    @Editor.Ignore
    public ValueBoxBase<T> asValueBoxBase() {
        return valueBoxBase;
    }

    @Override
    public int getTabIndex() {
        return valueBoxBase.getTabIndex();
    }

    @Override
    public void setAccessKey(char key) {
        valueBoxBase.setAccessKey(key);
    }

    @Override
    public void setFocus(final boolean focused) {
        Scheduler.get().scheduleDeferred(() -> {
            valueBoxBase.setFocus(focused);
            if (focused) {
                label.addStyleName("active");
            } else {
                updateLabelActiveStyle();
            }
        });
    }

    /**
     * Updates the style of the field label according to the field value if the
     * field value is empty - null or "" - removes the label 'active' style else
     * will add the 'active' style to the field label.
     */
    protected void updateLabelActiveStyle() {
        if (this.valueBoxBase.getText() != null && !this.valueBoxBase.getText().isEmpty()) {
            label.addStyleName("active");
        } else {
            label.removeStyleName("active");
        }
    }

    public String getSelectedText() {
        return valueBoxBase.getSelectedText();
    }

    public int getSelectionLength() {
        return valueBoxBase.getSelectionLength();
    }

    public void setSelectionRange(int pos, int length) {
        valueBoxBase.setSelectionRange(pos, length);
    }

    public void setReadOnly(boolean readOnly) {
        valueBoxBase.setReadOnly(readOnly);
    }

    public boolean isReadOnly() {
        return valueBoxBase.isReadOnly();
    }

    public void setCursorPos(int pos) {
        valueBoxBase.setCursorPos(pos);
    }

    public void setAlignment(TextAlignment align) {
        valueBoxBase.setAlignment(align);
    }

    @Override
    public void setTabIndex(int tabIndex) {
        valueBoxBase.setTabIndex(tabIndex);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        valueBoxBase.setEnabled(enabled);
    }

    @Override
    protected ErrorMixin<AbstractValueWidget, MaterialLabel> getErrorMixin() {
        return errorMixin;
    }
}
