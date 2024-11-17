package htsjdk.samtools.cram.encoding.huffman.codec;

import htsjdk.samtools.cram.io.DefaultBitInputStream;
import htsjdk.samtools.cram.io.DefaultBitOutputStream;
import htsjdk.samtools.cram.structure.ReadTag;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by vadim on 22/04/2015.
 */
public class HuffmanTest {
    @Test
    public void testHuffmanIntHelper() throws IOException {
        int size = 1000000;

        HuffmanParamsCalculator cal = new HuffmanParamsCalculator();
        cal.add(ReadTag.nameType3BytesToInt("OQ", 'Z'), size);
        cal.add(ReadTag.nameType3BytesToInt("X0", 'C'), size);
        cal.add(ReadTag.nameType3BytesToInt("X0", 'c'), size);
        cal.add(ReadTag.nameType3BytesToInt("X0", 's'), size);
        cal.add(ReadTag.nameType3BytesToInt("X1", 'C'), size);
        cal.add(ReadTag.nameType3BytesToInt("X1", 'c'), size);
        cal.add(ReadTag.nameType3BytesToInt("X1", 's'), size);
        cal.add(ReadTag.nameType3BytesToInt("XA", 'Z'), size);
        cal.add(ReadTag.nameType3BytesToInt("XC", 'c'), size);
        cal.add(ReadTag.nameType3BytesToInt("XT", 'A'), size);
        cal.add(ReadTag.nameType3BytesToInt("OP", 'i'), size);
        cal.add(ReadTag.nameType3BytesToInt("OC", 'Z'), size);
        cal.add(ReadTag.nameType3BytesToInt("BQ", 'Z'), size);
        cal.add(ReadTag.nameType3BytesToInt("AM", 'c'), size);

        cal.calculate();

        HuffmanIntHelper helper = new HuffmanIntHelper(cal.values(), cal.bitLens());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DefaultBitOutputStream bos = new DefaultBitOutputStream(baos);

        for (int i = 0; i < size; i++) {
            for (int b : cal.values()) {
                helper.write(bos, b);
            }
        }

        bos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DefaultBitInputStream bis = new DefaultBitInputStream(bais);

        int counter = 0;
        for (int i = 0; i < size; i++) {
            for (int b : cal.values()) {
                int v = helper.read(bis);
                if (v != b) {
                    Assert.fail("Mismatch: " + v + " vs " + b + " at " + counter);
                }

                counter++;
            }
        }
    }

    @Test
    public void testHuffmanByteHelper() throws IOException {
        int size = 1000000;

        long time5 = System.nanoTime();
        HuffmanParamsCalculator cal = new HuffmanParamsCalculator();
        for (byte i = 33; i < 33 + 15; i++) {
            cal.add(i);
        }
        cal.calculate();

        HuffmanByteHelper helper = new HuffmanByteHelper(cal.valuesAsBytes(), cal.bitLens());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DefaultBitOutputStream bos = new DefaultBitOutputStream(baos);

        for (int i = 0; i < size; i++) {
            for (byte b : cal.valuesAsBytes()) {
                helper.write(bos, b);
            }
        }

        bos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DefaultBitInputStream bis = new DefaultBitInputStream(bais);

        int counter = 0;
        for (int i = 0; i < size; i++) {
            for (int b : cal.values()) {
                int v = helper.read(bis);
                if (v != b) {
                    Assert.fail("Mismatch: " + v + " vs " + b + " at " + counter);
                }

                counter++;
            }
        }
    }
}
