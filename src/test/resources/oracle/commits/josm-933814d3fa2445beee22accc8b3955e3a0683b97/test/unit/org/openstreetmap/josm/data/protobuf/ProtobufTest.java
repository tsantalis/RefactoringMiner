// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.geom.Ellipse2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.Feature;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.Layer;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTTile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorTileSource;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.vector.VectorDataSet;
import org.openstreetmap.josm.data.vector.VectorNode;
import org.openstreetmap.josm.data.vector.VectorWay;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test class for {@link ProtobufParser} and {@link ProtobufRecord}
 *
 * @author Taylor Smock
 * @since 17862
 */
class ProtobufTest {
    /**
     * Convert an int array into a byte array
     * @param intArray The int array to convert (NOTE: numbers must be below 255)
     * @return A byte array that can be used
     */
    static byte[] toByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            if (intArray[i] > Byte.MAX_VALUE - Byte.MIN_VALUE) {
                throw new IllegalArgumentException();
            }
            byteArray[i] = Integer.valueOf(intArray[i]).byteValue();
        }
        return byteArray;
    }

    @RegisterExtension
    JOSMTestRules josmTestRules = new JOSMTestRules().preferences();

    private Number bytesToVarInt(int... bytes) {
        byte[] byteArray = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byteArray[i] = (byte) bytes[i];
        }
        return ProtobufParser.convertByteArray(byteArray, ProtobufParser.VAR_INT_BYTE_SIZE);
    }

    /**
     * Test reading tile from Mapillary ( 14/3248/6258 )
     *
     * @throws IOException if there is a problem reading the file
     */
    @Test
    void testRead_14_3248_6258() throws IOException {
        File vectorTile = Paths.get(TestUtils.getTestDataRoot(), "pbf", "mapillary", "14", "3248", "6258.mvt").toFile();
        InputStream inputStream = Compression.getUncompressedFileInputStream(vectorTile);
        Collection<ProtobufRecord> records = new ProtobufParser(inputStream).allRecords();
        assertEquals(2, records.size());
        List<Layer> layers = new ArrayList<>();
        for (ProtobufRecord record : records) {
            if (record.getField() == Layer.LAYER_FIELD) {
                layers.add(new Layer(record.getBytes()));
            } else {
                fail(MessageFormat.format("Invalid field {0}", record.getField()));
            }
        }
        Layer mapillarySequences = layers.get(0);
        Layer mapillaryPictures = layers.get(1);
        assertEquals("mapillary-sequences", mapillarySequences.getName());
        assertEquals("mapillary-images", mapillaryPictures.getName());
        assertEquals(4096, mapillarySequences.getExtent());
        assertEquals(4096, mapillaryPictures.getExtent());

        assertEquals(1,
                mapillarySequences.getFeatures().stream().filter(feature -> feature.getId() == 233760500).count());
        Feature testSequence = mapillarySequences.getFeatures().stream().filter(feature -> feature.getId() == 233760500)
                .findAny().orElse(null);
        assertEquals("dpudn262yz6aitu33zh7bl", testSequence.getTags().get("key"));
        assertEquals("clnaw3kpokIAe_CsN5Qmiw", testSequence.getTags().get("ikey"));
        assertEquals("B1iNjH4Ohn25cRAGPhetfw", testSequence.getTags().get("userkey"));
        assertEquals(Long.valueOf(1557535457401L), Long.valueOf(testSequence.getTags().get("captured_at")));
        assertEquals(0, Integer.parseInt(testSequence.getTags().get("pano")));
    }

    @Test
    void testRead_17_26028_50060() throws IOException {
        File vectorTile = Paths.get(TestUtils.getTestDataRoot(), "pbf", "openinframap", "17", "26028", "50060.pbf")
                .toFile();
        InputStream inputStream = Compression.getUncompressedFileInputStream(vectorTile);
        Collection<ProtobufRecord> records = new ProtobufParser(inputStream).allRecords();
        List<Layer> layers = new ArrayList<>();
        for (ProtobufRecord record : records) {
            if (record.getField() == Layer.LAYER_FIELD) {
                layers.add(new Layer(record.getBytes()));
            } else {
                fail(MessageFormat.format("Invalid field {0}", record.getField()));
            }
        }
        assertEquals(19, layers.size());
        List<Layer> dataLayers = layers.stream().filter(layer -> !layer.getFeatures().isEmpty())
                .collect(Collectors.toList());
        // power_plant, power_plant_point, power_generator, power_heatmap_solar, and power_generator_area
        assertEquals(5, dataLayers.size());

        // power_generator_area was rendered incorrectly
        final Layer powerGeneratorArea = dataLayers.stream()
                .filter(layer -> "power_generator_area".equals(layer.getName())).findAny().orElse(null);
        assertNotNull(powerGeneratorArea);
        final int extent = powerGeneratorArea.getExtent();
        // 17/26028/50060 bounds
        VectorDataSet vectorDataSet = new VectorDataSet();
        MVTTile vectorTile1 = new MVTTile(new MapboxVectorTileSource(new ImageryInfo("Test info", "example.org")),
                26028, 50060, 17);
        vectorTile1.loadImage(Compression.getUncompressedFileInputStream(vectorTile));
        vectorDataSet.addTileData(vectorTile1);
        vectorDataSet.setZoom(17);
        final Way one = new Way();
        one.addNode(new Node(new LatLon(39.0687509, -108.5100816)));
        one.addNode(new Node(new LatLon(39.0687509, -108.5095751)));
        one.addNode(new Node(new LatLon(39.0687169, -108.5095751)));
        one.addNode(new Node(new LatLon(39.0687169, -108.5100816)));
        one.addNode(one.getNode(0));
        one.setOsmId(666293899, 2);
        final BBox searchBBox = one.getBBox();
        searchBBox.addPrimitive(one, 0.00001);
        final Collection<VectorNode> searchedNodes = vectorDataSet.searchNodes(searchBBox);
        final Collection<VectorWay> searchedWays = vectorDataSet.searchWays(searchBBox);
        assertEquals(4, searchedNodes.size());
    }

    @Test
    void testReadVarInt() {
        assertEquals(ProtobufParser.convertLong(0), bytesToVarInt(0x0));
        assertEquals(ProtobufParser.convertLong(1), bytesToVarInt(0x1));
        assertEquals(ProtobufParser.convertLong(127), bytesToVarInt(0x7f));
        // This should b 0xff 0xff 0xff 0xff 0x07, but we drop the leading bit when reading to a byte array
        Number actual = bytesToVarInt(0x7f, 0x7f, 0x7f, 0x7f, 0x07);
        assertEquals(ProtobufParser.convertLong(Integer.MAX_VALUE), actual,
                MessageFormat.format("Expected {0} but got {1}", Integer.toBinaryString(Integer.MAX_VALUE),
                        Long.toBinaryString(actual.longValue())));
    }

    /**
     * Test simple message.
     * Check that a simple message is readable
     *
     * @throws IOException - if an IO error occurs
     */
    @Test
    void testSimpleMessage() throws IOException {
        ProtobufParser parser = new ProtobufParser(new byte[] {(byte) 0x08, (byte) 0x96, (byte) 0x01});
        ProtobufRecord record = new ProtobufRecord(parser);
        assertEquals(WireType.VARINT, record.getType());
        assertEquals(150, record.asUnsignedVarInt().intValue());
    }

    @Test
    void testSingletonMultiPoint() throws IOException {
        Collection<ProtobufRecord> records = new ProtobufParser(new ByteArrayInputStream(toByteArray(
                new int[] {0x1a, 0x2c, 0x78, 0x02, 0x0a, 0x03, 0x74, 0x6d, 0x70, 0x28, 0x80, 0x20, 0x1a, 0x04, 0x6e,
                        0x61, 0x6d, 0x65, 0x22, 0x0b, 0x0a, 0x09, 0x54, 0x65, 0x73, 0x74, 0x20, 0x6e, 0x61, 0x6d, 0x65,
                        0x12, 0x0d, 0x18, 0x01, 0x12, 0x02, 0x00, 0x00, 0x22, 0x05, 0x09, 0xe0, 0x3e, 0x84, 0x27})))
                                .allRecords();
        List<Layer> layers = new ArrayList<>();
        for (ProtobufRecord record : records) {
            if (record.getField() == Layer.LAYER_FIELD) {
                layers.add(new Layer(record.getBytes()));
            } else {
                fail(MessageFormat.format("Invalid field {0}", record.getField()));
            }
        }
        assertEquals(1, layers.size());
        assertEquals(1, layers.get(0).getGeometry().size());
        Ellipse2D shape = (Ellipse2D) layers.get(0).getGeometry().iterator().next().getShapes().iterator().next();
        assertEquals(4016, shape.getCenterX());
        assertEquals(2498, shape.getCenterY());
    }

    @Test
    void testZigZag() {
        assertEquals(0, ProtobufParser.decodeZigZag(0).intValue());
        assertEquals(-1, ProtobufParser.decodeZigZag(1).intValue());
        assertEquals(1, ProtobufParser.decodeZigZag(2).intValue());
        assertEquals(-2, ProtobufParser.decodeZigZag(3).intValue());
    }
}
