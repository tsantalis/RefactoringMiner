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

package eu.esdihumboldt.hale.io.gml.writer.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.Locatable;
import eu.esdihumboldt.hale.common.instance.geometry.GeometryUtil;
import eu.esdihumboldt.hale.common.instance.io.GeoInstanceWriter;
import eu.esdihumboldt.hale.common.instance.io.InstanceReader;
import eu.esdihumboldt.hale.common.instance.io.InstanceWriter;
import eu.esdihumboldt.hale.common.instance.io.util.EnumWindingOrderTypes;
import eu.esdihumboldt.hale.common.instance.model.Instance;
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection;
import eu.esdihumboldt.hale.common.instance.model.MutableGroup;
import eu.esdihumboldt.hale.common.instance.model.MutableInstance;
import eu.esdihumboldt.hale.common.instance.model.ResourceIterator;
import eu.esdihumboldt.hale.common.instance.model.impl.DefaultGroup;
import eu.esdihumboldt.hale.common.instance.model.impl.DefaultInstance;
import eu.esdihumboldt.hale.common.instance.model.impl.DefaultInstanceCollection;
import eu.esdihumboldt.hale.common.schema.geometry.GeometryProperty;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.DefinitionGroup;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace;
import eu.esdihumboldt.hale.common.test.TestUtil;
import eu.esdihumboldt.hale.io.gml.reader.internal.GmlInstanceReader;
import eu.esdihumboldt.hale.io.gml.writer.GmlInstanceWriter;
import eu.esdihumboldt.hale.io.gml.writer.internal.geometry.GeometryConverterRegistry;
import eu.esdihumboldt.hale.io.gml.writer.internal.geometry.GeometryConverterRegistry.ConversionLadder;
import eu.esdihumboldt.hale.io.xml.validator.XmlInstanceValidator;
import eu.esdihumboldt.hale.io.xsd.model.XmlElement;
import eu.esdihumboldt.hale.io.xsd.model.XmlIndex;
import eu.esdihumboldt.hale.io.xsd.reader.XmlSchemaReader;
import eu.esdihumboldt.util.geometry.WindingOrder;

/**
 * Tests for {@link StreamGmlWriter}.
 * 
 * @author Simon Templer
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 */
@SuppressWarnings("restriction")
public class StreamGmlWriterTest {

	/**
	 * Property path for the geometry property
	 */
	private static final List<QName> GEOMETRY_PROPERTY = Arrays
			.asList(new QName("eu:esdihumboldt:hale:test", "geometry"));

	/**
	 * If temporary files shall be deleted
	 */
	private static final boolean DEL_TEMP_FILES = true;

	private static final String DEF_SRS_NAME = "EPSG:31467"; //$NON-NLS-1$

	/**
	 * The geometry factory
	 */
	private final GeometryFactory geomFactory = new GeometryFactory();

//	/**
//	 * Test writing a simple feature from a simple schema (Watercourses VA)
//	 * 
//	 * @throws Exception if any error occurs 
//	 */
//	@Test
//	public void testFillWrite_WatercourseVA() throws Exception {
//		Map<List<String>, Object> values = new HashMap<List<String>, Object>();
//		
//		// create the geometry
////		MultiLineString mls = geomFactory.createMultiLineString(
////				new LineString[]{createLineString(0.0), createLineString(1.0),
////						createLineString(2.0)});
//		//XXX for some reason the MultiLineString is converted to a LineString when set a value -> so we are using a LineString instead to allow value comparison
//		LineString mls = createLineString(0.0);
//		
//		values.put(Arrays.asList("LENGTH"), Double.valueOf(10.2)); //$NON-NLS-1$
//		values.put(Arrays.asList("NAME"), "Test"); //$NON-NLS-1$ //$NON-NLS-2$
//		values.put(Arrays.asList("the_geom"), mls); //$NON-NLS-1$
//		
//		Report report = fillFeatureTest("Watercourses_VA", //$NON-NLS-1$
//				getClass().getResource("/data/sample_wva/wfs_va.xsd").toURI(),  //$NON-NLS-1$
//				values, "fillWrite_WVA", "EPSG:31251"); //$NON-NLS-1$ //$NON-NLS-2$
//		
//		assertTrue("Expected GML output to be valid", report.isValid()); //$NON-NLS-1$
//	}

	/**
	 * Test writing a {@link Point} to a GML 2 geometry type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Ignore
	// GML 2 stuff currently not working (because of unrelated schemas)
	@Test
	public void testGeometry_2_Point() throws Exception {
		// create the geometry
		Point point = createPoint(10.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, point); // $NON-NLS-1$

		IOReport report = fillFeatureTest("Test", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml2.xsd").toURI(), //$NON-NLS-1$
				values, "geometry_2_Point", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link Point} to a GML 3.2 geometry primitive type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_Point() throws Exception {
		// create the geometry
		Point point = createPoint(10.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, point); // $NON-NLS-1$

		IOReport report = fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_Point", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link Point} to a GML 3.2 geometry aggregate type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryAggregate_32_Point() throws Exception {
		// create the geometry
		Point point = createPoint(10.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, point); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryAggregate_32_Point", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiPoint} to a GML 2 geometry type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Ignore
	// GML 2 stuff currently not working (because of unrelated schemas)
	@Test
	public void testGeometry_2_MultiPoint() throws Exception {
		// create the geometry
		MultiPoint mp = geomFactory.createMultiPoint(
				new Point[] { createPoint(0.0), createPoint(1.0), createPoint(2.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("Test", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml2.xsd").toURI(), //$NON-NLS-1$
				values, "geometry_2_MultiPoint", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiPoint} to a GML 3.2 geometry aggregate type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryAggregate_32_MultiPoint() throws Exception {
		// create the geometry
		MultiPoint mp = geomFactory.createMultiPoint(
				new Point[] { createPoint(0.0), createPoint(1.0), createPoint(2.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryAggregate_32_MultiPoint", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	// TODO test with a type that references a gmlAbstractGeometry element

	/**
	 * Create a point
	 * 
	 * @param x the x ordinate
	 * 
	 * @return a point
	 */
	private Point createPoint(double x) {
		return geomFactory.createPoint(new Coordinate(x, x + 1));
	}

	/**
	 * Test writing a {@link Polygon} to a GML 2 geometry type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Ignore
	// GML 2 stuff currently not working (because of unrelated schemas)
	@Test
	public void testGeometry_2_Polygon() throws Exception {
		// create the geometry
		Polygon polygon = createPolygon(0.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, polygon); // $NON-NLS-1$

		IOReport report = fillFeatureTest("Test", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml2.xsd").toURI(), //$NON-NLS-1$
				values, "geometry_2_Polygon", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link Polygon} to a GML 3.0 geometry primitive type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Ignore
	// deactivated test because schema is invalid (according to xerces)
	@Test
	public void testGeometryPrimitive_3_Polygon() throws Exception {
		// create the geometry
		Polygon polygon = createPolygon(0.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, polygon);

		IOReport report = fillFeatureTest("PrimitiveTest",
				getClass().getResource("/data/geom_schema/geom-gml3.xsd").toURI(), values,
				"geometryPrimitive_3_Polygon", DEF_SRS_NAME);

		assertTrue("Expected GML output to be valid", report.isSuccess());
	}

	/**
	 * Test writing a {@link Polygon} to a GML 3.1 geometry primitive type
	 * 
	 * @throws Exception if an error occurs
	 */
//	@Ignore // loading Polygons not yet supported
	@Test
	public void testGeometryPrimitive_31_Polygon() throws Exception {
		// create the geometry
		Polygon polygon = createPolygon(0.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, polygon); // $NON-NLS-1$

		IOReport report = fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml31.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_31_Polygon", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link Polygon} to a GML 3.2 geometry primitive type
	 * 
	 * @throws Exception if an error occurs
	 */
//	@Ignore // loading Polygons not yet supported
	@Test
	public void testGeometryPrimitive_32_Polygon() throws Exception {
		// create the geometry
		Polygon polygon = createPolygon(0.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, polygon); // $NON-NLS-1$

		IOReport report = fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_Polygon", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Create a polygon
	 * 
	 * @param offset the offset along both axes
	 * 
	 * @return the created polygon
	 */
	private Polygon createPolygon(double offset) {
		LinearRing shell = geomFactory.createLinearRing(new Coordinate[] {
				new Coordinate(-1.0 + offset, offset), new Coordinate(offset, 1.0 + offset),
				new Coordinate(1.0 + offset, offset), new Coordinate(offset, -1.0 + offset),
				new Coordinate(-1.0 + offset, offset) });
		return geomFactory.createPolygon(shell, null);
	}

	/**
	 * Test writing a {@link LineString} to a GML 2 geometry type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Ignore
	// GML 2 stuff currently not working (because of unrelated schemas)
	@Test
	public void testGeometry_2_LineString() throws Exception {
		// create the geometry
		LineString lineString = createLineString(0.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, lineString); // $NON-NLS-1$

		IOReport report = fillFeatureTest("Test", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml2.xsd").toURI(), //$NON-NLS-1$
				values, "geometry_2_LineString", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link LineString} to a GML 3.2 geometry primitive type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_LineString() throws Exception {
		// create the geometry
		LineString lineString = createLineString(0.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, lineString); // $NON-NLS-1$

		IOReport report = fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_LineString", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link LineString} to a GML 3.2 geometry aggregate type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryAggregate_32_LineString() throws Exception {
		// create the geometry
		LineString lineString = createLineString(0.0);

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, lineString); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryAggregate_32_LineString", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Create a line string geometry
	 * 
	 * @param offset the line y-offset
	 * 
	 * @return the line string
	 */
	private LineString createLineString(double offset) {
		return geomFactory.createLineString(new Coordinate[] { new Coordinate(0.0, offset),
				new Coordinate(1.0, 1.0 + offset), new Coordinate(2.0, 2.0 + offset),
				new Coordinate(3.0, 1.0 + offset), new Coordinate(4.0, offset) });
	}

	/**
	 * Create a curve.
	 * 
	 * @return the line string
	 */
	private MultiLineString createCurve() {
		LineString ls1 = geomFactory.createLineString(new Coordinate[] { new Coordinate(0.0, 0.0),
				new Coordinate(1.0, 1.0), new Coordinate(2.0, 2.0), new Coordinate(3.0, 3.0),
				new Coordinate(4.0, 4.0) });
		LineString ls2 = geomFactory.createLineString(new Coordinate[] { new Coordinate(4.0, 4.0),
				new Coordinate(5.0, 3.0), new Coordinate(6.0, 2.0), new Coordinate(7.0, 1.0),
				new Coordinate(8.0, 0.0) });

		return geomFactory.createMultiLineString(new LineString[] { ls1, ls2 });
	}

	/**
	 * Test writing a {@link MultiLineString} to a GML 2 geometry type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Ignore
	// GML 2 stuff currently not working (because of unrelated schemas)
	@Test
	public void testGeometry_2_MultiLineString() throws Exception {
		// create the geometry
		MultiLineString mls = geomFactory.createMultiLineString(new LineString[] {
				createLineString(0.0), createLineString(1.0), createLineString(2.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mls); // $NON-NLS-1$

		IOReport report = fillFeatureTest("Test", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml2.xsd").toURI(), //$NON-NLS-1$
				values, "geometry_2_MultiLineString", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiLineString} to a GML 3.2 geometry primitive
	 * type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_MultiLineString() throws Exception {
		// create the geometry
		MultiLineString mls = geomFactory.createMultiLineString(new LineString[] {
				createLineString(0.0), createLineString(1.0), createLineString(2.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mls); // $NON-NLS-1$

		fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_MultiLineString", DEF_SRS_NAME, //$NON-NLS-1$
				true, true);

		// writing should have failed because the MLS is not a curve
	}

	/**
	 * Test writing a {@link MultiLineString} to a GML 3.2 geometry primitive
	 * type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_MultiLineString_Curve1() throws Exception {
		// create the geometry
		MultiLineString mls = geomFactory
				.createMultiLineString(new LineString[] { createLineString(0.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mls); // $NON-NLS-1$

		IOReport report = fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_MultiLineString", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiLineString} to a GML 3.2 geometry primitive
	 * type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_MultiLineString_Curve2() throws Exception {
		// create the geometry
		MultiLineString mls = createCurve();

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mls); // $NON-NLS-1$

		IOReport report = fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_MultiLineString", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiLineString} to a GML 3.2 geometry aggregate
	 * type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryAggregate_32_MultiLineString() throws Exception {
		// create the geometry
		MultiLineString mls = geomFactory.createMultiLineString(new LineString[] {
				createLineString(0.0), createLineString(1.0), createLineString(2.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mls); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryAggregate_32_MultiLineString", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiPolygon} to a GML 2 geometry type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Ignore
	// GML 2 stuff currently not working (because of unrelated schemas)
	@Test
	public void testGeometry_2_MultiPolygon() throws Exception {
		// create the geometry
		MultiPolygon mp = geomFactory.createMultiPolygon(
				new Polygon[] { createPolygon(0.0), createPolygon(1.0), createPolygon(-1.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("Test", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml2.xsd").toURI(), //$NON-NLS-1$
				values, "geometry_2_MultiPolygon", DEF_SRS_NAME); //$NON-NLS-1$

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiPolygon} to a GML 3.2 geometry primitive type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_MultiPolygon() throws Exception {
		// create the geometry
		MultiPolygon mp = geomFactory.createMultiPolygon(
				new Polygon[] { createPolygon(0.0), createPolygon(1.0), createPolygon(-1.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("PrimitiveTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_MultiPolygon", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiPolygon} to a GML 3.2 geometry aggregate type
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryAggregate_32_MultiPolygon() throws Exception {
		// create the geometry
		MultiPolygon mp = geomFactory.createMultiPolygon(
				new Polygon[] { createPolygon(0.0), createPolygon(1.0), createPolygon(-1.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryAggregate_32_MultiPolygon", DEF_SRS_NAME, //$NON-NLS-1$
				true, false);

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiPolygon} to a GML 3.2 geometry primitive type
	 * with Winding Order in CounterClockWise
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_MultiPolygon_WindingOrder_CCW() throws Exception {
		// create the geometry
		MultiPolygon mp = geomFactory.createMultiPolygon(
				new Polygon[] { createPolygon(0.0), createPolygon(1.0), createPolygon(-1.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryAggregate_32_MultiPolygon", DEF_SRS_NAME, //$NON-NLS-1$
				false, false, "COUNTERCLOCKWISE");

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$
	}

	/**
	 * Test writing a {@link MultiPolygon} to a GML 3.2 geometry primitive type
	 * with Winding Order in ClockWise
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_MultiPolygon_WindingOrder_CW() throws Exception {
		// create the geometry
		MultiPolygon mp = geomFactory.createMultiPolygon(
				new Polygon[] { createPolygon(0.0), createPolygon(1.0), createPolygon(-1.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_MultiPolygon", DEF_SRS_NAME, //$NON-NLS-1$
				false, false, "CLOCKWISE");

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$

	}

	/**
	 * Test writing a {@link MultiPolygon} to a GML 3.2 geometry primitive type
	 * with Winding Order in ClockWise
	 * 
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testGeometryPrimitive_32_MultiPolygon_WindingOrder() throws Exception {
		// create the geometry
		MultiPolygon mp = geomFactory.createMultiPolygon(
				new Polygon[] { createPolygon(0.0), createPolygon(1.0), createPolygon(-1.0) });

		Map<List<QName>, Object> values = new HashMap<List<QName>, Object>();
		values.put(GEOMETRY_PROPERTY, mp); // $NON-NLS-1$

		IOReport report = fillFeatureTest("AggregateTest", //$NON-NLS-1$
				getClass().getResource("/data/geom_schema/geom-gml32.xsd").toURI(), //$NON-NLS-1$
				values, "geometryPrimitive_32_MultiPolygon", DEF_SRS_NAME, //$NON-NLS-1$
				false, false, "DONTCHANGE");

		assertTrue("Expected GML output to be valid", report.isSuccess()); //$NON-NLS-1$

	}

	/**
	 * Create a feature, fill it with values, write it as GML, validate the GML
	 * and load the GML file again to compare the loaded values with the ones
	 * that were written
	 * 
	 * @param elementName the element name of the feature type to use, if
	 *            <code>null</code> a random element will be used
	 * @param targetSchema the schema to use, the first element will be used for
	 *            the type of the feature
	 * @param values the values to set on the feature
	 * @param testName the name of the test
	 * @param srsName the SRS name
	 * @return the validation report
	 * @throws Exception if any error occurs
	 */
	private IOReport fillFeatureTest(String elementName, URI targetSchema,
			Map<List<QName>, Object> values, String testName, String srsName) throws Exception {
		return fillFeatureTest(elementName, targetSchema, values, testName, srsName, false, false);
	}

	/**
	 * Create a feature, fill it with values, write it as GML, validate the GML
	 * and load the GML file again to compare the loaded values with the ones
	 * that were written
	 * 
	 * @param elementName the element name of the feature type to use, if
	 *            <code>null</code> a random element will be used
	 * @param targetSchema the schema to use, the first element will be used for
	 *            the type of the feature
	 * @param values the values to set on the feature
	 * @param testName the name of the test
	 * @param srsName the SRS name
	 * @param skipValueTest if the check for equality shall be skipped
	 * @param expectWriteFail if the GML writing is expected to fail
	 * @return the validation report or the GML writing report if writing
	 *         expected to fail
	 * @throws Exception if any error occurs
	 */
	private IOReport fillFeatureTest(String elementName, URI targetSchema,
			Map<List<QName>, Object> values, String testName, String srsName, boolean skipValueTest,
			boolean expectWriteFail) throws Exception {
		return fillFeatureTest(elementName, targetSchema, values, testName, srsName, skipValueTest,
				expectWriteFail, null);
	}

	/**
	 * Create a feature, fill it with values, write it as GML, validate the GML
	 * and load the GML file again to compare the loaded values with the ones
	 * that were written
	 * 
	 * @param elementName the element name of the feature type to use, if
	 *            <code>null</code> a random element will be used
	 * @param targetSchema the schema to use, the first element will be used for
	 *            the type of the feature
	 * @param values the values to set on the feature
	 * @param testName the name of the test
	 * @param srsName the SRS name
	 * @param skipValueTest if the check for equality shall be skipped
	 * @param expectWriteFail if the GML writing is expected to fail
	 * @param windingOrderParam winding order parameter or <code>null</code>
	 * @return the validation report or the GML writing report if writing
	 *         expected to fail
	 * @throws Exception if any error occurs
	 */
	private IOReport fillFeatureTest(String elementName, URI targetSchema,
			Map<List<QName>, Object> values, String testName, String srsName, boolean skipValueTest,
			boolean expectWriteFail, String windingOrderParam) throws Exception {
		// load the sample schema
		XmlSchemaReader reader = new XmlSchemaReader();
		reader.setSharedTypes(null);
		reader.setSource(new DefaultInputSupplier(targetSchema));
		IOReport schemaReport = reader.execute(null);
		assertTrue(schemaReport.isSuccess());
		XmlIndex schema = reader.getSchema();

		XmlElement element = null;
		if (elementName == null) {
			element = schema.getElements().values().iterator().next();
			if (element == null) {
				fail("No element found in the schema"); //$NON-NLS-1$
			}
		}
		else {
			for (XmlElement candidate : schema.getElements().values()) {
				if (candidate.getName().getLocalPart().equals(elementName)) {
					element = candidate;
					break;
				}
			}
			if (element == null) {
				fail("Element " + elementName + " not found in the schema"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (element == null) {
			throw new IllegalStateException();
		}

		// create feature
		MutableInstance feature = new DefaultInstance(element.getType(), null);

		// set some values
		for (Entry<List<QName>, Object> entry : values.entrySet()) {
			MutableGroup parent = feature;
			List<QName> properties = entry.getKey();
			for (int i = 0; i < properties.size() - 1; i++) {
				QName propertyName = properties.get(i);
				DefinitionGroup def = parent.getDefinition();

				Object[] vals = parent.getProperty(propertyName);
				if (vals != null && vals.length > 0) {
					Object value = vals[0];
					if (value instanceof MutableGroup) {
						parent = (MutableGroup) value;
					}
					else {
						MutableGroup child;
						ChildDefinition<?> childDef = def.getChild(propertyName);
						if (childDef.asProperty() != null || value != null) {
							// create instance
							child = new DefaultInstance(childDef.asProperty().getPropertyType(),
									null);
						}
						else {
							// create group
							child = new DefaultGroup(childDef.asGroup());
						}

						if (value != null) {
							// wrap value
							((MutableInstance) child).setValue(value);
						}

						parent = child;
					}
				}
			}
			parent.addProperty(properties.get(properties.size() - 1), entry.getValue());
		}

		InstanceCollection instances = new DefaultInstanceCollection(
				Collections.singleton(feature));

		// write to file
		InstanceWriter writer = new GmlInstanceWriter();
		if (windingOrderParam != null) {
			writer.setParameter(GeoInstanceWriter.PARAM_UNIFY_WINDING_ORDER,
					Value.of(windingOrderParam.toUpperCase()));
		}
		writer.setInstances(instances);
		DefaultSchemaSpace schemaSpace = new DefaultSchemaSpace();
		schemaSpace.addSchema(schema);
		writer.setTargetSchema(schemaSpace);
		File outFile = File.createTempFile(testName, ".gml"); //$NON-NLS-1$
		writer.setTarget(new FileIOSupplier(outFile));

		assertTrue(writer.getParameter(GeoInstanceWriter.PARAM_UNIFY_WINDING_ORDER)
				.as(EnumWindingOrderTypes.class) == EnumWindingOrderTypes.COUNTERCLOCKWISE);

		IOReport report = writer.execute(null); // new LogProgressIndicator());
		if (expectWriteFail) {
			assertFalse("Writing the GML output should not be successful", report.isSuccess());
			return report;
		}
		else {
			assertTrue("Writing the GML output not successful", report.isSuccess());
		}

		List<? extends Locatable> validationSchemas = writer.getValidationSchemas();

		System.out.println(outFile.getAbsolutePath());
		System.out.println(targetSchema.toString());

//		if (!DEL_TEMP_FILES && Desktop.isDesktopSupported()) {
//			Desktop.getDesktop().open(outFile);
//		}

		IOReport valReport = validate(outFile.toURI(), validationSchemas);

		// load file
		InstanceCollection loaded = loadGML(outFile.toURI(), schema);

		ResourceIterator<Instance> it = loaded.iterator();
		try {
			assertTrue(it.hasNext());

			if (!skipValueTest) {
				Instance l = it.next();

				// test values
				for (Entry<List<QName>, Object> entry : values.entrySet()) {
					// XXX conversion?

					Object expected = entry.getValue();

//					String propertyPath = Joiner.on('.').join(Collections2.transform(entry.getKey(), new Function<QName, String>() {
//
//						@Override
//						public String apply(QName input) {
//							return input.toString();
//						}
//					}));
//					Collection<Object> propValues = PropertyResolver.getValues(
//							l, propertyPath, true);
//					assertEquals(1, propValues.size());
//					Object value = propValues.iterator().next();

					Collection<GeometryProperty<?>> geoms = GeometryUtil.getAllGeometries(l);
					assertEquals(1, geoms.size());
					Object value = geoms.iterator().next().getGeometry();

					if (expected instanceof Geometry && value instanceof Geometry) {

						// I have to comment below line to test Winding Order.
						// Below method got assertion failed error.
						matchGeometries((Geometry) expected, (Geometry) value);

						// Winding Order Test.
						if (windingOrderParam != null) {
							if (windingOrderParam
									.toUpperCase() == EnumWindingOrderTypes.COUNTERCLOCKWISE
											.toString()) {
								assertFalse(((Geometry) expected).equalsExact((Geometry) value));
								assertTrue(((Geometry) expected)
										.getNumGeometries() == ((Geometry) value)
												.getNumGeometries());
								assertTrue(WindingOrder.isCounterClockwise((Geometry) value));
							}
							else if (windingOrderParam
									.toUpperCase() == EnumWindingOrderTypes.CLOCKWISE.toString()) {
								assertTrue(((Geometry) expected).equalsExact((Geometry) value));
							}
							else {
								assertTrue(WindingOrder
										.isCounterClockwise((Geometry) value) == WindingOrder
												.isCounterClockwise((Geometry) expected));
							}
						}
						else {
							// TODO check winding order is CCW
							assertTrue(WindingOrder.isCounterClockwise((Geometry) value));
							// assertTrue(1 == 0);
						}
					}
					else {
						assertEquals(expected.toString(), value.toString());
					}
				}

				assertFalse(it.hasNext());
			}
		} finally {
			it.close();
		}

		if (DEL_TEMP_FILES) {
			outFile.deleteOnExit();
		}

		return valReport;
	}

	/**
	 * Let the test fail if the given geometries don't match
	 * 
	 * @param expected the expected geometry
	 * @param value the geometry value
	 */
	private void matchGeometries(Geometry expected, Geometry value) {
		if (expected.toString().equals(value.toString())) {
			// direct match
			return;
		}

		// check match for all no-loss conversions on value
		ConversionLadder ladder = GeometryConverterRegistry.getInstance().createNoLossLadder(value);
		while (ladder.hasNext()) {
			Geometry converted = ladder.next();
			if (expected.toString().equals(converted.toString())) {
				// match
				return;
			}
		}

		assertEquals("Geometry not compatible to expected geometry", expected.toString(), //$NON-NLS-1$
				value.toString());
	}

	/**
	 * Load GML from a file.
	 * 
	 * @param sourceData the GML file
	 * @param schema the schema location
	 * @return the features
	 * @throws IOException if loading the file fails
	 * @throws IOProviderConfigurationException if the instance reader is not
	 *             configured correctly
	 */
	private InstanceCollection loadGML(URI sourceData, Schema schema)
			throws IOException, IOProviderConfigurationException {
		InstanceReader instanceReader = new GmlInstanceReader();

		instanceReader.setSource(new DefaultInputSupplier(sourceData));
		instanceReader.setSourceSchema(schema);

		IOReport instanceReport = instanceReader.execute(null);
		assertTrue(instanceReport.isSuccess());

		return instanceReader.getInstances();
	}

	/**
	 * Validate an XML file against the given schemas.
	 * 
	 * @param xmlLocation the location of the XML file
	 * @param validationSchemas schemas needed for validation
	 * @return the validation report
	 * @throws IOProviderConfigurationException if the validator is not
	 *             configured correctly
	 * @throws IOException if I/O operations fail
	 */
	private IOReport validate(URI xmlLocation, List<? extends Locatable> validationSchemas)
			throws IOProviderConfigurationException, IOException {
		XmlInstanceValidator validator = new XmlInstanceValidator();
		validator.setSchemas(validationSchemas.toArray(new Locatable[validationSchemas.size()]));
		validator.setSource(new DefaultInputSupplier(xmlLocation));

		return validator.execute(null);
	}

	/**
	 * Prepare the conversion service
	 */
	@BeforeClass
	public static void initAll() {
		TestUtil.startConversionService();
	}

}
