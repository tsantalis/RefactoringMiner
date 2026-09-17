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

public interface BLAS {

  public double dasum(int n, double[] x, int incx);
  public double dasum(int n, double[] x, int offsetx, int incx);

  public float sasum(int n, float[] x, int incx);
  public float sasum(int n, float[] x, int offsetx, int incx);

  public void daxpy(int n, double alpha, double[] x, int incx, double[] y, int incy);
  public void daxpy(int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy);

  public void saxpy(int n, float alpha, float[] x, int incx, float[] y, int incy);
  public void saxpy(int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy);

  public void dcopy(int n, double[] x, int incx, double[] y, int incy);
  public void dcopy(int n, double[] x, int offsetx, int incx, double[] y, int offsety, int incy);

  public void scopy(int n, float[] x, int incx, float[] y, int incy);
  public void scopy(int n, float[] x, int offsetx, int incx, float[] y, int offsety, int incy);

  public double ddot(int n, double[] x, int incx, double[] y, int incy);
  public double ddot(int n, double[] x, int offsetx, int incx, double[] y, int offsety, int incy);

  public float sdot(int n, float[] x, int incx, float[] y, int incy);
  public float sdot(int n, float[] x, int offsetx, int incx, float[] y, int offsety, int incy);

  public float sdsdot(int n, float sb, float[] sx, int incx, float[] sy, int incy);
  public float sdsdot(int n, float sb, float[] sx, int _sx_offset, int incx, float[] sy, int _sy_offset, int incy);

  public void dgbmv(String trans, int m, int n, int kl, int ku, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);
  public void dgbmv(String trans, int m, int n, int kl, int ku, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy);

  public void sgbmv(String trans, int m, int n, int kl, int ku, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);
  public void sgbmv(String trans, int m, int n, int kl, int ku, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy);

  public void dgemm(String transa, String transb, int m, int n, int k, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int ldc);
  public void dgemm(String transa, String transb, int m, int n, int k, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb, double beta, double[] c, int offsetc, int ldc);

  public void sgemm(String transa, String transb, int m, int n, int k, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int Ldc);
  public void sgemm(String transa, String transb, int m, int n, int k, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb, float beta, float[] c, int offsetc, int Ldc);

  public void dgemv(String trans, int m, int n, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);
  public void dgemv(String trans, int m, int n, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy);

  public void sgemv(String trans, int m, int n, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);
  public void sgemv(String trans, int m, int n, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy);

  public void dger(int m, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] a, int lda);
  public void dger(int m, int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy, double[] a, int offseta, int lda);

  public void sger(int m, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] a, int lda);
  public void sger(int m, int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy, float[] a, int offseta, int lda);

  public double dnrm2(int n, double[] x, int incx);
  public double dnrm2(int n, double[] x, int offsetx, int incx);

  public float snrm2(int n, float[] x, int incx);
  public float snrm2(int n, float[] x, int offsetx, int incx);

  public void drot(int n, double[] dx, int incx, double[] dy, int incy, double c, double s);
  public void drot(int n, double[] dx, int offsetdx, int incx, double[] dy, int offsetdy, int incy, double c, double s);

  public void srot(int n, float[] sx, int incx, float[] sy, int incy, float c, float s);
  public void srot(int n, float[] sx, int _sx_offset, int incx, float[] sy, int _sy_offset, int incy, float c, float s);

  public void drotg(org.netlib.util.doubleW da, org.netlib.util.doubleW db, org.netlib.util.doubleW c, org.netlib.util.doubleW s);

  public void srotg(org.netlib.util.floatW sa, org.netlib.util.floatW sb, org.netlib.util.floatW c, org.netlib.util.floatW s);

  public void drotm(int n, double[] dx, int incx, double[] dy, int incy, double[] dparam);
  public void drotm(int n, double[] dx, int offsetdx, int incx, double[] dy, int offsetdy, int incy, double[] dparam, int _dparam_offset);

  public void srotm(int n, float[] sx, int incx, float[] sy, int incy, float[] sparam);
  public void srotm(int n, float[] sx, int _sx_offset, int incx, float[] sy, int _sy_offset, int incy, float[] sparam, int _sparam_offset);

  public void drotmg(org.netlib.util.doubleW dd1, org.netlib.util.doubleW dd2, org.netlib.util.doubleW dx1, double dy1, double[] dparam);
  public void drotmg(org.netlib.util.doubleW dd1, org.netlib.util.doubleW dd2, org.netlib.util.doubleW dx1, double dy1, double[] dparam, int _dparam_offset);

  public void srotmg(org.netlib.util.floatW sd1, org.netlib.util.floatW sd2, org.netlib.util.floatW sx1, float sy1, float[] sparam);
  public void srotmg(org.netlib.util.floatW sd1, org.netlib.util.floatW sd2, org.netlib.util.floatW sx1, float sy1, float[] sparam, int _sparam_offset);

  public void dsbmv(String uplo, int n, int k, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);
  public void dsbmv(String uplo, int n, int k, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy);

  public void ssbmv(String uplo, int n, int k, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);
  public void ssbmv(String uplo, int n, int k, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy);

  public void dscal(int n, double alpha, double[] x, int incx);
  public void dscal(int n, double alpha, double[] x, int offsetx, int incx);

  public void sscal(int n, float alpha, float[] x, int incx);
  public void sscal(int n, float alpha, float[] x, int offsetx, int incx);

  public void dspmv(String uplo, int n, double alpha, double[] a, double[] x, int incx, double beta, double[] y, int incy);
  public void dspmv(String uplo, int n, double alpha, double[] a, int offseta, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy);

  public void sspmv(String uplo, int n, float alpha, float[] ap, float[] x, int incx, float beta, float[] y, int incy);
  public void sspmv(String uplo, int n, float alpha, float[] ap, int offsetap, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy);

  public void dspr(String uplo, int n, double alpha, double[] x, int incx, double[] a);
  public void dspr(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] a, int offseta);

  public void sspr(String uplo, int n, float alpha, float[] x, int incx, float[] ap);
  public void sspr(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] ap, int offsetap);

  public void dspr2(String uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] ap);
  public void dspr2(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy, double[] ap, int offsetap);

  public void sspr2(String uplo, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] ap);
  public void sspr2(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy, float[] ap, int offsetap);

  public void dswap(int n, double[] x, int incx, double[] y, int incy);
  public void dswap(int n, double[] x, int offsetx, int incx, double[] y, int offsety, int incy);

  public void sswap(int n, float[] x, int incx, float[] y, int incy);
  public void sswap(int n, float[] x, int offsetx, int incx, float[] y, int offsety, int incy);

  public void dsymm(String side, String uplo, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int Ldc);
  public void dsymm(String side, String uplo, int m, int n, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb, double beta, double[] c, int offsetc, int Ldc);

  public void ssymm(String side, String uplo, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int Ldc);
  public void ssymm(String side, String uplo, int m, int n, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb, float beta, float[] c, int offsetc, int Ldc);

  public void dsymv(String uplo, int n, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);
  public void dsymv(String uplo, int n, double alpha, double[] a, int offseta, int lda, double[] x, int offsetx, int incx, double beta, double[] y, int offsety, int incy);

  public void ssymv(String uplo, int n, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);
  public void ssymv(String uplo, int n, float alpha, float[] a, int offseta, int lda, float[] x, int offsetx, int incx, float beta, float[] y, int offsety, int incy);

  public void dsyr(String uplo, int n, double alpha, double[] x, int incx, double[] a, int lda);
  public void dsyr(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] a, int offseta, int lda);

  public void ssyr(String uplo, int n, float alpha, float[] x, int incx, float[] a, int lda);
  public void ssyr(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] a, int offseta, int lda);

  public void dsyr2(String uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] a, int lda);
  public void dsyr2(String uplo, int n, double alpha, double[] x, int offsetx, int incx, double[] y, int offsety, int incy, double[] a, int offseta, int lda);

  public void ssyr2(String uplo, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] a, int lda);
  public void ssyr2(String uplo, int n, float alpha, float[] x, int offsetx, int incx, float[] y, int offsety, int incy, float[] a, int offseta, int lda);

  public void dsyr2k(String uplo, String trans, int n, int k, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int Ldc);
  public void dsyr2k(String uplo, String trans, int n, int k, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb, double beta, double[] c, int offsetc, int Ldc);

  public void ssyr2k(String uplo, String trans, int n, int k, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int Ldc);
  public void ssyr2k(String uplo, String trans, int n, int k, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb, float beta, float[] c, int offsetc, int Ldc);

  public void dsyrk(String uplo, String trans, int n, int k, double alpha, double[] a, int lda, double beta, double[] c, int Ldc);
  public void dsyrk(String uplo, String trans, int n, int k, double alpha, double[] a, int offseta, int lda, double beta, double[] c, int offsetc, int Ldc);

  public void ssyrk(String uplo, String trans, int n, int k, float alpha, float[] a, int lda, float beta, float[] c, int Ldc);
  public void ssyrk(String uplo, String trans, int n, int k, float alpha, float[] a, int offseta, int lda, float beta, float[] c, int offsetc, int Ldc);

  public void dtbmv(String uplo, String trans, String diag, int n, int k, double[] a, int lda, double[] x, int incx);
  public void dtbmv(String uplo, String trans, String diag, int n, int k, double[] a, int offseta, int lda, double[] x, int offsetx, int incx);

  public void stbmv(String uplo, String trans, String diag, int n, int k, float[] a, int lda, float[] x, int incx);
  public void stbmv(String uplo, String trans, String diag, int n, int k, float[] a, int offseta, int lda, float[] x, int offsetx, int incx);

  public void dtbsv(String uplo, String trans, String diag, int n, int k, double[] a, int lda, double[] x, int incx);
  public void dtbsv(String uplo, String trans, String diag, int n, int k, double[] a, int offseta, int lda, double[] x, int offsetx, int incx);

  public void stbsv(String uplo, String trans, String diag, int n, int k, float[] a, int lda, float[] x, int incx);
  public void stbsv(String uplo, String trans, String diag, int n, int k, float[] a, int offseta, int lda, float[] x, int offsetx, int incx);

  public void dtpmv(String uplo, String trans, String diag, int n, double[] ap, double[] x, int incx);
  public void dtpmv(String uplo, String trans, String diag, int n, double[] ap, int offsetap, double[] x, int offsetx, int incx);

  public void stpmv(String uplo, String trans, String diag, int n, float[] ap, float[] x, int incx);
  public void stpmv(String uplo, String trans, String diag, int n, float[] ap, int offsetap, float[] x, int offsetx, int incx);

  public void dtpsv(String uplo, String trans, String diag, int n, double[] ap, double[] x, int incx);
  public void dtpsv(String uplo, String trans, String diag, int n, double[] ap, int offsetap, double[] x, int offsetx, int incx);

  public void stpsv(String uplo, String trans, String diag, int n, float[] ap, float[] x, int incx);
  public void stpsv(String uplo, String trans, String diag, int n, float[] ap, int offsetap, float[] x, int offsetx, int incx);

  public void dtrmm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb);
  public void dtrmm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb);

  public void strmm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb);
  public void strmm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb);

  public void dtrmv(String uplo, String trans, String diag, int n, double[] a, int lda, double[] x, int incx);
  public void dtrmv(String uplo, String trans, String diag, int n, double[] a, int offseta, int lda, double[] x, int offsetx, int incx);

  public void strmv(String uplo, String trans, String diag, int n, float[] a, int lda, float[] x, int incx);
  public void strmv(String uplo, String trans, String diag, int n, float[] a, int offseta, int lda, float[] x, int offsetx, int incx);

  public void dtrsm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb);
  public void dtrsm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int offseta, int lda, double[] b, int offsetb, int ldb);

  public void strsm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb);
  public void strsm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int offseta, int lda, float[] b, int offsetb, int ldb);

  public void dtrsv(String uplo, String trans, String diag, int n, double[] a, int lda, double[] x, int incx);
  public void dtrsv(String uplo, String trans, String diag, int n, double[] a, int offseta, int lda, double[] x, int offsetx, int incx);

  public void strsv(String uplo, String trans, String diag, int n, float[] a, int lda, float[] x, int incx);
  public void strsv(String uplo, String trans, String diag, int n, float[] a, int offseta, int lda, float[] x, int offsetx, int incx);

  public int idamax(int n, double[] x, int incx);
  public int idamax(int n, double[] x, int offsetx, int incx);

  public int isamax(int n, float[] sx, int incx);
  public int isamax(int n, float[] sx, int _sx_offset, int incx);

  public boolean lsame(String ca, String cb);
}
