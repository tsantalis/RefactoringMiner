/*
 * Copyright (c) 2014 Data Harmonisation Panel
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
 *     HUMBOLDT EU Integrated Project #030962
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package eu.esdihumboldt.hale.common.instance.io;

import javax.annotation.Nullable;

import eu.esdihumboldt.hale.common.instance.io.util.EnumWindingOrderTypes;
import eu.esdihumboldt.hale.common.schema.geometry.CRSDefinition;

/**
 * Provides support for writing instances
 * 
 * @author Simon Templer
 * @modifiedby Arun Varma (04.07.2016)
 * @since 2.9
 */
public interface GeoInstanceWriter extends InstanceWriter {

	/**
	 * Name of the parameter specifying a target CRS. Support of this parameter
	 * is optional.
	 */
	public static final String PARAM_TARGET_CRS = "crs";

	/**
	 * Name of the parameter specifying a prefix for EPSG codes. Support of this
	 * parameter is optional.
	 */
	public static final String PARAM_CRS_CODE_FORMAT = "crs.epsg.prefix";

	/**
	 * Name of the parameter specifying a Winding order of Geometry.
	 */
	public static final String PARAM_UNIFY_WINDING_ORDER = "counterclockwise";

	/**
	 * Set the target CRS for written instances. Note that supporting the target
	 * CRS conversion is optional for implementations.
	 * 
	 * @param crs the CRS definition
	 */
	public void setTargetCRS(@Nullable CRSDefinition crs);

	/**
	 * Get the target CRS to convert instance geometries to. Note that
	 * supporting the target CRS conversion is optional for implementations.
	 * 
	 * @return the target CRS definition or <code>null</code>
	 */
	@Nullable
	public CRSDefinition getTargetCRS();

	/**
	 * Set a custom prefix to be used to encode target EPSG CRS codes.
	 * 
	 * @param epsgPrefix the custom EPSG code prefix or <code>null</code> to
	 *            leave the CRS code untouched
	 */
	public void setCustomEPSGPrefix(@Nullable String epsgPrefix);

	/**
	 * Get the custom prefix to be used to encode target EPSG CRS codes.
	 * 
	 * @return the custom EPSG code prefix or <code>null</code>
	 */
	@Nullable
	public String getCustomEPSGPrefix();

	/**
	 * Set winding order of geometry.
	 * 
	 * @param windingOrderType Winding order of
	 */
	public void setWindingOrder(@Nullable EnumWindingOrderTypes windingOrderType);

	/**
	 * Get winding order of geometry.
	 * 
	 * @return the winding order of geometry
	 */
	@Nullable
	public EnumWindingOrderTypes getWindingOrder();

}
