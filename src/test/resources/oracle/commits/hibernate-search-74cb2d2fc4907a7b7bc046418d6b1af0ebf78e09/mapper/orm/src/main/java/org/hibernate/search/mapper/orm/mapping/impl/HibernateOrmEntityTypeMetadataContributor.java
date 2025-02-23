/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Optional;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmPathDefinitionProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

final class HibernateOrmEntityTypeMetadataContributor implements PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;
	private final PersistentClass persistentClass;
	private final Optional<String> identifierPropertyNameOptional;

	HibernateOrmEntityTypeMetadataContributor(PojoRawTypeModel<?> typeModel,
			PersistentClass persistentClass, Optional<String> identifierPropertyNameOptional) {
		this.typeModel = typeModel;
		this.persistentClass = persistentClass;
		this.identifierPropertyNameOptional = identifierPropertyNameOptional;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeModel.typeIdentifier().equals( collector.typeIdentifier() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		var node = collector.markAsEntity();
		node.entityName( persistentClass.getJpaEntityName() );
		node.pathDefinitionProvider( new HibernateOrmPathDefinitionProvider( typeModel, persistentClass ) );
		node.entityIdPropertyName( identifierPropertyNameOptional.orElse( null ) );
	}
}
