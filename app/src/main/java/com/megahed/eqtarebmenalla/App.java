package com.megahed.eqtarebmenalla;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class App extends Application implements Configuration.Provider {

    private static App mInstance;

    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static App getInstance() {
        return mInstance;
    }


    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
