package com.divyagyan.courierapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;


public class SplashActivity extends AppCompatActivity {
    private TextView splashScreenTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashScreenTextView = findViewById(R.id.splashScreenTextView);
        splashScreenTextView.animate().translationX(1000).setDuration(1000).setStartDelay(2500);

        Thread thread  = new Thread(){
            public void run(){
                try {
                    Thread.sleep(4000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    Intent intent = new Intent(SplashActivity.this,WelcomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        thread.start();

    }
}