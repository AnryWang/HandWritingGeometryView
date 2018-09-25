package com.hand.writing.listener;

import android.view.ScaleGestureDetector;

public interface OnGestureListener {

    void onDrag(float dx, float dy);

    void onFling(float startX, float startY, float velocityX, float velocityY);

    boolean onScaleBegin(ScaleGestureDetector detector);

    boolean onScale(ScaleGestureDetector detector);

    void onScaleEnd(ScaleGestureDetector detector);

    void release();
}