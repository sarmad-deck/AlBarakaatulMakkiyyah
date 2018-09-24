package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        setActionBar();
    }

    private void setActionBar() {
        getSupportActionBar().hide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initViews();
        setListener();
    }

    private void initViews() {
        btnEnter = findViewById(R.id.dummy_button);
    }

    private void setListener() {
        btnEnter.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }
}
