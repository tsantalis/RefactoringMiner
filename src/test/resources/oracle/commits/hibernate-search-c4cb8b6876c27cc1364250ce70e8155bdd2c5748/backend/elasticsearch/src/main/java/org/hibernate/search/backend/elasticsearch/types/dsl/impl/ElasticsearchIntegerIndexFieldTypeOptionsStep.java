/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchIntegerFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchIntegerIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchIntegerIndexFieldTypeOptionsStep, Integer> {

	ElasticsearchIntegerIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Integer.class, DataTypes.INTEGER, DefaultParseConverters.INTEGER );
	}

	@Override
	protected ElasticsearchFieldCodec<Integer> completeCodec() {
		return ElasticsearchIntegerFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
