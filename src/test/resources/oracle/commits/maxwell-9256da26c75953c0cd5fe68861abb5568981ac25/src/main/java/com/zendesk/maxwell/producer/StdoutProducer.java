package com.zendesk.maxwell.producer;

import com.zendesk.maxwell.MaxwellAbstractRowsEvent;
import com.zendesk.maxwell.MaxwellContext;
import com.zendesk.maxwell.RowMap;
import com.zendesk.maxwell.producer.AbstractProducer;

public class StdoutProducer extends AbstractProducer {
	public StdoutProducer(MaxwellContext context) {
		super(context);
	}

	@Override
	public void push(RowMap r) throws Exception {
		if (this.context.getConfig().hasExcludedColumns())
			System.out.println(r.toJSON(this.context.getConfig().exclude_columns));
		else
			System.out.println(r.toJSON());
		this.context.setPosition(r);
	}
}
