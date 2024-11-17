/**
 * ****************************************************************************
 * Copyright 2013 EMBL-EBI
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package htsjdk.samtools.cram.encoding.reader;

import htsjdk.samtools.cram.CRAMException;
import htsjdk.samtools.cram.common.IntHashMap;
import htsjdk.samtools.cram.encoding.BitCodec;
import htsjdk.samtools.cram.encoding.DataSeries;
import htsjdk.samtools.cram.encoding.DataSeriesMap;
import htsjdk.samtools.cram.encoding.DataSeriesType;
import htsjdk.samtools.cram.encoding.Encoding;
import htsjdk.samtools.cram.encoding.EncodingFactory;
import htsjdk.samtools.cram.io.BitInputStream;
import htsjdk.samtools.cram.structure.CompressionHeader;
import htsjdk.samtools.cram.structure.EncodingID;
import htsjdk.samtools.cram.structure.EncodingKey;
import htsjdk.samtools.cram.structure.EncodingParams;
import htsjdk.samtools.cram.structure.ReadTag;
import htsjdk.samtools.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

public class DataReaderFactory {
    private static Log log = Log.getInstance(DataReaderFactory.class);

    private final static boolean collectStats = false;

    public AbstractReader buildReader(final AbstractReader reader,
                                      final BitInputStream bitInputStream, final Map<Integer, InputStream> inputMap,
                                      final CompressionHeader header, final int refId) throws IllegalArgumentException {
        reader.captureReadNames = header.readNamesIncluded;
        reader.refId = refId;
        reader.APDelta = header.APDelta;

        for (final Field field : reader.getClass().getFields()) {
            if (field.isAnnotationPresent(DataSeries.class)) {
                final DataSeries dataSeries = field.getAnnotation(DataSeries.class);
                final EncodingKey key = dataSeries.key();
                final DataSeriesType type = dataSeries.type();
                if (header.encodingMap.get(key) == null) {
                    log.debug("Encoding not found for key: " + key);
                } else {
                    try {
                        field.set(reader,
                                createReader(type, header.encodingMap.get(key), bitInputStream, inputMap));
                    } catch (IllegalAccessException e) {
                        throw new CRAMException(e);
                    }
                }
            }

            if (field.isAnnotationPresent(DataSeriesMap.class)) {
                final DataSeriesMap dataSeriesMap = field.getAnnotation(DataSeriesMap.class);
                final String name = dataSeriesMap.name();
                if ("TAG".equals(name)) {
                    final IntHashMap map = new IntHashMap();
                    for (final Integer key : header.tMap.keySet()) {
                        final EncodingParams params = header.tMap.get(key);
                        final DataReader<byte[]> tagReader = createReader(
                                DataSeriesType.BYTE_ARRAY, params, bitInputStream,
                                inputMap);
                        map.put(key, tagReader);
                    }
                    try {
                        field.set(reader, map);
                    } catch (IllegalAccessException e) {
                        throw new CRAMException(e);
                    }
                }
            }
        }

        reader.tagIdDictionary = header.dictionary;
        return reader;
    }

    private <T> DataReader<T> createReader(final DataSeriesType valueType,
                                           final EncodingParams params, final BitInputStream bitInputStream,
                                           final Map<Integer, InputStream> inputMap) {
        if (params.id == EncodingID.NULL)
            //noinspection ConstantConditions
            return collectStats ? new DataReaderWithStats(
                    buildNullReader(valueType)) : buildNullReader(valueType);

        final EncodingFactory encodingFactory = new EncodingFactory();
        final Encoding<T> encoding = encodingFactory.createEncoding(valueType, params.id);
        if (encoding == null)
            throw new RuntimeException("Encoding not found for value type "
                    + valueType.name() + ", id=" + params.id);
        encoding.fromByteArray(params.params);

        //noinspection ConstantConditions
        return collectStats ? new DataReaderWithStats(new DefaultDataReader<T>(
                encoding.buildCodec(inputMap, null), bitInputStream))
                : new DefaultDataReader<T>(encoding.buildCodec(inputMap, null),
                bitInputStream);
    }

    private static <T> DataReader<T> buildNullReader(final DataSeriesType valueType) {
        switch (valueType) {
            case BYTE:
                return (DataReader<T>) new SingleValueReader<Byte>((byte) 0);
            case INT:
                return (DataReader<T>) new SingleValueReader<Integer>(
                        0);
            case LONG:
                return (DataReader<T>) new SingleValueReader<Long>((long) 0);
            case BYTE_ARRAY:
                return (DataReader<T>) new SingleValueReader<byte[]>(new byte[]{});

            default:
                throw new RuntimeException("Unknown data type: " + valueType.name());
        }
    }

    private static class DefaultDataReader<T> implements DataReader<T> {
        private final BitCodec<T> codec;
        private final BitInputStream bitInputStream;

        public DefaultDataReader(final BitCodec<T> codec, final BitInputStream bitInputStream) {
            this.codec = codec;
            this.bitInputStream = bitInputStream;
        }

        @Override
        public T readData() throws IOException {
            return codec.read(bitInputStream);
        }

        @Override
        public T readDataArray(final int length) throws IOException {
            return codec.read(bitInputStream, length);
        }

    }

    private static class SingleValueReader<T> implements DataReader<T> {
        private final T value;

        public SingleValueReader(final T value) {
            this.value = value;
        }

        @Override
        public T readData() throws IOException {
            return value;
        }

        @Override
        public T readDataArray(final int length) {
            return value;
        }
    }

    public static class DataReaderWithStats<T> implements DataReader<T> {
        public long nanos = 0;
        final DataReader<T> delegate;

        public DataReaderWithStats(final DataReader<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T readData() throws IOException {
            final long time = System.nanoTime();
            final T value = delegate.readData();
            nanos += System.nanoTime() - time;
            return value;
        }

        @Override
        public T readDataArray(final int length) throws IOException {
            final long time = System.nanoTime();
            final T value = delegate.readDataArray(length);
            nanos += System.nanoTime() - time;
            return value;
        }
    }

    public Map<String, DataReaderWithStats> getStats(final CramRecordReader reader)
            throws IllegalArgumentException, IllegalAccessException {
        final Map<String, DataReaderWithStats> map = new TreeMap<String, DataReaderFactory.DataReaderWithStats>();
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!collectStats)
            return map;

        for (final Field field : reader.getClass().getFields()) {
            if (field.isAnnotationPresent(DataSeries.class)) {
                final DataSeries dataSeries = field.getAnnotation(DataSeries.class);
                final EncodingKey key = dataSeries.key();
                map.put(key.name(), (DataReaderWithStats) field.get(reader));
            }

            if (field.isAnnotationPresent(DataSeriesMap.class)) {
                final DataSeriesMap dataSeriesMap = field.getAnnotation(DataSeriesMap.class);
                final String name = dataSeriesMap.name();
                if ("TAG".equals(name)) {
                    final Map<Integer, DataReader<byte[]>> tagMap = (Map<Integer, DataReader<byte[]>>) field
                            .get(reader);
                    for (final Integer key : tagMap.keySet()) {
                        final String tag = ReadTag.intToNameType4Bytes(key);
                        map.put(tag, (DataReaderWithStats) tagMap.get(key));
                    }
                }
            }
        }

        return map;
    }

}
