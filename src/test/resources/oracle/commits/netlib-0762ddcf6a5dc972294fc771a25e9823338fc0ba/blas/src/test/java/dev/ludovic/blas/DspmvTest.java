/*
* Copyright 2020, Ludovic Henry
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/

import dev.ludovic.blas.BLAS;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

public class DspmvTest extends BLASTest {

    @ParameterizedTest
    @MethodSource("BLASImplementations")
    void testSanity(BLAS blas) {
        int n = 4;
        double[] a = new double[] {
             3.0,
            -2.0, -8.0,
             2.0,  4.0, -3.0,
            -4.0,  7.0, -3.0, 0.0 };
        double[] x = new double[] {
             5.0, 2.0, -1.0, -9.0 };
        double[] y = new double[] {
            -3.0, 6.0, -8.0, -3.0 };
        double[] expected1 = new double[] {
            42.0, -87.0, 40.0, -6.0 };
        double[] expected2 = new double[] {
            19.5, -40.5, 16.0, -4.5 };
        double[] expected3 = new double[] {
            -25.5, 52.5, -32.0, -1.5 };
        double[] expected4 = new double[] {
            -3.0, 6.0, -8.0, -3.0 };
        double[] expected5 = new double[] {
            43.5, -90.0, 44.0, -4.5 };
        double[] expected6 = new double[] {
            46.5, -96.0, 52.0, -1.5 };
        double[] expected7 = new double[] {
            45.0, -93.0, 48.0, -3.0 };

        double[] y1 = y.clone();
        blas.dspmv("U", n,  1.0, a, x, 1,  1.0, y1, 1);
        assertArrayEquals(expected1, y1);

        double[] y2 = y.clone();
        blas.dspmv("U", n,  0.5, a, x, 1,  1.0, y2, 1);
        assertArrayEquals(expected2, y2);

        double[] y3 = y.clone();
        blas.dspmv("U", n, -0.5, a, x, 1,  1.0, y3, 1);
        assertArrayEquals(expected3, y3);

        double[] y4 = y.clone();
        blas.dspmv("U", n,  0.0, a, x, 1,  1.0, y4, 1);
        assertArrayEquals(expected4, y4);

        double[] y5 = y.clone();
        blas.dspmv("U", n,  1.0, a, x, 1,  0.5, y5, 1);
        assertArrayEquals(expected5, y5);

        double[] y6 = y.clone();
        blas.dspmv("U", n,  1.0, a, x, 1, -0.5, y6, 1);
        assertArrayEquals(expected6, y6);

        double[] y7 = y.clone();
        blas.dspmv("U", n,  1.0, a, x, 1,  0.0, y7, 1);
        assertArrayEquals(expected7, y7);
    }
}
