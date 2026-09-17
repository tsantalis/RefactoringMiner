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

package dev.ludovic.blas;

public class NetlibWrapperBLAS implements BLAS {

  private final com.github.fommil.netlib.BLAS blas;

  protected NetlibWrapperBLAS(com.github.fommil.netlib.BLAS _blas) {
    this.blas = _blas;
  }

  public static BLAS getInstance(com.github.fommil.netlib.BLAS blas) {
    //FIXME: cache this object on blas
    return new NetlibWrapperBLAS(blas);
  }

  public double dasum(int n, double[] x, int incx) {
    return blas.dasum(n, x, incx);
  }

  public double dasum(int n, double[] x, int offsetx, int incx) {
    return blas.dasum(n, x, offsetx, incx);
  }

  public float sasum(int n, float[] x, int incx) {
    return blas.sasum(n, x, incx);
  }

  public float sasum(int n, float[] x, int offsetx, int incx) {
    return blas.sasum(n, x, offsetx, incx);
  }

  public void daxpy(int n, double alpha, double[] x, int incx, double[] y, int incy) {
    blas.daxpy(n, alpha, x, incx, y, incy);
  }

  public void daxpy(int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy) {
    blas.daxpy(n, alpha, x, offsetx, incx, y, offsety, incy);
  }

  public void saxpy(int n, float alpha, float[] x, int incx, float[] y, int incy) {
    blas.saxpy(n, alpha, x, incx, y, incy);
  }

  public void saxpy(int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy) {
    blas.saxpy(n, alpha, x, offsetx, incx, y, offsety, incy);
  }

  public void dcopy(int n, double[] x, int incx, double[] y, int incy) {
    blas.dcopy(n, x, incx, y, incy);
  }

  public void dcopy(int n, double[] x, int offsetx, int incx, double[] y, int offsety, int incy) {
    blas.dcopy(n, x, offsetx, incx, y, offsety, incy);
  }

  public void scopy(int n, float[] x, int incx, float[] y, int incy) {
    blas.scopy(n, x, incx, y, incy);
  }

  public void scopy(int n, float[] x, int offsetx, int incx, float[] y, int offsety, int incy) {
    blas.scopy(n, x, offsetx, incx, y, offsety, incy);
  }

  public double ddot(int n, double[] x, int incx, double[] y, int incy) {
    return blas.ddot(n, x, incx, y, incy);
  }

  public double ddot(int n, double[] x, int offsetx, int incx, double[] y, int offsety, int incy) {
    return blas.ddot(n, x, offsetx, incx, y, offsety, incy);
  }

  public float sdot(int n, float[] x, int incx, float[] y, int incy) {
    return blas.sdot(n, x, incx, y, incy);
  }

  public float sdot(int n, float[] x, int offsetx, int incx, float[] y, int offsety, int incy) {
    return blas.sdot(n, x, offsetx, incx, y, offsety, incy);
  }

  public float sdsdot(int n, float sb, float[] sx, int incx, float[] sy, int incy) {
    return blas.sdsdot(n, sb, sx, incx, sy, incy);
  }
  public float sdsdot(int n, float sb, float[] sx, int _sx_offset, int incx, float[] sy, int _sy_offset, int incy) {
    return blas.sdsdot(n, sb, sx, _sx_offset, incx, sy, _sy_offset, incy);
  }

  public void dgbmv(String trans, int m, int n, int kl, int ku, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    blas.dgbmv(trans, m, n, kl, ku, alpha, a, lda, x, incx, beta, y, incy);
  }
  public void dgbmv(String trans, int m, int n, int kl, int ku, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy) {
    blas.dgbmv(trans, m, n, kl, ku, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void sgbmv(String trans, int m, int n, int kl, int ku, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy) {
    blas.sgbmv(trans, m, n, kl, ku, alpha, a, lda, x, incx, beta, y, incy);
  }
  public void sgbmv(String trans, int m, int n, int kl, int ku, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy) {
    blas.sgbmv(trans, m, n, kl, ku, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void dgemm(String transa, String transb, int m, int n, int k, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int ldc) {
    blas.dgemm(transa, transb, m, n, k, alpha, a, lda, b, ldb, beta, c, ldc);
  }

  public void dgemm(String transa, String transb, int m, int n, int k, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb, double beta, double[] c, int offsetc, int ldc) {
    blas.dgemm(transa, transb, m, n, k, alpha, a, offseta, lda, b, offsetb, ldb, beta, c, offsetc, ldc);
  }

  public void sgemm(String transa, String transb, int m, int n, int k, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int ldc) {
    blas.sgemm(transa, transb, m, n, k, alpha, a, lda, b, ldb, beta, c, ldc);
  }
  public void sgemm(String transa, String transb, int m, int n, int k, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb, float beta, float[] c, int offsetc, int ldc) {
    blas.sgemm(transa, transb, m, n, k, alpha, a, offseta, lda, b, offsetb, ldb, beta, c, offsetc, ldc);
  }

  public void dgemv(String trans, int m, int n, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    blas.dgemv(trans, m, n, alpha, a, lda, x, incx, beta, y, incy);
  }

  public void dgemv(String trans, int m, int n, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy) {
    blas.dgemv(trans, m, n, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void sgemv(String trans, int m, int n, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy) {
    blas.sgemv(trans, m, n, alpha, a, lda, x, incx, beta, y, incy);
  }

  public void sgemv(String trans, int m, int n, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy) {
    blas.sgemv(trans, m, n, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void dger(int m, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] a, int lda) {
    blas.dger(m, n, alpha, x, incx, y, incy, a, lda);
  }
  public void dger(int m, int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy, double[] a, int offseta, int lda) {
    blas.dger(m, n, alpha, x, offsetx, incx, y, offsety, incy, a, offseta, lda);
  }

  public void sger(int m, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] a, int lda) {
    blas.sger(m, n, alpha, x, incx, y, incy, a, lda);
  }
  public void sger(int m, int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy, float[] a, int offseta, int lda) {
    blas.sger(m, n, alpha, x, offsetx, incx, y, offsety, incy, a, offseta, lda);
  }

  public double dnrm2(int n, double[] x, int incx) {
    return blas.dnrm2(n, x, incx);
  }
  public double dnrm2(int n, double[] x, int offsetx, int incx) {
    return blas.dnrm2(n, x, offsetx, incx);
  }

  public float snrm2(int n, float[] x, int incx) {
    return blas.snrm2(n, x, incx);
  }
  public float snrm2(int n, float[] x, int offsetx, int incx) {
    return blas.snrm2(n, x, offsetx, incx);
  }

  public void drot(int n, double[] dx, int incx, double[] dy, int incy, double c, double s) {
    blas.drot(n, dx, incx, dy, incy, c, s);
  }
  public void drot(int n, double[] dx, int offsetdx, int incx, double[] dy, int offsetdy, int incy, double c, double s) {
    blas.drot(n, dx, offsetdx, incx, dy, offsetdy, incy, c, s);
  }

  public void srot(int n, float[] sx, int incx, float[] sy, int incy, float c, float s) {
    blas.srot(n, sx, incx, sy, incy, c, s);
  }
  public void srot(int n, float[] sx, int _sx_offset, int incx, float[] sy, int _sy_offset, int incy, float c, float s) {
    blas.srot(n, sx, _sx_offset, incx, sy, _sy_offset, incy, c, s);
  }

  public void drotg(org.netlib.util.doubleW da, org.netlib.util.doubleW db, org.netlib.util.doubleW c, org.netlib.util.doubleW s) {
    blas.drotg(da, db, c, s);
  }

  public void srotg(org.netlib.util.floatW sa, org.netlib.util.floatW sb, org.netlib.util.floatW c, org.netlib.util.floatW s) {
    blas.srotg(sa, sb, c, s);
  }

  public void drotm(int n, double[] dx, int incx, double[] dy, int incy, double[] dparam) {
    blas.drotm(n, dx, incx, dy, incy, dparam);
  }
  public void drotm(int n, double[] dx, int offsetdx, int incx, double[] dy, int offsetdy, int incy, double[] dparam, int _dparam_offset) {
    blas.drotm(n, dx, offsetdx, incx, dy, offsetdy, incy, dparam, _dparam_offset);
  }

  public void srotm(int n, float[] sx, int incx, float[] sy, int incy, float[] sparam) {
    blas.srotm(n, sx, incx, sy, incy, sparam);
  }
  public void srotm(int n, float[] sx, int _sx_offset, int incx, float[] sy, int _sy_offset, int incy, float[] sparam, int _sparam_offset) {
    blas.srotm(n, sx, _sx_offset, incx, sy, _sy_offset, incy, sparam, _sparam_offset);
  }

  public void drotmg(org.netlib.util.doubleW dd1, org.netlib.util.doubleW dd2, org.netlib.util.doubleW dx1, double dy1, double[] dparam) {
    blas.drotmg(dd1, dd2, dx1, dy1, dparam);
  }
  public void drotmg(org.netlib.util.doubleW dd1, org.netlib.util.doubleW dd2, org.netlib.util.doubleW dx1, double dy1, double[] dparam, int _dparam_offset) {
    blas.drotmg(dd1, dd2, dx1, dy1, dparam, _dparam_offset);
  }

  public void srotmg(org.netlib.util.floatW sd1, org.netlib.util.floatW sd2, org.netlib.util.floatW sx1, float sy1, float[] sparam) {
    blas.srotmg(sd1, sd2, sx1, sy1, sparam);
  }
  public void srotmg(org.netlib.util.floatW sd1, org.netlib.util.floatW sd2, org.netlib.util.floatW sx1, float sy1, float[] sparam, int _sparam_offset) {
    blas.srotmg(sd1, sd2, sx1, sy1, sparam, _sparam_offset);
  }

  public void dsbmv(String uplo, int n, int k, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    blas.dsbmv(uplo, n, k, alpha, a, lda, x, incx, beta, y, incy);
  }
  public void dsbmv(String uplo, int n, int k, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy) {
    blas.dsbmv(uplo, n, k, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void ssbmv(String uplo, int n, int k, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy) {
    blas.ssbmv(uplo, n, k, alpha, a, lda, x, incx, beta, y, incy);
  }
  public void ssbmv(String uplo, int n, int k, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy) {
    blas.ssbmv(uplo, n, k, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void dscal(int n, double alpha, double[] x, int incx) {
    blas.dscal(n, alpha, x, incx);
  }

  public void dscal(int n, double alpha, double[] x, int offsetx, int incx) {
    blas.dscal(n, alpha, x, offsetx, incx);
  }

  public void sscal(int n, float alpha, float[] x, int incx) {
    blas.sscal(n, alpha, x, incx);
  }

  public void sscal(int n, float alpha, float[] x, int offsetx, int incx) {
    blas.sscal(n, alpha, x, offsetx, incx);
  }

  public void dspmv(String uplo, int n, double alpha, double[] a, double[] x, int incx, double beta, double[] y, int incy) {
    blas.dspmv(uplo, n, alpha, a, x, incx, beta, y, incy);
  }

  public void dspmv(String uplo, int n, double alpha, double[] a, int offseta, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy) {
    blas.dspmv(uplo, n, alpha, a, offseta, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void sspmv(String uplo, int n, float alpha, float[] ap, float[] x, int incx, float beta, float[] y, int incy) {
    blas.sspmv(uplo, n, alpha, ap, x, incx, beta, y, incy);
  }
  public void sspmv(String uplo, int n, float alpha, float[] ap, int offsetap, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy) {
    blas.sspmv(uplo, n, alpha, ap, offsetap, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void dspr(String uplo, int n, double alpha, double[] x, int incx, double[] a) {
    blas.dspr(uplo, n, alpha, x, incx, a);
  }

  public void dspr(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] a, int offseta) {
    blas.dspr(uplo, n, alpha, x, offsetx, incx, a, offseta);
  }

  public void sspr(String uplo, int n, float alpha, float[] x, int incx, float[] ap) {
    blas.sspr(uplo, n, alpha, x, incx, ap);
  }
  public void sspr(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] ap, int offsetap) {
    blas.sspr(uplo, n, alpha, x, offsetx, incx, ap, offsetap);
  }

  public void dspr2(String uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] ap) {
    blas.dspr2(uplo, n, alpha, x, incx, y, incy, ap);
  }
  public void dspr2(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy, double[] ap, int offsetap) {
    blas.dspr2(uplo, n, alpha, x, offsetx, incx, y, offsety, incy, ap, offsetap);
  }

  public void sspr2(String uplo, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] ap) {
    blas.sspr2(uplo, n, alpha, x, incx, y, incy, ap);
  }
  public void sspr2(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy, float[] ap, int offsetap) {
    blas.sspr2(uplo, n, alpha, x, offsetx, incx, y, offsety, incy, ap, offsetap);
  }

  public void dswap(int n, double[] x, int incx, double[] y, int incy) {
    blas.dswap(n, x, incx, y, incy);
  }

  public void dswap(int n, double[] x, int offsetx, int incx, double[] y, int offsety, int incy) {
    blas.dswap(n, x, offsetx, incx, y, offsety, incy);
  }

  public void sswap(int n, float[] x, int incx, float[] y, int incy) {
    blas.sswap(n, x, incx, y, incy);
  }

  public void sswap(int n, float[] x, int offsetx, int incx, float[] y, int offsety, int incy) {
    blas.sswap(n, x, offsetx, incx, y, offsety, incy);
  }

  public void dsymm(String side, String uplo, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int ldc) {
    blas.dsymm(side, uplo, m, n, alpha, a, lda, b, ldb, beta, c, ldc);
  }
  public void dsymm(String side, String uplo, int m, int n, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb, double beta, double[] c, int offsetc, int ldc) {
    blas.dsymm(side, uplo, m, n, alpha, a, offseta, lda, b, offsetb, ldb, beta, c, offsetc, ldc);
  }

  public void ssymm(String side, String uplo, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int ldc) {
    blas.ssymm(side, uplo, m, n, alpha, a, lda, b, ldb, beta, c, ldc);
  }
  public void ssymm(String side, String uplo, int m, int n, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb, float beta, float[] c, int offsetc, int ldc) {
    blas.ssymm(side, uplo, m, n, alpha, a, offseta, lda, b, offsetb, ldb, beta, c, offsetc, ldc);
  }

  public void dsymv(String uplo, int n, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    blas.dsymv(uplo, n, alpha, a, lda, x, incx, beta, y, incy);
  }
  public void dsymv(String uplo, int n, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy) {
    blas.dsymv(uplo, n, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void ssymv(String uplo, int n, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy) {
    blas.ssymv(uplo, n, alpha, a, lda, x, incx, beta, y, incy);
  }
  public void ssymv(String uplo, int n, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy) {
    blas.ssymv(uplo, n, alpha, a, offseta, lda, x, offsetx, incx, beta, y, offsety, incy);
  }

  public void dsyr(String uplo, int n, double alpha, double[] x, int incx, double[] a, int lda) {
    blas.dsyr(uplo, n, alpha, x, incx, a, lda);
  }

  public void dsyr(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] a, int offseta, int lda) {
    blas.dsyr(uplo, n, alpha, x, offsetx, incx, a, offseta, lda);
  }

  public void ssyr(String uplo, int n, float alpha, float[] x, int incx, float[] a, int lda) {
    blas.ssyr(uplo, n, alpha, x, incx, a, lda);
  }
  public void ssyr(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] a, int offseta, int lda) {
    blas.ssyr(uplo, n, alpha, x, offsetx, incx, a, offseta, lda);
  }

  public void dsyr2(String uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] a, int lda) {
    blas.dsyr2(uplo, n, alpha, x, incx, y, incy, a, lda);
  }
  public void dsyr2(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy, double[] a, int offseta, int lda) {
    blas.dsyr2(uplo, n, alpha, x, offsetx, incx, y, offsety, incy, a, offseta, lda);
  }

  public void ssyr2(String uplo, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] a, int lda) {
    blas.ssyr2(uplo, n, alpha, x, incx, y, incy, a, lda);
  }
  public void ssyr2(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy, float[] a, int offseta, int lda) {
    blas.ssyr2(uplo, n, alpha, x, offsetx, incx, y, offsety, incy, a, offseta, lda);
  }

  public void dsyr2k(String uplo, String trans, int n, int k, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int ldc) {
    blas.dsyr2k(uplo, trans, n, k, alpha, a, lda, b, ldb, beta, c, ldc);
  }
  public void dsyr2k(String uplo, String trans, int n, int k, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb, double beta, double[] c, int offsetc, int ldc) {
    blas.dsyr2k(uplo, trans, n, k, alpha, a, offseta, lda, b, offsetb, ldb, beta, c, offsetc, ldc);
  }

  public void ssyr2k(String uplo, String trans, int n, int k, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int ldc) {
    blas.ssyr2k(uplo, trans, n, k, alpha, a, lda, b, ldb, beta, c, ldc);
  }
  public void ssyr2k(String uplo, String trans, int n, int k, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb, float beta, float[] c, int offsetc, int ldc) {
    blas.ssyr2k(uplo, trans, n, k, alpha, a, offseta, lda, b, offsetb, ldb, beta, c, offsetc, ldc);
  }

  public void dsyrk(String uplo, String trans, int n, int k, double alpha, double[] a, int lda, double beta, double[] c, int ldc) {
    blas.dsyrk(uplo, trans, n, k, alpha, a, lda, beta, c, ldc);
  }
  public void dsyrk(String uplo, String trans, int n, int k, double alpha, double[] a, int offseta, int lda, double beta, double[] c, int offsetc, int ldc) {
    blas.dsyrk(uplo, trans, n, k, alpha, a, offseta, lda, beta, c, offsetc, ldc);
  }

  public void ssyrk(String uplo, String trans, int n, int k, float alpha, float[] a, int lda, float beta, float[] c, int ldc) {
    blas.ssyrk(uplo, trans, n, k, alpha, a, lda, beta, c, ldc);
  }
  public void ssyrk(String uplo, String trans, int n, int k, float alpha, float[] a, int offseta, int lda, float beta, float[] c, int offsetc, int ldc) {
    blas.ssyrk(uplo, trans, n, k, alpha, a, offseta, lda, beta, c, offsetc, ldc);
  }

  public void dtbmv(String uplo, String trans, String diag, int n, int k, double[] a, int lda, double[] x, int incx) {
    blas.dtbmv(uplo, trans, diag, n, k, a, lda, x, incx);
  }
  public void dtbmv(String uplo, String trans, String diag, int n, int k, double[] a, int offseta, int lda, double[] x, int offsetx, int incx) {
    blas.dtbmv(uplo, trans, diag, n, k, a, offseta, lda, x, offsetx, incx);
  }

  public void stbmv(String uplo, String trans, String diag, int n, int k, float[] a, int lda, float[] x, int incx) {
    blas.stbmv(uplo, trans, diag, n, k, a, lda, x, incx);
  }
  public void stbmv(String uplo, String trans, String diag, int n, int k, float[] a, int offseta, int lda, float[] x, int offsetx, int incx) {
    blas.stbmv(uplo, trans, diag, n, k, a, offseta, lda, x, offsetx, incx);
  }

  public void dtbsv(String uplo, String trans, String diag, int n, int k, double[] a, int lda, double[] x, int incx) {
    blas.dtbsv(uplo, trans, diag, n, k, a, lda, x, incx);
  }
  public void dtbsv(String uplo, String trans, String diag, int n, int k, double[] a, int offseta, int lda, double[] x, int offsetx, int incx) {
    blas.dtbsv(uplo, trans, diag, n, k, a, offseta, lda, x, offsetx, incx);
  }

  public void stbsv(String uplo, String trans, String diag, int n, int k, float[] a, int lda, float[] x, int incx) {
    blas.stbsv(uplo, trans, diag, n, k, a, lda, x, incx);
  }
  public void stbsv(String uplo, String trans, String diag, int n, int k, float[] a, int offseta, int lda, float[] x, int offsetx, int incx) {
    blas.stbsv(uplo, trans, diag, n, k, a, offseta, lda, x, offsetx, incx);
  }

  public void dtpmv(String uplo, String trans, String diag, int n, double[] ap, double[] x, int incx) {
    blas.dtpmv(uplo, trans, diag, n, ap, x, incx);
  }
  public void dtpmv(String uplo, String trans, String diag, int n, double[] ap, int offsetap, double[] x, int offsetx, int incx) {
    blas.dtpmv(uplo, trans, diag, n, ap, offsetap, x, offsetx, incx);
  }

  public void stpmv(String uplo, String trans, String diag, int n, float[] ap, float[] x, int incx) {
    blas.stpmv(uplo, trans, diag, n, ap, x, incx);
  }
  public void stpmv(String uplo, String trans, String diag, int n, float[] ap, int offsetap, float[] x, int offsetx, int incx) {
    blas.stpmv(uplo, trans, diag, n, ap, offsetap, x, offsetx, incx);
  }

  public void dtpsv(String uplo, String trans, String diag, int n, double[] ap, double[] x, int incx) {
    blas.dtpsv(uplo, trans, diag, n, ap, x, incx);
  }
  public void dtpsv(String uplo, String trans, String diag, int n, double[] ap, int offsetap, double[] x, int offsetx, int incx) {
    blas.dtpsv(uplo, trans, diag, n, ap, offsetap, x, offsetx, incx);
  }

  public void stpsv(String uplo, String trans, String diag, int n, float[] ap, float[] x, int incx) {
    blas.stpsv(uplo, trans, diag, n, ap, x, incx);
  }
  public void stpsv(String uplo, String trans, String diag, int n, float[] ap, int offsetap, float[] x, int offsetx, int incx) {
    blas.stpsv(uplo, trans, diag, n, ap, offsetap, x, offsetx, incx);
  }

  public void dtrmm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb) {
    blas.dtrmm(side, uplo, transa, diag, m, n, alpha, a, lda, b, ldb);
  }
  public void dtrmm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb) {
    blas.dtrmm(side, uplo, transa, diag, m, n, alpha, a, offseta, lda, b, offsetb, ldb);
  }

  public void strmm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb) {
    blas.strmm(side, uplo, transa, diag, m, n, alpha, a, lda, b, ldb);
  }
  public void strmm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb) {
    blas.strmm(side, uplo, transa, diag, m, n, alpha, a, offseta, lda, b, offsetb, ldb);
  }

  public void dtrmv(String uplo, String trans, String diag, int n, double[] a, int lda, double[] x, int incx) {
    blas.dtrmv(uplo, trans, diag, n, a, lda, x, incx);
  }
  public void dtrmv(String uplo, String trans, String diag, int n, double[] a, int offseta, int lda, double[] x, int offsetx, int incx) {
    blas.dtrmv(uplo, trans, diag, n, a, offseta, lda, x, offsetx, incx);
  }

  public void strmv(String uplo, String trans, String diag, int n, float[] a, int lda, float[] x, int incx) {
    blas.strmv(uplo, trans, diag, n, a, lda, x, incx);
  }
  public void strmv(String uplo, String trans, String diag, int n, float[] a, int offseta, int lda, float[] x, int offsetx, int incx) {
    blas.strmv(uplo, trans, diag, n, a, offseta, lda, x, offsetx, incx);
  }

  public void dtrsm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb) {
    blas.dtrsm(side, uplo, transa, diag, m, n, alpha, a, lda, b, ldb);
  }
  public void dtrsm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb) {
    blas.dtrsm(side, uplo, transa, diag, m, n, alpha, a, offseta, lda, b, offsetb, ldb);
  }

  public void strsm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb) {
    blas.strsm(side, uplo, transa, diag, m, n, alpha, a, lda, b, ldb);
  }
  public void strsm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb) {
    blas.strsm(side, uplo, transa, diag, m, n, alpha, a, offseta, lda, b, offsetb, ldb);
  }

  public void dtrsv(String uplo, String trans, String diag, int n, double[] a, int lda, double[] x, int incx) {
    blas.dtrsv(uplo, trans, diag, n, a, lda, x, incx);
  }
  public void dtrsv(String uplo, String trans, String diag, int n, double[] a, int offseta, int lda, double[] x, int offsetx, int incx) {
    blas.dtrsv(uplo, trans, diag, n, a, offseta, lda, x, offsetx, incx);
  }

  public void strsv(String uplo, String trans, String diag, int n, float[] a, int lda, float[] x, int incx) {
    blas.strsv(uplo, trans, diag, n, a, lda, x, incx);
  }
  public void strsv(String uplo, String trans, String diag, int n, float[] a, int offseta, int lda, float[] x, int offsetx, int incx) {
    blas.strsv(uplo, trans, diag, n, a, offseta, lda, x, offsetx, incx);
  }

  public int idamax(int n, double[] x, int incx) {
    return blas.idamax(n, x, incx);
  }
  public int idamax(int n, double[] x, int offsetx, int incx) {
    return blas.idamax(n, x, offsetx, incx);
  }

  public int isamax(int n, float[] sx, int incx) {
    return blas.isamax(n, sx, incx);
  }
  public int isamax(int n, float[] sx, int _sx_offset, int incx) {
    return blas.isamax(n, sx, _sx_offset, incx);
  }

  public boolean lsame(String ca, String cb) {
    return blas.lsame(ca, cb);
  }
}
