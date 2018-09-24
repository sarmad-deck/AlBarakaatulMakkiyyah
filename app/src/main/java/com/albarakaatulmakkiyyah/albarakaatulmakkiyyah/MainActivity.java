package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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

    private void loadPdf() {
        pdfView.fromAsset("content.pdf")
                .spacing(0)
                .load();
    }

    private void initViews() {
        pdfView = findViewById(R.id.pdfView);
    }
}
