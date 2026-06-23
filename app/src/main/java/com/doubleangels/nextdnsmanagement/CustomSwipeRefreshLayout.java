package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * SwipeRefreshLayout that does not intercept clearly horizontal gestures,
 * so horizontal tab scrolling inside the WebView is not interrupted.
 */
public class CustomSwipeRefreshLayout extends SwipeRefreshLayout {

    private final int touchSlop;
    private float startX;
    private float startY;
    private boolean horizontalScroll;

    public CustomSwipeRefreshLayout(@NonNull Context context) {
        this(context, null);
    }

    public CustomSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                horizontalScroll = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);
                if (dx > touchSlop && dx > dy) {
                    horizontalScroll = true;
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                horizontalScroll = false;
                break;
            default:
                break;
        }
        if (horizontalScroll) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
