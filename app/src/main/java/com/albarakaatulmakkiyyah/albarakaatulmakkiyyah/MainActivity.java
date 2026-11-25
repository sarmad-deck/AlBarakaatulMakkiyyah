package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PDF_ASSET = "content.pdf";
    private static final String PREFS = "reader_prefs";
    private static final String KEY_BOOKMARK_PAGE = "bookmark_page";
    /** First PDF pages are covers/front matter with no printed page numbers. */
    private static final int FRONT_MATTER_PAGES = 5;

    private DrawerLayout drawerLayout;
    private View mainContent;
    private View navHeader;
    private ZoomableRecyclerView pdfView;
    private ProgressBar progress;
    private ActionBarDrawerToggle drawerToggle;
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private PdfRenderer renderer;
    private PdfPageAdapter adapter;
    private int pageCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OrientationLock.apply(this);
        setContentView(R.layout.activity_main);
        initViews();
        setupDrawer();
        applyWindowInsets();
        loadPdf();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.close();
        }
        renderExecutor.execute(() -> {
            if (renderer != null) {
                renderer.close();
                renderer = null;
            }
        });
        renderExecutor.shutdown();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void loadPdf() {
        renderExecutor.execute(() -> {
            try {
                File file = copyAssetToCache(PDF_ASSET);
                PdfRenderer pdfRenderer = new PdfRenderer(
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
                float[] aspectRatios = new float[pdfRenderer.getPageCount()];
                if (aspectRatios.length > 0) {
                    try (PdfRenderer.Page page = pdfRenderer.openPage(0)) {
                        Arrays.fill(aspectRatios, (float) page.getHeight() / page.getWidth());
                    }
                }
                runOnUiThread(() -> {
                    if (isDestroyed()) {
                        renderExecutor.execute(pdfRenderer::close);
                        return;
                    }
                    renderer = pdfRenderer;
                    pageCount = aspectRatios.length;
                    adapter = new PdfPageAdapter(pdfRenderer, aspectRatios, renderExecutor);
                    pdfView.setAdapter(adapter);
                    progress.setVisibility(View.GONE);
                    restoreBookmarkIfNeeded();
                });
            } catch (IOException e) {
                Log.e(TAG, "Unable to open " + PDF_ASSET, e);
            }
        });
    }

    private File copyAssetToCache(String assetName) throws IOException {
        File file = new File(getCacheDir(), assetName);
        long installedAt;
        try {
            installedAt = getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            installedAt = Long.MAX_VALUE;
        }
        if (file.exists() && file.length() > 0 && file.lastModified() >= installedAt) {
            return file;
        }
        File temp = new File(getCacheDir(), assetName + ".tmp");
        try (InputStream in = getAssets().open(assetName);
             OutputStream out = new FileOutputStream(temp)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        if (!temp.renameTo(file)) {
            throw new IOException("Unable to move " + temp + " to " + file);
        }
        return file;
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        mainContent = findViewById(R.id.main_content);
        navHeader = findViewById(R.id.nav_header);
        pdfView = findViewById(R.id.pdfView);
        progress = findViewById(R.id.progress);
        pdfView.setLayoutManager(new LinearLayoutManager(this));
        pdfView.setOnZoomChangedListener(scale -> {
            if (adapter != null) {
                adapter.setRenderScale(scale);
            }
        });

        ImageView headerIcon = findViewById(R.id.nav_header_icon);
        headerIcon.setClipToOutline(true);
        headerIcon.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });

        findViewById(R.id.nav_go_to).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showGoToDialog();
        });
        findViewById(R.id.nav_setting).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showSettingsDialog();
        });
        findViewById(R.id.nav_save).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            saveBookmark();
        });
        findViewById(R.id.nav_about).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, AboutActivity.class));
        });
    }

    private void setupDrawer() {
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.nav_open_drawer,
                R.string.nav_close_drawer);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            mainContent.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            navHeader.setPadding(
                    navHeader.getPaddingLeft(),
                    bars.top + dp(28),
                    navHeader.getPaddingRight(),
                    navHeader.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_settings);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        SwitchCompat portrait = dialog.findViewById(R.id.switch_portrait);
        SwitchCompat landscape = dialog.findViewById(R.id.switch_landscape);
        String mode = OrientationLock.get(this);
        portrait.setChecked(OrientationLock.PORTRAIT.equals(mode));
        landscape.setChecked(OrientationLock.LANDSCAPE.equals(mode));

        portrait.setOnCheckedChangeListener((button, checked) -> {
            if (!button.isPressed()) {
                return;
            }
            if (checked) {
                landscape.setChecked(false);
                OrientationLock.set(this, OrientationLock.PORTRAIT);
            } else if (!landscape.isChecked()) {
                OrientationLock.set(this, OrientationLock.NONE);
            }
        });
        landscape.setOnCheckedChangeListener((button, checked) -> {
            if (!button.isPressed()) {
                return;
            }
            if (checked) {
                portrait.setChecked(false);
                OrientationLock.set(this, OrientationLock.LANDSCAPE);
            } else if (!portrait.isChecked()) {
                OrientationLock.set(this, OrientationLock.NONE);
            }
        });
        dialog.show();
    }

    private void showGoToDialog() {
        if (indexedPageCount() <= 0) {
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_go_to);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText pageInput = dialog.findViewById(R.id.go_to_page_input);
        dialog.findViewById(R.id.go_to_page_button).setOnClickListener(v -> {
            if (goToBookPage(pageInput.getText().toString())) {
                dialog.dismiss();
            }
        });

        View.OnClickListener jump = v -> {
            Object tag = v.getTag();
            if (tag != null && goToBookPage(tag.toString())) {
                dialog.dismiss();
            }
        };
        int[] jumps = {
                R.id.manzil_1, R.id.manzil_2, R.id.manzil_3, R.id.manzil_4,
                R.id.manzil_5, R.id.manzil_6, R.id.manzil_7,
                R.id.sulas_1, R.id.sulas_2, R.id.sulas_3, R.id.all_in_one
        };
        for (int id : jumps) {
            dialog.findViewById(id).setOnClickListener(jump);
        }
        dialog.show();
    }

    private boolean goToBookPage(String rawPage) {
        int lastBookPage = indexedPageCount();
        try {
            int bookPage = Integer.parseInt(rawPage.trim());
            if (bookPage < 1 || bookPage > lastBookPage) {
                Toast.makeText(this, getString(R.string.go_to_invalid, lastBookPage),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            scrollToPdfIndex(pdfIndexForBookPage(bookPage));
            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.go_to_invalid, lastBookPage),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void saveBookmark() {
        int bookPage = bookPageForPdfIndex(currentPageIndex());
        if (bookPage < 1) {
            Toast.makeText(this, R.string.save_unnumbered_page, Toast.LENGTH_SHORT).show();
            return;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_BOOKMARK_PAGE, bookPage)
                .apply();
        Toast.makeText(this, getString(R.string.save_bookmark, bookPage), Toast.LENGTH_SHORT).show();
    }

    private void restoreBookmarkIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int bookPage = prefs.getInt(KEY_BOOKMARK_PAGE, -1);
        if (bookPage >= 1 && bookPage <= indexedPageCount()) {
            pdfView.post(() -> scrollToPdfIndex(pdfIndexForBookPage(bookPage)));
        }
    }

    private void scrollToPdfIndex(int index) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) pdfView.getLayoutManager();
        if (layoutManager == null) {
            pdfView.scrollToPosition(index);
            return;
        }
        layoutManager.scrollToPositionWithOffset(index, 0);
    }

    private int currentPageIndex() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) pdfView.getLayoutManager();
        if (layoutManager == null) {
            return 0;
        }
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first < 0) {
            return 0;
        }
        if (last < first) {
            last = first;
        }
        int best = first;
        int bestVisible = -1;
        int viewportBottom = pdfView.getHeight();
        for (int i = first; i <= last; i++) {
            View child = layoutManager.findViewByPosition(i);
            if (child == null) {
                continue;
            }
            int top = Math.max(child.getTop(), 0);
            int bottom = Math.min(child.getBottom(), viewportBottom);
            int visible = bottom - top;
            if (visible > bestVisible) {
                bestVisible = visible;
                best = i;
            }
        }
        return best;
    }

    private int indexedPageCount() {
        return Math.max(0, pageCount - FRONT_MATTER_PAGES);
    }

    private int pdfIndexForBookPage(int bookPage) {
        return bookPage - 1 + FRONT_MATTER_PAGES;
    }

    private int bookPageForPdfIndex(int pdfIndex) {
        return pdfIndex - FRONT_MATTER_PAGES + 1;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
