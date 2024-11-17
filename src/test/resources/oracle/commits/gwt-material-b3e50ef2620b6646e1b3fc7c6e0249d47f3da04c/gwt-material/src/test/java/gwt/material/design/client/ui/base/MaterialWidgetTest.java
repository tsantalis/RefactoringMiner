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
package gwt.material.design.client.ui.base;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HasEnabled;
import gwt.material.design.client.MaterialDesign;
import gwt.material.design.client.base.*;
import gwt.material.design.client.constants.*;
import gwt.material.design.client.resources.MaterialResources;
import gwt.material.design.client.resources.WithJQueryResources;
import gwt.material.design.client.ui.MaterialPanel;
import org.junit.Test;

import static gwt.material.design.jquery.client.api.JQuery.$;

/**
 * Test case for MaterialWidget base
 *
 * @author kebzlou7979
 */
public class MaterialWidgetTest extends BaseEventTest {

    @Test
    public void testBaseWidget() {
        MaterialWidget widget = new MaterialWidget(Document.get().createDivElement());
        checkWidget(widget);
    }

    public void checkJQuery() {
        MaterialDesign.injectJs(WithJQueryResources.INSTANCE.jQuery());
        MaterialDesign.injectJs(MaterialResources.INSTANCE.materializeJs());
        assertTrue(MaterialDesign.isjQueryLoaded());
        assertTrue(MaterialDesign.isMaterializeLoaded());
        // gwt-material-jquery Test
        assertNotNull($("body"));
    }

    protected <T extends MaterialWidget> void checkWidget(T widget) {
        checkJQuery();
        checkId(widget);
        checkInitialClasses(widget);
        checkEnabled(widget);
        checkColor(widget);
        checkTextAlign(widget);
        checkGrid(widget);
        checkShadow(widget);
        checkSeparator(widget);
        checkScrollspy(widget);
        checkHideOnShowOn(widget);
        checkCenterOn(widget);
        checkCircle(widget);
        checkWaves(widget);
        checkTooltip(widget);
        checkHoverable(widget);
        checkFontWeight(widget);
        checkTruncate(widget);
        checkChildren(widget);
        checkInteractionEvents(widget, true);
        checkInteractionEvents(widget, false);
    }

    protected <T extends MaterialWidget> void checkInitialClasses(T widget) {
        widget.addAttachHandler(attachEvent -> {
            assertNotNull(widget.getInitialClasses());
        });
    }

    protected <T extends MaterialWidget> void checkChildren(T widget) {
        if (widget.getChildren().size() == 0) {
            int childCount = 3;
            for (int i = 1; i <= childCount; i++) {
                MaterialPanel panel = new MaterialPanel();
                widget.add(panel);
            }
            assertTrue(widget.getChildren().size() != 0);
            assertEquals(widget.getChildren().size(), 3);
            widget.getChildren().remove(0);
            assertEquals(widget.getChildren().size(), 2);
            widget.clear();
            assertEquals(widget.getChildren().size(), 0);
        }
    }

    protected <T extends MaterialWidget> void checkTruncate(T widget) {
        final Element element = widget.getElement();
        widget.setTruncate(true);
        assertTrue(element.hasClassName(CssName.TRUNCATE));
        widget.setTruncate(false);
        assertFalse(element.hasClassName(CssName.TRUNCATE));
    }

    protected <T extends MaterialWidget> void checkFontWeight(T widget) {
        final Element element = widget.getElement();
        widget.setFontWeight(Style.FontWeight.BOLD);
        assertEquals(element.getStyle().getFontWeight(), Style.FontWeight.BOLD.getCssName());
        assertEquals(widget.getFontWeight(), Style.FontWeight.BOLD.getCssName());
    }

    protected <T extends MaterialWidget & HasHoverable> void checkHoverable(T widget) {
        final Element element = widget.getElement();
        widget.setHoverable(true);
        assertTrue(element.hasClassName(CssName.HOVERABLE));
        assertEquals(widget.isHoverable(), true);
        widget.setHoverable(false);
        assertFalse(element.hasClassName(CssName.HOVERABLE));
        assertEquals(widget.isHoverable(), false);
    }

    protected <T extends MaterialWidget & HasTooltip> void checkTooltip(T widget) {
        final Element element = widget.getElement();
        widget.setTooltip("Tooltip");
        widget.setTooltipPosition(Position.RIGHT);
        widget.setTooltipDelayMs(300);
        assertTrue(element.hasAttribute("data-tooltip"));
        assertEquals(element.getAttribute("data-tooltip"), "Tooltip");
        assertEquals(widget.getTooltip(), "Tooltip");
        assertTrue(element.hasAttribute("data-position"));
        assertEquals(element.getAttribute("data-position"), Position.RIGHT.getCssName());
        assertEquals(widget.getTooltipPosition(), Position.RIGHT);
        assertTrue(element.hasAttribute("data-delay"));
        assertEquals(element.getAttribute("data-delay"), "300");
        assertEquals(widget.getTooltipDelayMs(), 300);
        widget.setTooltipHTML("<b>Tooltip</b>");
        assertEquals(widget.getTooltipHTML(), "<b>Tooltip</b>");
    }

    protected <T extends MaterialWidget & HasWaves> void checkWaves(T widget) {
        final Element element = widget.getElement();
        widget.setWaves(WavesType.YELLOW);
        assertTrue(element.hasClassName(WavesType.YELLOW.getCssName()));
        assertEquals(widget.getWaves(), WavesType.YELLOW);
    }

    protected <T extends MaterialWidget & HasCircle> void checkCircle(T widget) {
        final Element element = widget.getElement();
        widget.setCircle(true);
        assertTrue(element.hasClassName(CssName.CIRCLE));
        widget.setCircle(false);
        assertFalse(element.hasClassName(CssName.CIRCLE));
        assertEquals(widget.isCircle(), false);
    }

    protected <T extends MaterialWidget & HasCenterOn> void checkCenterOn(T widget) {
        final Element element = widget.getElement();
        widget.setCenterOn(CenterOn.CENTER_ON_SMALL);
        assertTrue(element.hasClassName(CenterOn.CENTER_ON_SMALL.getCssName()));
        assertEquals(widget.getCenterOn(), CenterOn.CENTER_ON_SMALL);
    }

    protected <T extends MaterialWidget & HasHideOn & HasShowOn> void checkHideOnShowOn(T widget) {
        final Element element = widget.getElement();
        widget.setHideOn(HideOn.HIDE_ON_MED);
        assertTrue(element.hasClassName(HideOn.HIDE_ON_MED.getCssName()));
        assertEquals(widget.getHideOn(), HideOn.HIDE_ON_MED);
        widget.setShowOn(ShowOn.SHOW_ON_MED_UP);
        assertTrue(element.hasClassName(ShowOn.SHOW_ON_MED_UP.getCssName()));
        assertEquals(widget.getShowOn(), ShowOn.SHOW_ON_MED_UP);
    }

    protected <T extends MaterialWidget & HasScrollspy> void checkScrollspy(T widget) {
        final Element element = widget.getElement();
        widget.setScrollspy("scrollspy-1");
        assertTrue(element.hasClassName(CssName.SECTION + " " + CssName.SCROLLSPY));
        assertEquals(widget.getId(), "scrollspy-1");
        assertEquals(widget.getScrollspy(), "scrollspy-1");
    }

    protected <T extends MaterialWidget & HasSeparator> void checkSeparator(T widget) {
        final Element element = widget.getElement();
        widget.setSeparator(true);
        assertEquals("1px solid #e9e9e9", element.getStyle().getProperty("borderBottom"));
        assertEquals(widget.isSeparator(), true);
        widget.setSeparator(false);
        assertEquals("", element.getStyle().getProperty("borderBottom"));
        assertEquals(widget.isSeparator(), false);
    }

    protected <T extends MaterialWidget & HasShadow> void checkShadow(T widget) {
        final Element element = widget.getElement();
        widget.setShadow(1);
        assertTrue(element.hasClassName(CssName.Z_DEPTH_1));
        assertEquals(widget.getShadow(), 1);
    }

    protected <T extends MaterialWidget & HasGrid> void checkGrid(T widget) {
        final Element element = widget.getElement();
        widget.setGrid("s12 m12 l12");
        assertTrue(element.hasClassName("col s12 m12 l12"));
        widget.setGrid("s12");
        assertTrue(element.hasClassName("col s12"));
        widget.setGrid("s12 l12");
        assertTrue(element.hasClassName("col s12 l12"));
        widget.setOffset("s12 m12 l12");
        assertTrue(element.hasClassName("offset-s12 offset-m12 offset-l12"));
    }


    protected <T extends MaterialWidget & HasTextAlign> void checkTextAlign(T widget) {
        final Element element = widget.getElement();
        widget.setTextAlign(TextAlign.CENTER);
        assertTrue(element.hasClassName(TextAlign.CENTER.getCssName()));
        widget.setTextAlign(TextAlign.LEFT);
        assertTrue(element.hasClassName(TextAlign.LEFT.getCssName()));
        widget.setTextAlign(TextAlign.RIGHT);
        assertTrue(element.hasClassName(TextAlign.RIGHT.getCssName()));
    }

    protected <T extends MaterialWidget & HasColors> void checkColor(T widget) {
        final Element element = widget.getElement();
        widget.setTextColor(Color.WHITE);
        assertTrue(element.hasClassName(Color.WHITE.getCssName() + "-text"));
        widget.setBackgroundColor(Color.BLACK);
        assertTrue(element.hasClassName(Color.BLACK.getCssName()));
    }

    protected <T extends MaterialWidget & HasEnabled> void checkEnabled(T widget) {
        final Element element = widget.getElement();
        assertFalse(element.hasClassName(CssName.DISABLED));
        assertFalse(element.hasAttribute(CssName.DISABLED));
        widget.setEnabled(true);
        assertFalse(element.hasClassName(CssName.DISABLED));
        assertFalse(element.hasAttribute(CssName.DISABLED));
        assertEquals(widget.isEnabled(), true);
        widget.setEnabled(false);
        assertTrue(element.hasClassName(CssName.DISABLED));
        assertTrue(element.hasAttribute(CssName.DISABLED));
        assertEquals(widget.isEnabled(), false);
    }

    protected <T extends MaterialWidget & HasId> void checkId(T widget) {
        final Element element = widget.getElement();
        widget.setId("test");
        assertNotNull(widget.getId());
        assertTrue(element.hasAttribute("id"));
        assertEquals(element.getId(), widget.getId());
    }
}
