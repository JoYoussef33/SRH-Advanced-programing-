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

public class WaveView extends View {

    private final Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float phase = 0f;
    private ValueAnimator animator;

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint1.setColor(Color.parseColor("#1A00D4FF"));
        paint1.setStyle(Paint.Style.FILL);
        paint2.setColor(Color.parseColor("#1400FFD4"));
        paint2.setStyle(Paint.Style.FILL);
        paint3.setColor(Color.parseColor("#0C0D2137"));
        paint3.setStyle(Paint.Style.FILL);

        animator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        animator.setDuration(5000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(anim -> {
            phase = (float) anim.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        drawWave(canvas, paint3, 0.20f, phase + 2.0f, 0.18f, w, h);
        drawWave(canvas, paint2, 0.38f, phase + 1.0f, 0.14f, w, h);
        drawWave(canvas, paint1, 0.52f, phase,        0.11f, w, h);
    }

    private void drawWave(Canvas canvas, Paint paint, float yRatio,
                          float ph, float ampRatio, int w, int h) {
        path.reset();
        path.moveTo(0, h * yRatio);
        for (int x = 0; x <= w; x += 4) {
            double angle = ph + x * 2.5 * Math.PI / w;
            float y = (float) (h * yRatio + Math.sin(angle) * h * ampRatio);
            path.lineTo(x, y);
        }
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
