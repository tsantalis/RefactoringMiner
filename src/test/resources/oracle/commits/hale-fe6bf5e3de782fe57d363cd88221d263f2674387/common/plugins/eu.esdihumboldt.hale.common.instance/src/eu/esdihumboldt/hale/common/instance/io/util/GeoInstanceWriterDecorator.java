/*
 * Copyright (c) 2015 Data Harmonisation Panel
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package eu.esdihumboldt.hale.common.instance.io.util;

import eu.esdihumboldt.hale.common.core.io.util.ExportProviderDecorator;
import eu.esdihumboldt.hale.common.instance.io.GeoInstanceWriter;
import eu.esdihumboldt.hale.common.schema.geometry.CRSDefinition;

/**
 * Decorator for {@link GeoInstanceWriter}s.
 * 
 * @param <T> the provider type
 * @author Simon Templer
 */
public class GeoInstanceWriterDecorator<T extends GeoInstanceWriter> extends
		InstanceWriterDecorator<T> implements GeoInstanceWriter {

	/**
	 * @see ExportProviderDecorator#ExportProviderDecorator(eu.esdihumboldt.hale.common.core.io.ExportProvider)
	 */
	public GeoInstanceWriterDecorator(T internalProvider) {
		super(internalProvider);
	}

	@Override
	public void setTargetCRS(CRSDefinition crs) {
		internalProvider.setTargetCRS(crs);
	}

	@Override
	public CRSDefinition getTargetCRS() {
		return internalProvider.getTargetCRS();
	}

	@Override
	public void setCustomEPSGPrefix(String epsgPrefix) {
		internalProvider.setCustomEPSGPrefix(epsgPrefix);
	}

	@Override
	public String getCustomEPSGPrefix() {
		return internalProvider.getCustomEPSGPrefix();
	}

}
