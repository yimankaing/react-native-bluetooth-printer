package com.native_module.bluetooth_printer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;


public class PrintPicture {

  /**
   * @param mBitmap
   * @param nWidth
   * @param nMode
   * @return
   */
  public static byte[] POS_PrintBMP(Bitmap mBitmap, int nWidth, int nMode) {

    int width = ((nWidth + 7) / 8) * 8;
    int height = mBitmap.getHeight() * width / mBitmap.getWidth();
    height = ((height + 7) / 8) * 8;

    Bitmap rszBitmap = mBitmap;
    if (mBitmap.getWidth() != width) {
      rszBitmap = resizeImage(mBitmap, width, height);
    }

    Bitmap grayBitmap = toGrayscale(rszBitmap);

    byte[] dithered = thresholdToBWPic(grayBitmap);

    byte[] data = eachLinePixToCmd(dithered, width, nMode);

    return data;
  }

  /**
   * @param bmp
   * @return
   */
  public static byte[] Print_1D2A(Bitmap bmp) {

    int width = bmp.getWidth();
    int height = bmp.getHeight();
    byte data[] = new byte[1024 * 10];
    data[0] = 0x1D;
    data[1] = 0x2A;
    data[2] = (byte) ((width - 1) / 8 + 1);
    data[3] = (byte) ((height - 1) / 8 + 1);
    byte k = 0;
    int position = 4;
    int i;
    int j;
    byte temp = 0;
    for (i = 0; i < width; i++) {

      System.out.println("进来了...I");
      for (j = 0; j < height; j++) {
        System.out.println("进来了...J");
        if (bmp.getPixel(i, j) != -1) {
          temp |= (0x80 >> k);
        } // end if
        k++;
        if (k == 8) {
          data[position++] = temp;
          temp = 0;
          k = 0;
        } // end if k
      }// end for j
      if (k % 8 != 0) {
        data[position++] = temp;
        temp = 0;
        k = 0;
      }

    }
    System.out.println("data" + data);

    if (width % 8 != 0) {
      i = height / 8;
      if (height % 8 != 0) i++;
      j = 8 - (width % 8);
      for (k = 0; k < i * j; k++) {
        data[position++] = 0;
      }
    }
    return data;
  }

  public static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    float scaleWidth = (float) w / (float) width;
    float scaleHeight = (float) h / (float) height;
    Matrix matrix = new Matrix();
    matrix.postScale(scaleWidth, scaleHeight);
    Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    return resizedBitmap;
  }

  public static Bitmap toGrayscale(Bitmap bmpOriginal) {
    int height = bmpOriginal.getHeight();
    int width = bmpOriginal.getWidth();
    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bmpGrayscale);
    Paint paint = new Paint();
    ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(0.0F);
    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
    paint.setColorFilter(f);
    c.drawBitmap(bmpOriginal, 0.0F, 0.0F, paint);
    return bmpGrayscale;
  }

  public static byte[] thresholdToBWPic(Bitmap mBitmap) {
    int[] pixels = new int[mBitmap.getWidth() * mBitmap.getHeight()];
    byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight()];
    mBitmap.getPixels(pixels, 0, mBitmap.getWidth(), 0, 0, mBitmap.getWidth(), mBitmap.getHeight());
    format_K_threshold(pixels, mBitmap.getWidth(), mBitmap.getHeight(), data);
    return data;
  }

  private static void format_K_threshold(int[] orgpixels, int xsize, int ysize, byte[] despixels) {
    int graytotal = 0;
    boolean grayave = true;
    int k = 0;

    int i;
    int j;
    int gray;
    for (i = 0; i < ysize; ++i) {
      for (j = 0; j < xsize; ++j) {
        gray = orgpixels[k] & 255;
        graytotal += gray;
        ++k;
      }
    }

    int var10 = graytotal / ysize / xsize;
    k = 0;

    for (i = 0; i < ysize; ++i) {
      for (j = 0; j < xsize; ++j) {
        gray = orgpixels[k] & 255;
        if (gray > var10) {
          despixels[k] = 0;
        } else {
          despixels[k] = 1;
        }

        ++k;
      }
    }

  }

  public static byte[] eachLinePixToCmd(byte[] src, int nWidth, int nMode) {
    int[] p0 = new int[]{0, 128};
    int[] p1 = new int[]{0, 64};
    int[] p2 = new int[]{0, 32};
    int[] p3 = new int[]{0, 16};
    int[] p4 = new int[]{0, 8};
    int[] p5 = new int[]{0, 4};
    int[] p6 = new int[]{0, 2};

    int nHeight = src.length / nWidth;
    int nBytesPerLine = nWidth / 8;
    byte[] data = new byte[nHeight * (8 + nBytesPerLine)];
    boolean offset = false;
    int k = 0;

    for (int i = 0; i < nHeight; ++i) {
      int var10 = i * (8 + nBytesPerLine);
      data[var10 + 0] = 29;
      data[var10 + 1] = 118;
      data[var10 + 2] = 48;
      data[var10 + 3] = (byte) (nMode & 1);
      data[var10 + 4] = (byte) (nBytesPerLine % 256);
      data[var10 + 5] = (byte) (nBytesPerLine / 256);
      data[var10 + 6] = 1;
      data[var10 + 7] = 0;

      for (int j = 0; j < nBytesPerLine; ++j) {
        data[var10 + 8 + j] = (byte) (p0[src[k]] + p1[src[k + 1]] + p2[src[k + 2]] + p3[src[k + 3]] + p4[src[k + 4]] + p5[src[k + 5]] + p6[src[k + 6]] + src[k + 7]);
        k += 8;
      }
    }

    return data;
  }

}
