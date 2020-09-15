package com.example.myproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

//어플리케이션 처음 시작화면
public class SplashActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        try{
            Thread.sleep(3000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }
}
