package org.wicketstuff.foundation;

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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.wicketstuff.foundation.tab.AjaxFoundationTab;
import org.wicketstuff.foundation.tab.FoundationTab;

public class TabPage extends BasePage
{
	public TabPage(PageParameters params)
	{
		super(params);

		List<ITab> tabs = new ArrayList<>();

		tabs.add(new AbstractTab(Model.of("title 1"))
		{

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return new TextualPanel(panelId, Model.of("This is the first panel of the basic tab example. You can place all sorts of content here including a grid."));
			}
		});

		tabs.add(new AbstractTab(Model.of("title 2"))
		{

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return new TextualPanel(panelId, Model.of("This is the second panel of the basic tab example. This is the second panel of the basic tab example."));
			}
		});
		
		tabs.add(new AbstractTab(Model.of("title 3"))
		{

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return new TextualPanel(panelId, Model.of("This is the third panel of the basic tab example. This is the third panel of the basic tab example."));
			}
		});
		
		tabs.add(new AbstractTab(Model.of("title 4"))
		{

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return new TextualPanel(panelId, Model.of("This is the fourth panel of the basic tab example. This is the fourth panel of the basic tab example."));
			}
		});

		add(new FoundationTab<>("tabHorizontal", tabs));
		add(new FoundationTab<>("tabVertical", tabs).setVerticalTab(true));
		add(new AjaxFoundationTab<>("ajaxTab", tabs));
	}
	
	class ContainerString extends WebMarkupContainer implements IMarkupResourceStreamProvider, IMarkupCacheKeyProvider
	{

		public ContainerString(String id, IModel<String> model)
		{
			super(id, model);
			setOutputMarkupId(true);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<div>" + getDefaultModelObjectAsString() + "</div>");
		}

		@Override
		public String getCacheKey(MarkupContainer container, Class<?> containerClass)
		{
			return null;
		}
	}
}
