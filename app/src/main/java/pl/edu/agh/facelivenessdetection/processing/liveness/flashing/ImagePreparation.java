package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImagePreparation {

    public Mat applyGrayFilter(Mat mat) {
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);
        return mat;
    }

    public Bitmap applyGrayFilter(Bitmap bitmap) {
        Bitmap bitmapGrayScale = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmapGrayScale);
        Paint paint = new Paint();

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmapGrayScale;
    }

    public Mat resize(Mat mat, int width, int height) {
        Mat resizedMat = new Mat();
        Imgproc.resize(mat, resizedMat, new Size(width, height));
        return resizedMat;
    }

    public Bitmap resize(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public Mat crop(Mat mat, int startX, int startY, int width, int height) {
        Rect rect = new Rect(startX, startY, width, height);
        return new Mat(mat, rect);
    }

    public Bitmap crop(Bitmap bitmap, int startX, int startY, int width, int height) {
        return Bitmap.createBitmap(bitmap, startX, startY, width, height);
    }

    public Mat applyGaussianBlur(Mat mat, int sigma) {
        Imgproc.GaussianBlur(mat, mat, new Size(0, 0), sigma);
        return mat;
    }

    public Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmapCopy, mat);
        return mat;
    }

    public Bitmap mapToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

}
