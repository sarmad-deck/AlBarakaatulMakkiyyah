package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Shows one PDF page per row. Pages are rendered on {@code renderExecutor}, which
 * must be single-threaded because PdfRenderer allows only one open page at a time.
 */
public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageHolder> {

    private static final int MAX_RENDER_WIDTH = 2048;
    private static final Object PAYLOAD_RERENDER = new Object();

    private final PdfRenderer renderer;
    private final float[] pageAspectRatios;
    private final ExecutorService renderExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private RecyclerView recyclerView;
    private float renderScale = 1f;
    private volatile boolean closed;

    public PdfPageAdapter(PdfRenderer renderer, float[] pageAspectRatios, ExecutorService renderExecutor) {
        this.renderer = renderer;
        this.pageAspectRatios = pageAspectRatios;
        this.renderExecutor = renderExecutor;
    }

    public void setRenderScale(float scale) {
        if (scale == renderScale) {
            return;
        }
        renderScale = scale;
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_RERENDER);
    }

    /** Marks the adapter closed; pending renders are skipped. */
    public void close() {
        closed = true;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setBackgroundColor(Color.WHITE);
        imageView.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new PageHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.imageView.setImageBitmap(null);
        bind(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_RERENDER)) {
            bind(holder, position);
        } else {
            onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return pageAspectRatios.length;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = null;
    }

    @Override
    public void onViewRecycled(@NonNull PageHolder holder) {
        holder.renderToken++;
        holder.imageView.setImageBitmap(null);
    }

    private void bind(PageHolder holder, int position) {
        ImageView imageView = holder.imageView;
        int width = recyclerView != null ? recyclerView.getWidth() : 0;
        if (width <= 0) {
            width = imageView.getResources().getDisplayMetrics().widthPixels;
        }
        int height = Math.round(width * pageAspectRatios[position]);
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            imageView.setLayoutParams(params);
        }

        int renderWidth = Math.min(Math.round(width * renderScale), MAX_RENDER_WIDTH);
        int renderHeight = Math.round(renderWidth * pageAspectRatios[position]);
        final int token = ++holder.renderToken;
        final float expectedRatio = pageAspectRatios[position];
        renderExecutor.execute(() -> {
            if (closed || holder.renderToken != token) {
                return;
            }
            try (PdfRenderer.Page page = renderer.openPage(position)) {
                float actualRatio = (float) page.getHeight() / page.getWidth();
                if (Math.abs(actualRatio - expectedRatio) > 0.001f) {
                    // Page sizes are guessed from the first page; fix this row and re-bind.
                    mainHandler.post(() -> {
                        pageAspectRatios[position] = actualRatio;
                        notifyItemChanged(position);
                    });
                    return;
                }
                Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                mainHandler.post(() -> {
                    if (!closed && holder.renderToken == token) {
                        holder.imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (IllegalStateException e) {
                // Renderer was closed while this page was queued.
            }
        });
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        volatile int renderToken;

        PageHolder(@NonNull ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }
}
