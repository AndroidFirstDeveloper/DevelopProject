package com.example.develop.slide_slip;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 自定义RV，可以防止多值触摸时多个SlideSlipView同时展开
 */
public class TouchRecyclerView extends RecyclerView {

    private final String TAG = TouchRecyclerView.this.getClass().getSimpleName();
    private boolean invalidTouch = false;

    public TouchRecyclerView(Context context) {
        super(context);
    }

    public TouchRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
//        final int actionIndex = event.getActionIndex();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                invalidTouch = true;
                break;
        }
        if (invalidTouch) {
            invalidTouch = false;
            return false;
        }
        return super.dispatchTouchEvent(event);
    }
}
