package agh.edu.pl.facelivenessdetection.model.method1;

import android.graphics.Bitmap;

import agh.edu.pl.facelivenessdetection.model.MobileModel;

public class Method extends Thread {

    MobileModel mobileModel;

    public Method(MobileModel mobileModel) {
        this.mobileModel = mobileModel;
    }

    @Override
    public void run() {
        Bitmap m = mobileModel.getPhoto();
        System.out.println(m.getColor(0, 0));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mobileModel.setReal();
    }
}
