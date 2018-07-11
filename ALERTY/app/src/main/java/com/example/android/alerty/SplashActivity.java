package com.example.android.alerty;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Locale;

import static com.example.android.alerty.MapsActivity.myTTS;

public class SplashActivity extends AppCompatActivity {
    private static int SPLASH_TIME_OUT = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logo);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#43baee")));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Set up the Text To Speech engine.
        TextToSpeech.OnInitListener listener =
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(final int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            myTTS.setLanguage(Locale.US);
                        } else {
                            Toast.makeText(getApplicationContext(),"Error to load voice alerts",Toast.LENGTH_LONG);
                        }
                    }
                };
        myTTS = new TextToSpeech(this.getApplicationContext(), listener);


        new Handler().postDelayed(new Runnable() {


            @Override
            public void run() {

                Intent i = new Intent(SplashActivity.this,SignInActivity.class);
                startActivity(i);

                finish();
            }
        }, SPLASH_TIME_OUT);
    }


}
