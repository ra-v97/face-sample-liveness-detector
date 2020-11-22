package agh.edu.pl.facelivenessdetection.model;

import android.graphics.Bitmap;

public interface MobileModel {

    /*
     * This method lets you invoke from any thread a context change on UI that will indicate that
     * face shown on picture is fake
     */
    public void setFake();

    /*
     * This method lets you invoke from any thread a context change on UI that will indicate that
     * face shown on picture is fake
     */
    public void setReal();

    public Bitmap getPhoto();
}
