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

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.TabType;
import gwt.material.design.client.ui.base.MaterialWidgetTest;
import org.junit.Test;

import static gwt.material.design.jquery.client.api.JQuery.$;

public class MaterialTabTest extends MaterialWidgetTest {

    @Test
    public void testTab() {
        MaterialTab tab = new MaterialTab();
        checkWidget(tab);
        //checkPreselection(tab);
        generateTabItems(tab);
        checkTypes(tab);
        RootPanel.get().add(tab);
    }

    public void generateTabItems(MaterialTab tab) {
        for (int i = 0; i < 5; i++) {
            MaterialTabItem item = new MaterialTabItem();
            MaterialLink link = new MaterialLink("Item " + i);
            link.setHref("item" + i);
            item.add(link);
            tab.add(item);
            assertTrue(item.getWidget(0) instanceof MaterialLink);
            assertEquals(tab.getWidget(i), item);
        }

        assertEquals(tab.getChildren().size(), 5);
        for (Widget w : tab.getChildren()) {
            assertNotNull(w);
            assertTrue(w instanceof MaterialTabItem);
            MaterialTabItem item = (MaterialTabItem) w;
            item.selectTab();
            //assertEquals(tab.getTabId(), "");
        }

        assertEquals(tab.getChildren().size(), 5);
    }

    public <T extends MaterialTab> void checkTypes(T widget) {
        widget.setType(TabType.DEFAULT);
        assertEquals(widget.getType(), TabType.DEFAULT);
        assertTrue(widget.getElement().getClassName().contains(TabType.DEFAULT.getCssName()));
        widget.setType(TabType.ICON);
        assertEquals(widget.getType(), TabType.ICON);
        assertTrue(widget.getElement().hasClassName(TabType.ICON.getCssName()));
    }

    // TODO Need GWT Support JSINterop on Testcases
    public <T extends MaterialTab> void checkIndicatorColor(T widget) {
        widget.setIndicatorColor(Color.AMBER);
    }

    // TODO Tab SElection test
    public <T extends MaterialTab> void checkPreselection(T widget) {
        final int TAB_INDEX = 1;
        assertNotNull(widget.getElement());
        widget.setTabIndex(TAB_INDEX);
        assertNotNull($(widget.getElement()));
        /*assertEquals(widget.getTabIndex(), TAB_INDEX);
        assertNotNull(widget.getWidget(TAB_INDEX));
        assertTrue(widget.getWidget(TAB_INDEX) instanceof MaterialTabItem);
        MaterialTabItem item = (MaterialTabItem) widget.getWidget(TAB_INDEX);
        assertNotNull(item.getWidget(0));
        assertTrue(item.getWidget(0) instanceof MaterialLink);
        MaterialLink link = (MaterialLink) item.getWidget(0);
        assertTrue(link.getElement().hasClassName(CssName.ACTIVE));*/
    }
}
