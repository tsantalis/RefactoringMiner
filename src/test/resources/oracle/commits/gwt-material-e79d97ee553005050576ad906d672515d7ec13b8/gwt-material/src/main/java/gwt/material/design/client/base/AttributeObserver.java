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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import gwt.material.design.client.events.ObservedEvent;
import gwt.material.design.client.events.ObservedEvent.ObservedHandler;

public abstract class AttributeObserver implements HasHandlers {

    Widget widget;
    JavaScriptObject observer;
    
    public AttributeObserver(Widget widget) {
        this.widget = widget;
    }

    protected abstract JavaScriptObject createMutationObject();

    public void fireObserved(String old, String newValue) {
        ObservedEvent.fire(this, old, newValue);
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        widget.fireEvent(event);
    }

    public <H extends ObservedHandler> HandlerRegistration addObservedHandler(H handler) {
        return widget.addHandler(handler, ObservedEvent.TYPE);
    }

    public <H extends ObservedHandler> HandlerRegistration addObservedHandler(H handler, GwtEvent.Type<H> type) {
        return widget.addHandler(handler, type);
    }

    protected native JavaScriptObject getMutationFunction(String attr1, String attr2) /*-{
        var that = this;
        return function(mutations) {
            mutations.forEach(function(mutation) {
                var old = mutation.oldValue;
                var value = mutation.target[attr1][attr2];

                if(mutation.attributeName == attr1 && old !== null && old.slice(0, attr2.length) == attr2) {
                    that.@gwt.material.design.client.base.AttributeObserver::fireObserved(Ljava/lang/String;Ljava/lang/String;)(
                        old, value);
                }
            });
        }
    }-*/;
    
    protected static native JavaScriptObject getObserver(JavaScriptObject func) /*-{
       return new MutationObserver(func);
    }-*/;

    public void observe(Element e, String attr) {
        if(observer == null) {
            observer = getObserver(createMutationObject());
        }
        nativeObserve(e, attr);
    }

    public native void nativeObserve(Element e, String attr) /*-{
        this.@gwt.material.design.client.base.AttributeObserver::observer.observe(e, {
            attributes: true,
            attributeOldValue: true,
            attributeFilter: [attr]
        });
    }-*/;
}
