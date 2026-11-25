package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OrientationLock.apply(this);
        setContentView(R.layout.activity_about);
        SystemBarInsets.applyToContent(this);
        button = findViewById(R.id.email_button);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.email_button) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto","Ans14e@gmail.com", null));
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
    }
}
