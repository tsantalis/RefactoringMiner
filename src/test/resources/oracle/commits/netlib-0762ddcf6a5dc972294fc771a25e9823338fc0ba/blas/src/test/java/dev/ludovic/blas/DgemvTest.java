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

public class DgemvTest extends BLASTest {

    @ParameterizedTest
    @MethodSource("BLASImplementations")
    void testSanity(BLAS blas) {
        int m = 4;
        int n = 3;
        double[] a = new double[] {
            0.0, 1.0, 0.0, 0.0,
            2.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 3.0 };
        double[] aT = new double[] {
            0.0, 2.0, 0.0,
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 3.0 };
        double[] x = new double[] {
            1.0, 2.0, 3.0 };
        double[] y = new double[] {
            1.0, 3.0, 1.0, 0.0 };
        double[] expected1 = new double[] {
            4.0, 1.0, 2.0, 9.0 };
        double[] expected2 = new double[] {
            6.0, 7.0, 4.0, 9.0 };
        double[] expected3 = new double[] {
            10.0, 8.0, 6.0, 18.0 };

        double[] y1 = y.clone();
        blas.dgemv("N", m, n, 1.0, a, m, x, 1, 2.0, y1, 1);
        assertArrayEquals(expected2, y1);

        double[] y2 = y.clone();
        blas.dgemv("T", n, m, 1.0, aT, n, x, 1, 2.0, y2, 1);
        assertArrayEquals(expected2, y2);

        double[] y3 = y.clone();
        blas.dgemv("N", m, n, 2.0, a, m, x, 1, 2.0, y3, 1);
        assertArrayEquals(expected3, y3);

        double[] y4 = y.clone();
        blas.dgemv("T", n, m, 2.0, aT, n, x, 1, 2.0, y4, 1);
        assertArrayEquals(expected3, y4);
    }
}
