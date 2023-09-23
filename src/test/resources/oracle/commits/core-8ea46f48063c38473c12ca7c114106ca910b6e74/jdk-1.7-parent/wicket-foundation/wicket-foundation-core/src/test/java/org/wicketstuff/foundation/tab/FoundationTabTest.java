package org.wicketstuff.foundation.tab;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.Test;

public class FoundationTabTest extends WicketTestCase {
	
	public static final String THIRD_TAB_TEXT = "This is the third panel of the basic tab example. This is the third panel of the basic tab example.";
	public static final String SECOND_TAB_TEXT = "This is the second panel of the basic tab example. This is the second panel of the basic tab example.";
	public static final String FIRST_TAB_TEXT = "This is the first panel of the basic tab example. You can place all sorts of content here including a grid.";
	private final ArrayList<ITab> tabs;

	public FoundationTabTest() {
		tabs = new ArrayList<>();

		tabs.add(new AbstractTab(Model.of("title 1")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new TextualPanel(panelId, Model.of(FIRST_TAB_TEXT));
			}
		});

		tabs.add(new AbstractTab(Model.of("title 2")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new TextualPanel(panelId, Model.of(SECOND_TAB_TEXT));
			}
		});
		
		tabs.add(new AbstractTab(Model.of("title 3")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new TextualPanel(panelId, Model.of(THIRD_TAB_TEXT));
			}
		});
	}
	
	@Test
	public void renderSimpleTab() throws Exception {
		FoundationTab<ITab> tab = new FoundationTab<>("id", tabs);
		
		tester.startComponentInPage(tab);
		
		testRenderedTab();
		
		//render a vertical tab component
		tab = new FoundationTab<>("id", tabs);
		tester.startComponentInPage(tab.setVerticalTab(true));
		TagTester tagByWicketId = tester.getTagByWicketId("tabs-container");
		
		assertTrue(tagByWicketId.getAttributeContains("class", "vertical"));
	}
	
	@Test
	public void renderAjaxTab() {
		AjaxFoundationTab<ITab> tab = new AjaxFoundationTab<>("id", tabs);
		
		tester.startComponentInPage(tab);
		
		testRenderedTab();
	}

	private void testRenderedTab() {
		//must have a rendered title section for each tab.
		List<TagTester> tagsByWicketId = tester.getTagsByWicketId("title");
		assertEquals(tabs.size(), tagsByWicketId.size());
		
		for (TagTester tagTester : tagsByWicketId) {
			assertTrue(tagTester.getValue().startsWith("title"));
		}
		
		//the first tab must be rendered as default tab
		tester.assertContains(FIRST_TAB_TEXT);
		
		//click on second link and check that the resulting content is ok
		tester.clickLink("id:tabs-container:tabs:1:link");
		tester.assertContains(SECOND_TAB_TEXT);
	}
	
	class TextualPanel extends Panel implements IMarkupResourceStreamProvider, IMarkupCacheKeyProvider {

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
			return new StringResourceStream("<wicket:panel><div>" + getDefaultModelObjectAsString() + "</div></wicket:panel>");
		}
	}
}
