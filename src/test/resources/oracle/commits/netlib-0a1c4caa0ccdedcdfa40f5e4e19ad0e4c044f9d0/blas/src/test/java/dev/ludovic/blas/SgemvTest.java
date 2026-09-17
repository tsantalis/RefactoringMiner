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

public class SgemvTest extends BLASTest {

    @Test
    void testSanity() {
        int m = 4;
        int n = 3;
        float[] a = new float[] {
            0.0f, 1.0f, 0.0f, 0.0f,
            2.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 3.0f };
        float[] aT = new float[] {
            0.0f, 2.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 3.0f };
        float[] x = new float[] {
            1.0f, 2.0f, 3.0f };
        float[] y = new float[] {
            1.0f, 3.0f, 1.0f, 0.0f };
        float[] expected1 = new float[] {
            4.0f, 1.0f, 2.0f, 9.0f };
        float[] expected2 = new float[] {
            6.0f, 7.0f, 4.0f, 9.0f };
        float[] expected3 = new float[] {
            10.0f, 8.0f, 6.0f, 18.0f };

        float[] y1 = y.clone();
        blas.sgemv("N", m, n, 1.0f, a, m, x, 1, 2.0f, y1, 1);
        assertArrayEquals(expected2, y1);

        float[] y2 = y.clone();
        blas.sgemv("T", n, m, 1.0f, aT, n, x, 1, 2.0f, y2, 1);
        assertArrayEquals(expected2, y2);

        float[] y3 = y.clone();
        blas.sgemv("N", m, n, 2.0f, a, m, x, 1, 2.0f, y3, 1);
        assertArrayEquals(expected3, y3);

        float[] y4 = y.clone();
        blas.sgemv("T", n, m, 2.0f, aT, n, x, 1, 2.0f, y4, 1);
        assertArrayEquals(expected3, y4);
    }
}
