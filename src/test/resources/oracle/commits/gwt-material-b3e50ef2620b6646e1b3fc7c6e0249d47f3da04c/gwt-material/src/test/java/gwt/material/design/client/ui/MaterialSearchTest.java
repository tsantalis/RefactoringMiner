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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.base.SearchObject;
import gwt.material.design.client.constants.InputType;
import gwt.material.design.client.ui.html.Label;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static gwt.material.design.jquery.client.api.JQuery.$;

public class MaterialSearchTest extends MaterialValueBoxTest {

    @Test
    public void testSearch() {
        MaterialSearch search = new MaterialSearch();
        checkStructure(search);
        checkValueBox(search);
    }

    // TODO Check Structure
    public <T extends MaterialSearch> void checkStructure(T widget) {
        assertEquals(widget.getType(), InputType.SEARCH);
        assertEquals(widget.getChildren().size(), 3);

        widget.onLoad();
        MaterialSearchResult searchResultWidget = widget.getSearchResultPanel();
        assertNotNull(searchResultWidget);

        List<SearchObject> objects = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            SearchObject obj = new SearchObject();
            obj.setKeyword("keyword" + i);
            obj.setLink("link"+i);
            objects.add(obj);
        }

        // Expected result by default 0
        widget.setListSearches(objects);
        assertEquals(objects.size(), widget.getListSearches().size());
        assertEquals(searchResultWidget.getChildren().size(), 0);


        final boolean[] isClickFired = new boolean[1];
        widget.addClickHandler(clickEvent -> {
            isClickFired[0] = true;
        });

        $(widget.getElement()).trigger("click", true);

        assertTrue(isClickFired[0]);
    }

    // TODO Check Active

    // TODO Set / Get Searches


}
