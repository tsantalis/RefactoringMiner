package gwt.material.design.client.ui.base;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.junit.client.GWTTestCase;
import gwt.material.design.client.base.MaterialWidget;

public class BaseEventTest extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "gwt.material.design.GwtMaterialDesign";
    }

    protected <T extends MaterialWidget> void checkInteractionEvents(T widget, boolean enabled) {
        checkClickAndDoubleClickEvent(widget, enabled);
        //checkAllMouseEvent(widget, enabled);
        //checkAllTouchEvent(widget, enabled);
        //checkAllGestureEvent(widget, enabled);
        //checkAllKeyEvent(widget, enabled);
        //checkFocusEvent(widget, enabled);
    }

    protected  <T extends MaterialWidget> void checkFocusEvent(T widget, boolean enabled) {
        widget.setEnabled(enabled);
        // Check Focus
        final boolean[] isFocusFired = {false};
        widget.addFocusHandler(focusEvent -> isFocusFired[0] = true);
        widget.fireEvent(new GwtEvent<FocusHandler>() {
            @Override
            public Type<FocusHandler> getAssociatedType() {
                return FocusEvent.getType();
            }

            @Override
            protected void dispatch(FocusHandler eventHandler) {
                eventHandler.onFocus(null);
            }
        });
        
        // Check Blur
        final boolean[] isBlurFired = {false};
        widget.addBlurHandler(blurEvent -> isBlurFired[0] = true);
        widget.fireEvent(new GwtEvent<BlurHandler>() {
            @Override
            public Type<BlurHandler> getAssociatedType() {
                return BlurEvent.getType();
            }

            @Override
            protected void dispatch(BlurHandler eventHandler) {
                eventHandler.onBlur(null);
            }
        });

        assertEquals(isFocusFired[0], enabled);
        assertEquals(isBlurFired[0], enabled);
    }

    protected <T extends MaterialWidget> void checkClickAndDoubleClickEvent(T widget, boolean enabled) {
        widget.setEnabled(enabled);
        // Check Click Event
        final boolean[] isClickFired = {false};
        widget.addClickHandler(clickEvent -> isClickFired[0] = true);
        widget.fireEvent(new GwtEvent<ClickHandler>() {
            @Override
            public Type<ClickHandler> getAssociatedType() {
                return ClickEvent.getType();
            }

            @Override
            protected void dispatch(ClickHandler eventHandler) {
                eventHandler.onClick(null);
            }
        });

        // Check Double Click Event
        final boolean[] isDoubleClickFired = {false};
        widget.addDoubleClickHandler(doubleClickEvent -> isDoubleClickFired[0] = true);
        widget.fireEvent(new GwtEvent<DoubleClickHandler>() {
            @Override
            public Type<DoubleClickHandler> getAssociatedType() {
                return DoubleClickEvent.getType();
            }

            @Override
            protected void dispatch(DoubleClickHandler eventHandler) {
                eventHandler.onDoubleClick(null);
            }
        });

        assertEquals(isClickFired[0], enabled);
        assertEquals(isDoubleClickFired[0], enabled);
    }

    protected <T extends MaterialWidget> void checkAllKeyEvent(T widget, boolean enabled) {
        widget.setEnabled(enabled);
        // Key Down
        final boolean[] isKeyDownFired = {false};
        widget.addKeyDownHandler(keyDownEvent -> isKeyDownFired[0] = true);
        widget.fireEvent(new GwtEvent<KeyDownHandler>() {
            @Override
            public Type<KeyDownHandler> getAssociatedType() {
                return KeyDownEvent.getType();
            }

            @Override
            protected void dispatch(KeyDownHandler eventHandler) {
                eventHandler.onKeyDown(null);
            }
        });

        // Key Up
        final boolean[] isKeyUpFired = {false};
        widget.addKeyUpHandler(keyUpEvent -> isKeyUpFired[0] = true);
        widget.fireEvent(new GwtEvent<KeyUpHandler>() {
            @Override
            public Type<KeyUpHandler> getAssociatedType() {
                return KeyUpEvent.getType();
            }

            @Override
            protected void dispatch(KeyUpHandler eventHandler) {
                eventHandler.onKeyUp(null);
            }
        });

        // Key Press
        final boolean[] isKeyPressFired = {false};
        widget.addKeyPressHandler(keyPressEvent -> isKeyPressFired[0] = true);
        widget.fireEvent(new GwtEvent<KeyPressHandler>() {
            @Override
            public Type<KeyPressHandler> getAssociatedType() {
                return KeyPressEvent.getType();
            }

            @Override
            protected void dispatch(KeyPressHandler eventHandler) {
                eventHandler.onKeyPress(null);
            }
        });

        assertEquals(isKeyDownFired[0], enabled);
        assertEquals(isKeyUpFired[0], enabled);
        assertEquals(isKeyPressFired[0], enabled);
    }

    protected <T extends MaterialWidget> void checkAllGestureEvent(T widget, boolean enabled) {
        widget.setEnabled(enabled);
        // Gesture Start
        final boolean[] isGestureStartFired = {false};
        widget.addGestureStartHandler(gestureStartEvent -> isGestureStartFired[0] = true);
        widget.fireEvent(new GwtEvent<GestureStartHandler>() {
            @Override
            public Type<GestureStartHandler> getAssociatedType() {
                return GestureStartEvent.getType();
            }

            @Override
            protected void dispatch(GestureStartHandler eventHandler) {
                eventHandler.onGestureStart(null);
            }
        });

        // Gesture End
        final boolean[] isGestureEndFired = {false};
        widget.addGestureEndHandler(gestureEndEvent -> isGestureEndFired[0] = true);
        widget.fireEvent(new GwtEvent<GestureEndHandler>() {
            @Override
            public Type<GestureEndHandler> getAssociatedType() {
                return GestureEndEvent.getType();
            }

            @Override
            protected void dispatch(GestureEndHandler eventHandler) {
                eventHandler.onGestureEnd(null);
            }
        });

        // Gesture Change
        final boolean[] isGestureChangeFired = {false};
        widget.addGestureChangeHandler(gestureChangeEvent -> isGestureChangeFired[0] = true);
        widget.fireEvent(new GwtEvent<GestureChangeHandler>() {
            @Override
            public Type<GestureChangeHandler> getAssociatedType() {
                return GestureChangeEvent.getType();
            }

            @Override
            protected void dispatch(GestureChangeHandler eventHandler) {
                eventHandler.onGestureChange(null);
            }
        });

        assertEquals(isGestureStartFired[0], enabled);
        assertEquals(isGestureChangeFired[0], enabled);
        assertEquals(isGestureEndFired[0], enabled);
    }

    protected <T extends MaterialWidget> void checkAllTouchEvent(T widget, boolean enabled) {
        widget.setEnabled(enabled);
        // Touch Start
        final boolean[] isTouchStartFired = {false};
        widget.addTouchStartHandler(touchStartEvent -> isTouchStartFired[0] = true);
        widget.fireEvent(new GwtEvent<TouchStartHandler>() {
            @Override
            public Type<TouchStartHandler> getAssociatedType() {
                return TouchStartEvent.getType();
            }

            @Override
            protected void dispatch(TouchStartHandler eventHandler) {
                eventHandler.onTouchStart(null);
            }
        });

        // Touch Move
        final boolean[] isTouchMoveFired = {false};
        widget.addTouchMoveHandler(touchMoveEvent -> isTouchMoveFired[0] = true);
        widget.fireEvent(new GwtEvent<TouchMoveHandler>() {
            @Override
            public Type<TouchMoveHandler> getAssociatedType() {
                return TouchMoveEvent.getType();
            }

            @Override
            protected void dispatch(TouchMoveHandler eventHandler) {
                eventHandler.onTouchMove(null);
            }
        });

        // Touch End
        final boolean[] isTouchEndFired = {false};
        widget.addTouchEndHandler(touchEndEvent -> isTouchEndFired[0] = true);
        widget.fireEvent(new GwtEvent<TouchEndHandler>() {
            @Override
            public Type<TouchEndHandler> getAssociatedType() {
                return TouchEndEvent.getType();
            }

            @Override
            protected void dispatch(TouchEndHandler eventHandler) {
                eventHandler.onTouchEnd(null);
            }
        });

        // Touch Cancel
        final boolean[] isTouchCancelFired = {false};
        widget.addTouchCancelHandler(touchCancelEvent -> isTouchCancelFired[0] = true);
        widget.fireEvent(new GwtEvent<TouchCancelHandler>() {
            @Override
            public Type<TouchCancelHandler> getAssociatedType() {
                return TouchCancelEvent.getType();
            }

            @Override
            protected void dispatch(TouchCancelHandler eventHandler) {
                eventHandler.onTouchCancel(null);
            }
        });

        assertEquals(isTouchStartFired[0], enabled);
        assertEquals(isTouchMoveFired[0], enabled);
        assertEquals(isTouchEndFired[0], enabled);
        assertEquals(isTouchCancelFired[0], enabled);
    }

    protected <T extends MaterialWidget> void checkAllMouseEvent(T widget, boolean enabled) {
        widget.setEnabled(enabled);
        // Check Mouse Down Event
        final boolean[] isMouseDownFired = {false};
        widget.addMouseDownHandler(mouseDownEvent -> isMouseDownFired[0] = true);
        widget.fireEvent(new GwtEvent<MouseDownHandler>() {
            @Override
            public Type<MouseDownHandler> getAssociatedType() {
                return MouseDownEvent.getType();
            }

            @Override
            protected void dispatch(MouseDownHandler eventHandler) {
                eventHandler.onMouseDown(null);
            }
        });

        // Check Mouse Up Event
        final boolean[] isMouseUpFired = {false};
        widget.addMouseUpHandler(mouseUpEvent -> isMouseUpFired[0] = true);
        widget.fireEvent(new GwtEvent<MouseUpHandler>() {
            @Override
            public Type<MouseUpHandler> getAssociatedType() {
                return MouseUpEvent.getType();
            }

            @Override
            protected void dispatch(MouseUpHandler eventHandler) {
                eventHandler.onMouseUp(null);
            }
        });

        // Check Mouse Over
        final boolean[] isMouseOverFired = {false};
        widget.addMouseOverHandler(mouseOverEvent -> isMouseOverFired[0] = true);
        widget.fireEvent(new GwtEvent<MouseOverHandler>() {
            @Override
            public Type<MouseOverHandler> getAssociatedType() {
                return MouseOverEvent.getType();
            }

            @Override
            protected void dispatch(MouseOverHandler eventHandler) {
                eventHandler.onMouseOver(null);
            }
        });

        // Check Mouse Move
        final boolean[] isMouseMoveFired = {false};
        widget.addMouseMoveHandler(mouseMoveEvent -> isMouseMoveFired[0] = true);
        widget.fireEvent(new GwtEvent<MouseMoveHandler>() {
            @Override
            public Type<MouseMoveHandler> getAssociatedType() {
                return MouseMoveEvent.getType();
            }

            @Override
            protected void dispatch(MouseMoveHandler eventHandler) {
                eventHandler.onMouseMove(null);
            }
        });

        // Check Mouse Wheel
        final boolean[] isMouseWheelFired = {false};
        widget.addMouseWheelHandler(mouseWheelEvent -> isMouseWheelFired[0] = true);
        widget.fireEvent(new GwtEvent<MouseWheelHandler>() {
            @Override
            public Type<MouseWheelHandler> getAssociatedType() {
                return MouseWheelEvent.getType();
            }

            @Override
            protected void dispatch(MouseWheelHandler eventHandler) {
                eventHandler.onMouseWheel(null);
            }
        });
        assertEquals(isMouseUpFired[0], widget.isEnabled());
        assertEquals(isMouseDownFired[0], widget.isEnabled());
        assertEquals(isMouseMoveFired[0], widget.isEnabled());
        assertEquals(isMouseWheelFired[0], widget.isEnabled());
        assertEquals(isMouseOverFired[0], widget.isEnabled());
    }
}
