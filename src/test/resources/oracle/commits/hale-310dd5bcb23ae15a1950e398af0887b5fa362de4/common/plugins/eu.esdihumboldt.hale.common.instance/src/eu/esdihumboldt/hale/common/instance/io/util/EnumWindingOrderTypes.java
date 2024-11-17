/*
 * Copyright (c) 2016 Data Harmonisation Panel
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

/**
 * Enumeration for different types of winding orders for Geometry. Selected type
 * will be applied geometry. In case of Polygon/MultiPolygon selected type will
 * be applied to the shell and the reversed type is applied to the holes.
 * 
 * @author Arun
 */
public enum EnumWindingOrderTypes {

	/**
	 * Unified geometry by Clockwise Winding order to the shell and Counter
	 * Clockwise to the holes.
	 */
	clockwise, /**
				 * Unified geometry by Counter Clockwise Winding order to the
				 * shell and Clockwise to the holes.
				 */
	counterClockwise, /**
						 * no change in Geometry. Leave it as it is.
						 */
	noChanges;
}
