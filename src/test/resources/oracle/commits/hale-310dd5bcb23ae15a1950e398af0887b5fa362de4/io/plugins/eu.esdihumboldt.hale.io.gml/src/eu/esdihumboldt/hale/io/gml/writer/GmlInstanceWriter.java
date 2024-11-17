/*
 * Copyright (c) 2012 Data Harmonisation Panel
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

package eu.esdihumboldt.hale.io.gml.writer;

import eu.esdihumboldt.hale.common.instance.io.util.EnumWindingOrderTypes;
import eu.esdihumboldt.hale.io.gml.writer.internal.StreamGmlWriter;

/**
 * Writes instances to a GML FeatureCollection
 * 
 * @author Simon Templer
 */
public class GmlInstanceWriter extends StreamGmlWriter {

	/**
	 * The identifier of the writer as registered to the I/O provider extension.
	 */
	public static final String ID = "eu.esdihumboldt.hale.io.gml.writer";

	/**
	 * Default constructor
	 */
	public GmlInstanceWriter() {
		super(true);
	}

	/**
	 * @see StreamGmlWriter#requiresDefaultContainer()
	 */
	@Override
	protected boolean requiresDefaultContainer() {
		return true; // requires a FeatureCollection being present
	}

	/**
	 * @see eu.esdihumboldt.hale.common.instance.io.impl.AbstractGeoInstanceWriter#getDefaultWindingOrder()
	 */
	@Override
	protected EnumWindingOrderTypes getDefaultWindingOrder() {
		return EnumWindingOrderTypes.counterClockwise;
	}

}
