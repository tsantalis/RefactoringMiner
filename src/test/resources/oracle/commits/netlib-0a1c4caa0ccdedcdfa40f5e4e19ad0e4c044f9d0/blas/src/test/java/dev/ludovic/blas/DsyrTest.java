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

public class DsyrTest extends BLASTest {

    @Test
    void testSanity() {
        int n = 4;
        double alpha = 0.15;
        double[] x = new double[] {
            0.0, 2.7, 3.5, 2.1 };
        double[] a = new double[] {
            0.0, 1.2, 2.2, 3.1,
            1.2, 3.2, 5.3, 4.6,
            2.2, 5.3, 1.8, 3.0,
            3.1, 4.6, 3.0, 0.8 };
        double[] expected = new double[] {
            0.0,    1.2,    2.2,    3.1,
            1.2, 4.2935,    5.3,    4.6,
            2.2, 6.7175, 3.6375,    3.0,
            3.1, 5.4505, 4.1025, 1.4615 };

        double[] a1 = a.clone();
        blas.dsyr("U", n, alpha, x, 1, a1, n);
        assertArrayEquals(expected, a1);
    }
}
