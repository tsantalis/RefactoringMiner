package org.wicketstuff.foundation.tab;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.Test;

public class FoundationTabTest extends WicketTestCase {
	
	private final ArrayList<ITab> tabs;

	public FoundationTabTest() {
		tabs = new ArrayList<>();

		tabs.add(new AbstractTab(Model.of("title 1")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new TextualPanel(panelId, Model.of("This is the first panel of the basic tab example. You can place all sorts of content here including a grid."));
			}
		});

		tabs.add(new AbstractTab(Model.of("title 2")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new TextualPanel(panelId, Model.of("This is the second panel of the basic tab example. This is the second panel of the basic tab example."));
			}
		});
		
		tabs.add(new AbstractTab(Model.of("title 3")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new TextualPanel(panelId, Model.of("This is the third panel of the basic tab example. This is the third panel of the basic tab example."));
			}
		});
	}
	
	@Test
	public void renderSimpleTab() throws Exception {
		FoundationTab<ITab> tab = new FoundationTab<>("id", tabs);
		
		tester.startComponentInPage(tab);
		
		//must have the three rendered titles for the tabs.
		List<TagTester> tagsByWicketId = tester.getTagsByWicketId("title");
		assertEquals(tabs.size(), tagsByWicketId.size());
		
		for (TagTester tagTester : tagsByWicketId) {
			assertTrue(tagTester.getValue().startsWith("title"));
		}
		
		//render a vertical tab component
		tab = new FoundationTab<>("id", tabs);
		tester.startComponentInPage(tab.setVerticalTab(true));
		TagTester tagByWicketId = tester.getTagByWicketId("tabs-container");
		
		assertTrue(tagByWicketId.getAttributeContains("class", "vertical"));
	}
	
	class TextualPanel extends WebMarkupContainer implements IMarkupResourceStreamProvider, IMarkupCacheKeyProvider {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public TextualPanel(String id, IModel<?> model) {
			super(id, model);
		}

		@Override
		public String getCacheKey(MarkupContainer container, Class<?> containerClass) {
			return null;
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass) {
			return new StringResourceStream("<div>" + getDefaultModelObjectAsString() + "</div>");
		}

		@Override
		protected void onRender()
		{
			
		}
	}
}
