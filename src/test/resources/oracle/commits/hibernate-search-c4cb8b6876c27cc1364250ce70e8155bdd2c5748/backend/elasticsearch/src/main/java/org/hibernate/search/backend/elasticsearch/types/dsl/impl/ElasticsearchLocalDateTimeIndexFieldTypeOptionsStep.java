/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalDateTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep,
				LocalDateTime> {

	ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDateTime.class, DefaultParseConverters.LOCAL_DATE_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalDateTime> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchLocalDateTimeFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
