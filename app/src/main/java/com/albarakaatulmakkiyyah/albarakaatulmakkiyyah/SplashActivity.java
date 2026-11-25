package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.annotation.SuppressLint;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
        OrientationLock.apply(this);
        setContentView(R.layout.activity_splash);
        SystemBarInsets.applyToContent(this);
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
