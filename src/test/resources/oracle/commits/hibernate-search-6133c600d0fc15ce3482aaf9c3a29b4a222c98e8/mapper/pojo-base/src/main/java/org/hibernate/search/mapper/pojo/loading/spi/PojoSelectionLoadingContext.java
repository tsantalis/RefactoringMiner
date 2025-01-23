/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

/**
 * Context passed {@link PojoMassLoadingStrategy}.
 * <p>
 * Mappers will generally need to cast this type to the mapper-specific subtype.
 */
public interface PojoSelectionLoadingContext {

	/**
	 * Check whether this context is still open, throwing an exception if it is not.
	 */
	void checkOpen();

	PojoRuntimeIntrospector runtimeIntrospector();

}
