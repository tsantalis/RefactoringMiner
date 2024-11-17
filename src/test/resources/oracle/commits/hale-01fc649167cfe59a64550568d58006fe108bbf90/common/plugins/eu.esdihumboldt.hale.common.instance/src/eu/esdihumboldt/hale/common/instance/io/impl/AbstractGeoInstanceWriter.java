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

package eu.esdihumboldt.hale.common.instance.io.impl;

import java.util.Collection;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;

import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.instance.geometry.CRSDefinitionManager;
import eu.esdihumboldt.hale.common.instance.geometry.CRSDefinitionUtil;
import eu.esdihumboldt.hale.common.instance.geometry.impl.CodeDefinition;
import eu.esdihumboldt.hale.common.instance.io.GeoInstanceWriter;
import eu.esdihumboldt.hale.common.instance.io.util.EnumWindingOrderTypes;
import eu.esdihumboldt.hale.common.schema.geometry.CRSDefinition;
import eu.esdihumboldt.hale.common.schema.geometry.GeometryProperty;
import eu.esdihumboldt.util.Pair;
import eu.esdihumboldt.util.geometry.WindingOrder;

/**
 * Abstract {@link GeoInstanceWriter} base implementation
 * 
 * @author Simon Templer
 * @since 2.9
 */
public abstract class AbstractGeoInstanceWriter extends AbstractInstanceWriter
		implements GeoInstanceWriter {

	@Override
	public void setTargetCRS(CRSDefinition crs) {
		setParameter(PARAM_TARGET_CRS, Value.of(CRSDefinitionManager.getInstance().asString(crs)));
	}

	@Override
	public CRSDefinition getTargetCRS() {
		return CRSDefinitionManager.getInstance()
				.parse(getParameter(PARAM_TARGET_CRS).as(String.class));
	}

	@Override
	public void setCustomEPSGPrefix(String epsgPrefix) {
		setParameter(PARAM_CRS_CODE_FORMAT, Value.of(epsgPrefix));
	}

	@Override
	public String getCustomEPSGPrefix() {
		return getParameter(PARAM_CRS_CODE_FORMAT).as(String.class);
	}

	@Override
	public void setWindingOrder(EnumWindingOrderTypes windingOrderType) {
		setParameter(PARAM_UNIFY_WINDING_ORDER, Value.of(windingOrderType.toString()));
	}

	@Override
	public EnumWindingOrderTypes getWindingOrder() {
		EnumWindingOrderTypes value = getParameter(PARAM_UNIFY_WINDING_ORDER)
				.as(EnumWindingOrderTypes.class);
		if (value == null)
			return getDefaultWindingOrder();
		else
			return value;
	}

	/**
	 * Get default Winding Order. Function is to give functionality to the
	 * subType to change the default Winding order.
	 * 
	 * @return EnumWindingOrderTypes default Winding order
	 */
	protected EnumWindingOrderTypes getDefaultWindingOrder() {
		return EnumWindingOrderTypes.noChanges;
	}

	/**
	 * Convert the given geometry to the target CRS, if possible (and a target
	 * CRS is set).
	 * 
	 * @param geom the geometry to convert
	 * @param sourceCrs the source CRS
	 * @param report the reporter
	 * @return a pair of geometry and CRS definition, either the converted
	 *         geometry and the target CRS or the given geometry and the source
	 *         CRS
	 */
	protected Pair<Geometry, CRSDefinition> convertGeometry(Geometry geom, CRSDefinition sourceCrs,
			IOReporter report) {
		if (sourceCrs != null && sourceCrs.getCRS() != null && getTargetCRS() != null
				&& getTargetCRS().getCRS() != null) {
			try {
				// TODO cache mathtransforms?
				MathTransform transform = CRS.findMathTransform(sourceCrs.getCRS(),
						getTargetCRS().getCRS());
				Geometry targetGeometry = JTS.transform(geom, transform);
				return new Pair<>(targetGeometry, getTargetCRS());
			} catch (Exception e) {
				if (report != null) {
					report.error(new IOMessageImpl("Could not convert geometry to target CRS", e));
				}
				// return original geometry
				return new Pair<>(geom, sourceCrs);
			}
		}
		else {
			// return original geometry
			return new Pair<>(geom, sourceCrs);
		}
	}

	/**
	 * Returns a pair of geometry and associated CRS definition for the given
	 * value. The value has to be a Geometry or a GeometryProperty, otherwise
	 * <code>null</code> is returned.
	 * 
	 * @param value the value to extract the information from
	 * @param allowConvert if conversion to the target CRS should be performed
	 *            if applicable
	 * @param report the reporter
	 * @return a pair of geometry and CRS definition (latter may be
	 *         <code>null</code>), or <code>null</code> if the argument doesn't
	 *         contain a geometry
	 */
	protected Pair<Geometry, CRSDefinition> extractGeometry(Object value, boolean allowConvert,
			IOReporter report) {
		Pair<Geometry, CRSDefinition> pair = getGeometryPair(value, allowConvert, report);
		return unifyGeometryPair(pair, report);
	}

	/**
	 * Returns a pair of geometry and associated CRS definition for the given
	 * value. The value has to be a Geometry or a GeometryProperty, otherwise
	 * <code>null</code> is returned.
	 * 
	 * @param value the value to extract the information from
	 * @param allowConvert if conversion to the target CRS should be performed
	 *            if applicable
	 * @param report the reporter
	 * @return a pair of geometry and CRS definition (latter may be
	 *         <code>null</code>), or <code>null</code> if the argument doesn't
	 *         contain a geometry
	 */
	private Pair<Geometry, CRSDefinition> getGeometryPair(Object value, boolean allowConvert,
			IOReporter report) {
		// TODO collection handling (-> happens for example with target
		// CompositeSurface)
		if (value instanceof Collection) {
			if (!((Collection<?>) value).isEmpty()) {
				// TODO combine geometries?
				value = ((Collection<?>) value).iterator().next();
			}
		}
		if (value instanceof Geometry) {
			return new Pair<>((Geometry) value, null);
		}
		else if (value instanceof GeometryProperty<?>) {
			CRSDefinition def = ((GeometryProperty<?>) value).getCRSDefinition();
			Geometry geom = ((GeometryProperty<?>) value).getGeometry();
			if (allowConvert) {
				return convertGeometry(geom, def, report);
			}
			return new Pair<>(geom, def);
		}
		else
			return null;
	}

	/**
	 * Returns a pair of unified geometry of given geometry and associated CRS
	 * definition based on Winding order supplied.
	 * 
	 * @param pair A pair of Geometry and CRSDefinition, on which winding
	 *            process will get done.
	 * @param report the reporter
	 * @return Unified Pair .
	 */
	protected Pair<Geometry, CRSDefinition> unifyGeometryPair(Pair<Geometry, CRSDefinition> pair,
			IOReporter report) {

		// get Geometry object
		Geometry geom = pair.getFirst();
		if (geom == null) {
			return pair;
		}
		// unify geometry
		geom = unifyGeometry(geom, report);
		return new Pair<>(geom, pair.getSecond());
	}

	/**
	 * Returns a unified geometry of given geometry based on Winding order
	 * supplied.
	 * 
	 * @param geom The Geometry object, on which winding process will get done.
	 * @param report the reporter
	 * @return Unified geometry .
	 */
	protected Geometry unifyGeometry(Geometry geom, IOReporter report) {
		if (geom == null) {
			return geom;
		}
		// getting winding order
		EnumWindingOrderTypes windingOrder = getWindingOrder();

		if (windingOrder == null || windingOrder == EnumWindingOrderTypes.noChanges) {
			return geom;
		}
		else {
			Geometry unifiedGeometry;
			// unify geometry using WindingOrder utility.

			switch (windingOrder) {

			case counterClockwise:
				unifiedGeometry = WindingOrder.unifyWindingOrder(geom, true);
				break;
			case clockwise:
				unifiedGeometry = WindingOrder.unifyWindingOrder(geom, false);
				break;
			default:
				if (report != null) {
					report.error(new IOMessageImpl(
							"Parameter encountered as winding order is not known: "
									+ windingOrder.toString(),
							null));
				}
				unifiedGeometry = geom;
				break;
			}
			return unifiedGeometry;
		}
	}

	/**
	 * Extract a CRS code from the given CRS definition.
	 * 
	 * @param crsDef the CRS definition
	 * @return the CRS code, may be <code>null</code>
	 */
	protected String extractCode(CRSDefinition crsDef) {
		if (crsDef == null) {
			return null;
		}
		String orgCode = CRSDefinitionUtil.getCode(crsDef);
		String customPrefix = getCustomEPSGPrefix();
		if (orgCode != null && customPrefix != null) {
			// try to extract EPSG code
			String epsgCode = CodeDefinition.extractEPSGCode(orgCode);
			if (epsgCode != null) {
				return customPrefix + epsgCode;
			}
		}
		return orgCode;
	}

}
