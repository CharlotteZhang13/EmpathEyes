package com.example.opencv;

import android.app.Application;

import cn.leancloud.LCObject;
import cn.leancloud.LeanCloud;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LeanCloud.initialize(this, "nMKlsJWMeXPRTzUXRAtfRA24-gzGzoHsz", "jHVnjnAuhQ52D1BD6QLbKmaH", "https://nmklsjwm.lc-cn-n1-shared.com");

//        LCObject testObject = new LCObject("TestObject");
//        testObject.put("words", "Hello world!");
//        testObject.saveInBackground().blockingSubscribe();
    }
}
