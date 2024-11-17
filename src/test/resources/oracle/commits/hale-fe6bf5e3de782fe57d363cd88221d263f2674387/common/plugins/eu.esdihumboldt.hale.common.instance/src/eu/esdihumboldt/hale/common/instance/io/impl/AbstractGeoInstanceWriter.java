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
import eu.esdihumboldt.hale.common.schema.geometry.CRSDefinition;
import eu.esdihumboldt.hale.common.schema.geometry.GeometryProperty;
import eu.esdihumboldt.util.Pair;

/**
 * Abstract {@link GeoInstanceWriter} base implementation
 * 
 * @author Simon Templer
 * @since 2.9
 */
public abstract class AbstractGeoInstanceWriter extends AbstractInstanceWriter implements
		GeoInstanceWriter {

	@Override
	public void setTargetCRS(CRSDefinition crs) {
		setParameter(PARAM_TARGET_CRS, Value.of(CRSDefinitionManager.getInstance().asString(crs)));
	}

	@Override
	public CRSDefinition getTargetCRS() {
		return CRSDefinitionManager.getInstance().parse(
				getParameter(PARAM_TARGET_CRS).as(String.class));
	}

	@Override
	public void setCustomEPSGPrefix(String epsgPrefix) {
		setParameter(PARAM_CRS_CODE_FORMAT, Value.of(epsgPrefix));
	}

	@Override
	public String getCustomEPSGPrefix() {
		return getParameter(PARAM_CRS_CODE_FORMAT).as(String.class);
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
				MathTransform transform = CRS.findMathTransform(sourceCrs.getCRS(), getTargetCRS()
						.getCRS());
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
