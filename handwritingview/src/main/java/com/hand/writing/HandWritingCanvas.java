package com.hand.writing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Random;

import static com.hand.writing.HandWritingViewHelper.DEBUG;

public class HandWritingCanvas {
    private String TAG = "HandWritingCanvas";
    private Random random = new Random();
    private RectF dirtyRect = new RectF();
    private int tileWidth = 200;
    public int width;
    public int height;
    private int tileHorizontalCount;// 水平方向小块个数
    private int tileVerticalCount;// 垂直方向小块个数
    private Tile[] tiles;

    private class Tile {
        Bitmap bitmap;
        Canvas canvas;
        Rect rect;
        int index; //从水平方向往右开始的索引
        int horizontalIndex; //水平索引
        int verticalIndex; //竖直索引

        Tile(Bitmap bitmap, int horizontalIndex, int verticalIndex) {
            this.bitmap = bitmap;
            this.canvas = new Canvas(bitmap);
            canvas.translate(-horizontalIndex * tileWidth, -verticalIndex * tileWidth);
            //抗锯齿 等效于Paint
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG));
            this.horizontalIndex = horizontalIndex;
            this.verticalIndex = verticalIndex;
            int left = horizontalIndex * tileWidth;
            int top = verticalIndex * tileWidth;
            rect = new Rect(left, top, left + tileWidth, top + tileWidth);
            index = verticalIndex * tileHorizontalCount + horizontalIndex;
        }
    }

    public int randomColor() {
        return Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }

    public HandWritingCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        tileHorizontalCount = width / tileWidth + (width % tileWidth == 0 ? 0 : 1);
        tileVerticalCount = height / tileWidth + (height % tileWidth == 0 ? 0 : 1);

        if (DEBUG) {
            Log.i(TAG, "width = " + width + ", height = " + height +
                    ", tileHorizontalCount = " + tileHorizontalCount +
                    ", tileVerticalCount = " + tileVerticalCount);
        }

        tiles = new Tile[tileHorizontalCount * tileVerticalCount];
        int i = 0;
        for (int horizontalIndex = 0; horizontalIndex < tileHorizontalCount; horizontalIndex++) {
            for (int verticalIndex = 0; verticalIndex < tileVerticalCount; verticalIndex++) {
                Bitmap bitmap = Bitmap.createBitmap(tileWidth, tileWidth, Bitmap.Config.ARGB_4444);
                Tile tile = new Tile(bitmap, horizontalIndex, verticalIndex);
                tiles[i] = tile;
                i++;
            }
        }
    }

    public void drawPath(Path path, Paint paint, float penSize) {
        paint.setStrokeWidth(penSize);
        drawPath(path, paint);
    }

    public void drawPath(@NonNull Path path, @NonNull Paint paint) {
        path.computeBounds(dirtyRect, false);
        for (Tile tile : tiles) {
            boolean inside = tile.rect.intersects((int) dirtyRect.left, (int) dirtyRect.top, (int) dirtyRect.right, (int) dirtyRect.bottom);
            if (inside) {
                drawPath(path, tile.canvas, paint);
            }
        }
    }

    private void drawPath(Path path, Canvas canvas, Paint paint) {
        canvas.drawPath(path, paint);
    }

    public void drawLine(float startX, float startY, float endX, float endY, Paint paint) {
        dirtyRect.left = startX > endX ? endX : startX;
        dirtyRect.right = startX < endX ? endX : startX;
        dirtyRect.top = startY > endY ? endY : startY;
        dirtyRect.bottom = startY < endY ? endY : startY;
        for (Tile tile : tiles) {
            boolean inside = tile.rect.intersects((int) dirtyRect.left, (int) dirtyRect.top, (int) dirtyRect.right, (int) dirtyRect.bottom);
            if (inside) {
                tile.canvas.drawLine(startX, startY, endX, endY, paint);
            }
        }
    }

    public void drawCanvas(Canvas canvas, Paint paint) {
        for (Tile tile : tiles) {
            canvas.drawBitmap(tile.bitmap, tile.rect.left, tile.rect.top, paint);
        }
    }

    public void drawRect(Rect rect, Paint paint) {
        for (Tile tile : tiles) {
            boolean inside = tile.rect.intersects(rect.left, rect.top, rect.right, rect.bottom);
            if (inside) {
                tile.canvas.drawRect(rect, paint);
            }
        }
    }

    public void clearCanvas() {
        for (Tile tile : tiles) {
            tile.bitmap.eraseColor(Color.TRANSPARENT);
        }
    }

    public void releaseCanvas() {
        for (Tile tile : tiles) {
            tile.bitmap.recycle();
        }
    }

    public void drawBitmap(Bitmap bitmap, Paint paint) {
        for (Tile tile : tiles) {
            if (bitmap.getWidth() > tile.rect.left && bitmap.getHeight() > tile.rect.top) {
                tile.canvas.drawBitmap(bitmap, tile.rect, tile.rect, paint);
            }
        }
    }
}
