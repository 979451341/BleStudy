package com.yzm.myapplication;

import android.app.Application;

/**
 * Created by Autil On 2020/4/1
 */
public class App extends Application {

    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }


    public static App getInstance(){
        return app;
    }
}
