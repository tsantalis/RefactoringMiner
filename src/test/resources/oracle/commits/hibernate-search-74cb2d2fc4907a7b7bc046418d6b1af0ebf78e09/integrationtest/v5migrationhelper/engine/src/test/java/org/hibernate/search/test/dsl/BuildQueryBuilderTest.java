/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2521")
class BuildQueryBuilderTest {


	@RegisterExtension
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder(
			ConfiguredNotIndexed.class, ConfiguredIndexed.class );

	@Test
	void forEntity_configured_indexed() {
		QueryBuilder builder = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( ConfiguredIndexed.class ).get();
		assertThat( new MatchAllDocsQuery() ).isEqualTo( builder.all().createQuery() );
	}

	@Test
	void forEntity_configured_notIndexed() {
		assertThatThrownBy(
				() -> sfHolder.getSearchFactory().buildQueryBuilder().forEntity( ConfiguredNotIndexed.class ).get() )
				.hasMessageContainingAll(
						"No matching indexed entity types for types: [" + ConfiguredNotIndexed.class.getName() + "]",
						"These types are not indexed entity types, nor is any of their subtypes"
				);
	}

	@Test
	void forEntity_notConfigured_indexed() {
		assertThatThrownBy(
				() -> sfHolder.getSearchFactory().buildQueryBuilder().forEntity( NotConfiguredIndexed.class ).get() )
				.hasMessageContainingAll(
						"No matching indexed entity types for types: [" + NotConfiguredIndexed.class.getName() + "]",
						"These types are not indexed entity types, nor is any of their subtypes"
				);
	}

	@Test
	void forEntity_notConfigured_notIndexed() {
		assertThatThrownBy(
				() -> sfHolder.getSearchFactory().buildQueryBuilder().forEntity( NotConfiguredNotIndexed.class ).get() )
				.hasMessageContainingAll(
						"No matching indexed entity types for types: [" + NotConfiguredNotIndexed.class.getName() + "]",
						"These types are not indexed entity types, nor is any of their subtypes",
						NotConfiguredNotIndexed.class.getSimpleName()
				);
	}

	private static class ConfiguredNotIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;

		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "embedded")))
		private ConfiguredIndexed containing;
	}

	@Indexed
	private static class ConfiguredIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;

		@IndexedEmbedded
		private ConfiguredNotIndexed embedded;
	}

	private static class NotConfiguredNotIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;
	}

	@Indexed
	private static class NotConfiguredIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;
	}
}
