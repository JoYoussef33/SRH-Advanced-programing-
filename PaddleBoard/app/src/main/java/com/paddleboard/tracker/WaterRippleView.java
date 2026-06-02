package com.paddleboard.tracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class WaterRippleView extends View {

    private static final int NUM_RINGS    = 4;
    private static final float WARP_BASE  = 0.075f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  path  = new Path();

    private long  birthTime     = System.currentTimeMillis();
    private long  rippleDuration = 2600; // ms for one ring to fully expand
    private int   rippleColor   = 0xFF00D4FF;

    private ValueAnimator ticker;

    public WaterRippleView(Context ctx)                { super(ctx);    init(); }
    public WaterRippleView(Context ctx, AttributeSet a){ super(ctx, a); init(); }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        // Fire ~60 fps invalidates
        ticker = ValueAnimator.ofFloat(0f, 1f);
        ticker.setDuration(16);
        ticker.setRepeatCount(ValueAnimator.INFINITE);
        ticker.setInterpolator(new LinearInterpolator());
        ticker.addUpdateListener(a -> invalidate());
        ticker.start();
    }

    /** Call with true when a session is active (ripples speed up). */
    public void setActive(boolean active) {
        rippleDuration = active ? 1300 : 2600;
        birthTime = System.currentTimeMillis(); // reset so colour/speed takes effect cleanly
    }

    public void setRippleColor(int color) {
        rippleColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f, cy = h / 2f;
        float maxR = Math.min(cx, cy) * 0.90f;
        long elapsed = System.currentTimeMillis() - birthTime;

        for (int i = 0; i < NUM_RINGS; i++) {
            // Stagger each ring's birth time evenly within one cycle
            long stagger = (long)(i * rippleDuration / (float) NUM_RINGS);
            if (elapsed < stagger) continue; // ring not yet born

            long age = (elapsed - stagger) % rippleDuration;
            float progress = age / (float) rippleDuration; // 0 → 1

            float radius  = progress * maxR;
            // Alpha: fast-appear, slow-fade (quadratic curve)
            float alpha   = (float) Math.pow(1f - progress, 1.4f);
            float strokeW = (1f - progress) * 7f + 1.5f;

            int r = Color.red(rippleColor);
            int g = Color.green(rippleColor);
            int b = Color.blue(rippleColor);
            paint.setColor(Color.argb((int)(alpha * 210), r, g, b));
            paint.setStrokeWidth(strokeW);

            // Phase drives the organic shape; drifts slowly over real time
            float globalSec  = elapsed / 1000f;
            float phase      = globalSec * 0.9f + i * 1.618f; // golden-ratio offset per ring
            // Rings are more warped close to centre, smoother as they expand
            float warpFactor = 1f - progress * 0.55f;

            drawOrganicRing(canvas, cx, cy, radius, phase, warpFactor);
        }
    }

    /**
     * Draws a single ring whose radius varies organically around the circle —
     * overlapping sine waves at different frequencies and phases produce a
     * non-uniform, water-drop-like outline that evolves over time.
     */
    private void drawOrganicRing(Canvas canvas, float cx, float cy,
                                  float r, float phase, float warp) {
        if (r < 2f) return;
        path.reset();
        final int pts = 72; // more points → smoother curve
        for (int i = 0; i <= pts; i++) {
            double a = 2 * Math.PI * i / pts;
            // Four overlapping harmonics for a complex, organic wobble
            float rad = r * (1f
                + warp * WARP_BASE        * (float) Math.sin(2.7  * a + phase)
                + warp * WARP_BASE * 0.7f * (float) Math.cos(4.1  * a + phase * 1.35f)
                + warp * WARP_BASE * 0.5f * (float) Math.sin(5.9  * a - phase * 0.85f)
                + warp * WARP_BASE * 0.35f* (float) Math.cos(9.1  * a + phase * 1.75f)
                + warp * WARP_BASE * 0.2f * (float) Math.sin(13.3 * a - phase * 0.5f));
            float x = cx + rad * (float) Math.cos(a);
            float y = cy + rad * (float) Math.sin(a);
            if (i == 0) path.moveTo(x, y);
            else        path.lineTo(x, y);
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (ticker != null) ticker.cancel();
        super.onDetachedFromWindow();
    }
}
