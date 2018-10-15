package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.github.barteksc.pdfviewer.PDFView;

public class MainActivity extends AppCompatActivity {

    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        loadPdf();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        return true;
    }

    private void loadPdf() {
        pdfView.fromAsset("content.pdf")
                .spacing(0)
                .load();
    }

    private void initViews() {
        pdfView = findViewById(R.id.pdfView);
    }
}
