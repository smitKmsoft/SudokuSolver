package com.sportbvet.game.myapplication;

import android.app.Application;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (!OpenCVLoader.initLocal()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initLocal();
        }
    }
}
