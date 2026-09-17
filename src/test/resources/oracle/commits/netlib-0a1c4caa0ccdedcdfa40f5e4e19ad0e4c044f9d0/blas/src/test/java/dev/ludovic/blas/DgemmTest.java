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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DgemmTest extends BLASTest {

    @Test
    void testSanity() {
        int m = 4;
        int n = 2;
        int k = 3;
        double[] a = new double[] {
            0.0, 1.0, 0.0, 0.0,
            2.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 3.0 };
        double[] aT = new double[] {
            0.0, 2.0, 0.0,
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 3.0 };
        double[] b = new double[] {
            1.0, 0.0, 0.0,
            0.0, 2.0, 1.0 };
        double[] bT = new double[] {
            1.0, 0.0,
            0.0, 2.0,
            0.0, 1.0 };
        double[] c = new double[] {
            1.0, 0.0, 2.0, 1.0,
            0.0, 0.0, 1.0, 0.0 };
        double[] expected1 = new double[] {
            2.0, 1.0, 4.0, 2.0,
            4.0, 0.0, 4.0, 3.0 };
        double[] expected2 = new double[] {
            0.0, 1.0, 0.0, 0.0,
            4.0, 0.0, 2.0, 3.0 };
        double[] expected3 = new double[] {
            1.0, 0.0, 2.0, 1.0,
            0.0, 0.0, 1.0, 0.0 };

        double[] c1 = c.clone();
        blas.dgemm("N", "N", m, n, k, 1.0, a, m, b, k, 2.0, c1, m);
        assertArrayEquals(expected1, c1);

        double[] c2 = c.clone();
        blas.dgemm("N", "T", m, n, k, 1.0, a, m, bT, n, 2.0, c2, m);
        assertArrayEquals(expected1, c2);

        double[] c3 = c.clone();
        blas.dgemm("T", "N", m, n, k, 1.0, aT, k, b, k, 2.0, c3, m);
        assertArrayEquals(expected1, c3);

        double[] c4 = c.clone();
        blas.dgemm("T", "T", m, n, k, 1.0, aT, k, bT, n, 2.0, c4, m);
        assertArrayEquals(expected1, c4);

        double[] c5 = c.clone();
        blas.dgemm("N", "N", m, n, k, 1.0, a, m, b, k, 0.0, c5, m);
        assertArrayEquals(expected2, c5);

        double[] c6 = c.clone();
        blas.dgemm("N", "T", m, n, k, 1.0, a, m, bT, n, 0.0, c6, m);
        assertArrayEquals(expected2, c6);

        double[] c7 = c.clone();
        blas.dgemm("T", "N", m, n, k, 1.0, aT, k, b, k, 0.0, c7, m);
        assertArrayEquals(expected2, c7);

        double[] c8 = c.clone();
        blas.dgemm("T", "T", m, n, k, 1.0, aT, k, bT, n, 0.0, c8, m);
        assertArrayEquals(expected2, c8);

        double[] c9 = c.clone();
        blas.dgemm("N", "N", m, n, k, 0.0, a, m, b, k, 1.0, c9, m);
        assertArrayEquals(expected3, c9);

        double[] c10 = c.clone();
        blas.dgemm("N", "T", m, n, k, 0.0, a, m, bT, n, 1.0, c10, m);
        assertArrayEquals(expected3, c10);

        double[] c11 = c.clone();
        blas.dgemm("T", "N", m, n, k, 0.0, aT, k, b, k, 1.0, c11, m);
        assertArrayEquals(expected3, c11);

        double[] c12 = c.clone();
        blas.dgemm("T", "T", m, n, k, 0.0, aT, k, bT, n, 1.0, c12, m);
        assertArrayEquals(expected3, c12);
    }
}
