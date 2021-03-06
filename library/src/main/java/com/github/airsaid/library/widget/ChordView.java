/*
 * Copyright 2018 Airsaid. https://github.com/airsaid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.airsaid.library.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.annotation.*;
import android.util.AttributeSet;
import android.view.View;
import com.github.airsaid.library.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * 用于渲染吉他和铉的 Android 自定义 View。
 *
 * @author airsaid
 */
public class ChordView extends View {

    @IntDef({NORMAL_SHOW_MODE, SIMPLE_SHOW_MODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShowMode {}

    /** 弦数 */
    private static final int STRING = 6;
    /** 品数 */
    private static final int FRET = 4;
    /** 默认显示模式 */
    public static final int NORMAL_SHOW_MODE = 1;
    /** 简单显示模式，默认只显示三品 */
    public static final int SIMPLE_SHOW_MODE = 2;

    /** 显示模式 */
    @ShowMode private int mShowMode;

    /** 表示闭弦符号的图片，如果为 NULL，则不会绘制闭弦提示符号 */
    private Bitmap mClosedStringBitmap;
    /** 表示空弦符号的图片，如果为 NULL，则不会绘制空弦提示符号 */
    private Bitmap mEmptyStringBitmap;
    /** 空弦、闭弦提示符号的 y 轴偏移量 */
    private float mStringOffsetY;

    /** 琴头弧度 */
    private float mHeadRadius;
    /** 琴头颜色 */
    private int mHeadColor;

    /** 品文字大小 */
    private float mFretTextSize;
    /** 品文字颜色 */
    private int mFretTextColor;
    /** 品文字 x 轴偏移量 */
    private float mFretTextOffsetX;

    /** 网格线的宽度 */
    private float mGridLineWidth;
    /** 网格线的颜色 */
    private int mGridLineColor;

    /** 节点颜色 */
    private int mNoteColor;
    /** 节点圆的半径 */
    private float mNoteRadius;
    /** 节点文字大小 */
    private float mNoteTextSize;
    /** 节点文字颜色 */
    private int mNoteTextColor;
    /** 节点边框 */
    private float mNoteStrokeWidth;
    /** 节点边框颜色 */
    private int mNoteStrokeColor;
    /** 节点透明度 */
    private int mNoteAlpha;

    /** 横按区域颜色 */
    private int mBarreColor;
    /** 横按区域透明度 */
    private int mBarreAlpha;
    /** 横按区域边框 */
    private float mBarreStrokeWidth;
    /** 横按区域边框颜色 */
    private int mBarreStrokeColor;

    private Chord mChord;
    private Paint mPaint;
    private ChordHelper mChordHelper;
    private Path mHeadPath = new Path();

    public ChordView(Context context) {
        this(context, null);
    }

    public ChordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        mChordHelper = new ChordHelper();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChordView);
        setShowMode(a.getInt(R.styleable.ChordView_cv_showMode, NORMAL_SHOW_MODE));
        setClosedStringImage(a.getResourceId(R.styleable.ChordView_cv_closedStringImage, 0));
        setEmptyStringImage(a.getResourceId(R.styleable.ChordView_cv_emptyStringImage, 0));
        setStringOffsetY(a.getDimension(R.styleable.ChordView_cv_stringOffsetY, 0f));
        setHeadRadius(a.getDimension(R.styleable.ChordView_cv_headRadius, 0f));
        setHeadColor(a.getColor(R.styleable.ChordView_cv_headColor, Color.WHITE));
        setFretTextSize(a.getDimension(R.styleable.ChordView_cv_fretTextSize, 40f));
        setFretTextColor(a.getColor(R.styleable.ChordView_cv_fretTextColor, Color.WHITE));
        setFretTextOffsetX(a.getDimension(R.styleable.ChordView_cv_fretTextOffsetX, 0f));
        setGridLineWidth(a.getDimension(R.styleable.ChordView_cv_gridLineWidth, 10f));
        setGridLineColor(a.getColor(R.styleable.ChordView_cv_gridLineColor, Color.WHITE));
        setNoteColor(a.getColor(R.styleable.ChordView_cv_noteColor, Color.WHITE));
        setNoteRadius(a.getDimension(R.styleable.ChordView_cv_noteRadius, 40f));
        setNoteTextSize(a.getDimension(R.styleable.ChordView_cv_noteTextSize, 40f));
        setNoteTextColor(a.getColor(R.styleable.ChordView_cv_noteTextColor, Color.BLACK));
        setNoteStrokeWidth(a.getDimension(R.styleable.ChordView_cv_noteStrokeWidth, 0f));
        setNoteStrokeColor(a.getColor(R.styleable.ChordView_cv_noteStrokeColor, Color.WHITE));
        setNoteAlpha(a.getInt(R.styleable.ChordView_cv_noteAlpha, 255));
        setBarreColor(a.getColor(R.styleable.ChordView_cv_barreColor, Color.WHITE));
        setBarreAlpha(a.getInt(R.styleable.ChordView_cv_barreAlpha, 255));
        setBarreStrokeWidth(a.getDimension(R.styleable.ChordView_cv_barreStrokeWidth, 0f));
        setBarreStrokeColor(a.getColor(R.styleable.ChordView_cv_barreStrokeColor, Color.WHITE));
        a.recycle();
    }

    /**
     * 设置和弦对象并根据该和弦开始渲染。
     *
     * @param chord 和弦对象
     */
    public void setChord(Chord chord) {
        mChord = chord;
        invalidate();
    }

    /**
     * 获取当前和弦对象。
     *
     * @return 和弦对象。
     */
    public Chord getChord() {
        return mChord;
    }

    /**
     * 设置显示模式。可设置的参数有默认的 {@link #NORMAL_SHOW_MODE} 和 {@link #SIMPLE_SHOW_MODE}。
     *
     * @param mode 显示模式
     */
    public void setShowMode(@ShowMode int mode) {
        mShowMode = mode;
        invalidate();
    }

    /**
     * 获取显示模式。
     *
     * @return 显示模式。
     */
    @ShowMode public int getShowMode() {
        return mShowMode;
    }

    /**
     * 通过图片资源 id 设置闭弦符号的图片。
     *
     * @param resId 闭弦符号图片资源 id
     */
    public void setClosedStringImage(@IdRes int resId) {
        if (resId == 0) return;
        setClosedStringBitmap(BitmapFactory.decodeResource(getResources(), resId));
    }

    /**
     * 设置闭弦符号的图片。
     *
     * @param bitmap Bitmap 图片
     */
    public void setClosedStringBitmap(Bitmap bitmap) {
        mClosedStringBitmap = bitmap;
    }

    /**
     * 获取闭弦符号的图片。
     *
     * @return 闭弦 Bitmap 图片。
     */
    public Bitmap getClosedStringBitmap() {
        return mClosedStringBitmap;
    }

    /**
     * 通过图片资源 id 设置空弦符号的图片。
     *
     * @param resId 空弦符号图片资源 id
     */
    public void setEmptyStringImage(@IdRes int resId) {
        if (resId == 0) return;
        setEmptyStringBitmap(BitmapFactory.decodeResource(getResources(), resId));
    }

    /**
     * 设置空弦符号的图片。
     *
     * @param bitmap Bitmap 图片
     */
    public void setEmptyStringBitmap(Bitmap bitmap) {
        mEmptyStringBitmap = bitmap;
    }

    /**
     * 获取空弦符号的图片。
     *
     * @return 空弦 Bitmap 图片。
     */
    public Bitmap getEmptyStringBitmap() {
        return mEmptyStringBitmap;
    }

    /**
     * 设置空弦、闭弦提示符号的 y 轴偏移量
     *
     * @param offsetY y 轴偏移量
     */
    public void setStringOffsetY(float offsetY) {
        mStringOffsetY = offsetY;
    }

    /**
     * 获取空弦、闭弦提示符号的 y 轴偏移量
     *
     * @return y 轴偏移量。
     */
    public float getStringOffsetY() {
        return mStringOffsetY;
    }

    /**
     * 设置琴头的弧度。
     *
     * @param radius 弧度
     */
    public void setHeadRadius(float radius) {
        mHeadRadius = radius;
    }

    /**
     * 获取琴头的弧度。
     *
     * @return 琴头弧度。
     */
    public float getHeadRadius() {
        return mHeadRadius;
    }

    /**
     * 设置琴头的颜色。
     *
     * @param color 琴头颜色
     */
    public void setHeadColor(@ColorInt int color) {
        mHeadColor = color;
    }

    /**
     * 获取琴头的颜色。
     *
     * @return 琴头颜色。
     */
    @ColorInt
    public int getHeadColor() {
        return mHeadColor;
    }

    /**
     * 设置品文字的大小。
     *
     * @param textSize 文字大小
     */
    public void setFretTextSize(float textSize) {
        mFretTextSize = textSize;
    }

    /**
     * 获取品文字的大小。
     *
     * @return 品文字大小。
     */
    public float getFretTextSize() {
        return mFretTextSize;
    }

    /**
     * 设置品文字的颜色。
     *
     * @param textColor 文字颜色
     */
    public void setFretTextColor(int textColor) {
        mFretTextColor = textColor;
    }

    /**
     * 获取品文字的颜色。
     *
     * @return 文字颜色。
     */
    public int getFretTextColor() {
        return mFretTextColor;
    }

    /**
     * 设置品文字 x 轴偏移量。
     *
     * @param offsetX x 轴偏移量
     */
    public void setFretTextOffsetX(float offsetX) {
        mFretTextOffsetX = offsetX;
    }

    /**
     * 获取品文字 x 轴偏移量。
     *
     * @return x 轴偏移量。
     */
    public float getFretTextOffsetX() {
        return mFretTextOffsetX;
    }

    /**
     * 设置网格线的宽度。
     *
     * @param lineWidth 网格线的宽度
     */
    public void setGridLineWidth(float lineWidth) {
        mGridLineWidth = lineWidth;
    }

    /**
     * 获取网格线的宽度。
     *
     * @return 网格线的宽度。
     */
    public float getGridLineWidth() {
        return mGridLineWidth;
    }

    /**
     * 设置网格线的颜色。
     *
     * @param lineColor 网格线颜色
     */
    public void setGridLineColor(int lineColor) {
        mGridLineColor = lineColor;
    }

    /**
     * 获取网格线的颜色。
     *
     * @return 网格线颜色。
     */
    public int getGridLineColor() {
        return mGridLineColor;
    }

    /**
     * 设置节点的颜色。
     *
     * @param noteColor 节点颜色
     */
    public void setNoteColor(int noteColor) {
        mNoteColor = noteColor;
    }

    /**
     * 获取节点的颜色。
     *
     * @return 节点颜色。
     */
    public int getNoteColor() {
        return mNoteColor;
    }

    /**
     * 设置节点圆的半径。
     *
     * @param radius 节点圆半径
     */
    public void setNoteRadius(float radius) {
        mNoteRadius = radius;
    }

    /**
     * 获取节点圆的半径。
     *
     * @return 节点圆半径。
     */
    public float getNoteRadius() {
        return mNoteRadius;
    }

    /**
     * 设置节点文字的大小。
     *
     * @param textSize 节点文字大小
     */
    public void setNoteTextSize(float textSize) {
        mNoteTextSize = textSize;
    }

    /**
     * 获取节点文字的大小。
     *
     * @return 节点文字大小。
     */
    public float getNoteTextSize() {
        return mNoteTextSize;
    }

    /**
     * 设置节点文字的颜色。
     *
     * @param textColor 节点文字颜色
     */
    public void setNoteTextColor(int textColor) {
        mNoteTextColor = textColor;
    }

    /**
     * 获取节点文字的颜色。
     *
     * @return 节点文字颜色。
     */
    public int getNoteTextColor() {
        return mNoteTextColor;
    }

    /**
     * 设置节点边框的宽度。
     *
     * @param width 节点边框宽度
     */
    public void setNoteStrokeWidth(float width) {
        mNoteStrokeWidth = width;
    }

    /**
     * 获取节点边框的宽度。
     *
     * @return 节点边框宽度。
     */
    public float getNoteStrokeWidth() {
        return mNoteStrokeWidth;
    }

    /**
     * 设置节点边框的颜色。
     *
     * @param color 节点边框颜色
     */
    public void setNoteStrokeColor(int color) {
        mNoteStrokeColor = color;
    }

    /**
     * 获取节点边框的颜色。
     *
     * @return 节点边框颜色。
     */
    public int getNoteStrokeColor() {
        return mNoteStrokeColor;
    }

    /**
     * 设置节点的透明度。
     *
     * @param alpha 节点透明度
     */
    public void setNoteAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mNoteAlpha = alpha;
    }

    /**
     * 获取节点的透明度。
     *
     * @return 节点透明度。
     */
    @IntRange(from = 0, to = 255)
    public int getNoteAlpha() {
        return mNoteAlpha;
    }

    /**
     * 设置横按区域的颜色。
     *
     * @param color 横按区域颜色
     */
    public void setBarreColor(int color) {
        mBarreColor = color;
    }

    /**
     * 获取横按区域的颜色。
     *
     * @return 横按区域颜色。
     */
    public int getBarreColor() {
        return mBarreColor;
    }

    /**
     * 设置横按区域的透明度。
     *
     * @param alpha 横按区域透明度
     */
    public void setBarreAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mBarreAlpha = alpha;
    }

    /**
     * 获取横按区域的透明度。
     *
     * @return 横按区域透明度。
     */
    @IntRange(from = 0, to = 255)
    public int getBarreAlpha() {
        return mBarreAlpha;
    }

    /**
     * 设置横按区域的边框大小。
     *
     * @param width 横按区域边框大小
     */
    public void setBarreStrokeWidth(float width) {
        mBarreStrokeWidth = width;
    }

    /**
     * 获取横按区域的边框大小。
     *
     * @return 横按区域边框大小。
     */
    public float getBarreStrokeWidth() {
        return mBarreStrokeWidth;
    }

    /**
     * 设置横按区域边框的颜色。
     *
     * @param color 横按区域边框颜色
     */
    public void setBarreStrokeColor(int color) {
        mBarreStrokeColor = color;
    }

    /**
     * 获取横按区域边框的颜色。
     *
     * @return 横按区域边框颜色。
     */
    public int getBarreStrokeColor() {
        return mBarreStrokeColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawClosedEmptyString(canvas);
        drawFrets(canvas);
        drawHead(canvas);
        drawGrid(canvas);
        drawNotes(canvas);
//        drawDebug(canvas);
    }

    /**
     * 绘制闭弦和空弦。
     *
     * @param canvas 画布对象
     */
    private void drawClosedEmptyString(Canvas canvas) {
        if (!isDrawStrings()) return;

        for (int i = 0; i < STRING; i++) {
            Bitmap bitmap = getStringBitmap(i);
            if (bitmap == null) continue;
            float left = getFretWidth() - bitmap.getWidth() / 2 + (getGridColumnWidth() * i);
            float top = Math.max(bitmapHeight(mClosedStringBitmap)
                    , bitmapHeight(mEmptyStringBitmap)) / 2;
            canvas.drawBitmap(bitmap, left, top, mPaint);
        }
    }

    /**
     * 绘制品文字。
     *
     * @param canvas 画布对象
     */
    private void drawFrets(Canvas canvas) {
        // 如果最高品未超过 4 品，则不绘制
        if (!isExceedDefaultFret()) {
            return;
        }

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mFretTextSize);
        mPaint.setColor(mFretTextColor);
        mPaint.setAlpha(255);

        int index = 1;
        int leastFret = getLeastFret();
        float fretWidth = getFretWidth();

        for (int i = leastFret; i < STRING + leastFret; i++) {
            String fret = String.valueOf(i);
            float fretTextWidth = mPaint.measureText(fret);
            float x = fretWidth - fretTextWidth - mFretTextOffsetX;
            float y = getStringHeight() + getHeadHeight() + (getGridRowHeight() * index);
            canvas.drawText(fret, x, y, mPaint);
            // 简单模式下，如果和弦中的品超过三品，则就只展示第一个品数字
            if (mShowMode == SIMPLE_SHOW_MODE) {
                // 不继续绘制
                break;
            }
            index++;
        }
    }

    /**
     * 绘制琴头。
     *
     * @param canvas 画布对象
     */
    private void drawHead(Canvas canvas) {
        if (!isDrawHead()) return;

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mHeadColor);

        float width = getGridWidth();
        float x = getFretWidth();
        float y = getStringHeight();
        mHeadPath.rewind();
        mHeadPath.moveTo(x, y + mHeadRadius);
        mHeadPath.quadTo(x, y, x + mHeadRadius, y);
        mHeadPath.lineTo(x + width - mHeadRadius, y);
        mHeadPath.quadTo(x + width, y, x + width, y + mHeadRadius);
        canvas.drawPath(mHeadPath, mPaint);
    }

    /**
     * 绘制指板网格。
     *
     * @param canvas 画布对象
     */
    private void drawGrid(Canvas canvas) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mGridLineWidth);
        mPaint.setColor(mGridLineColor);

        int row = getRow();
        float width = getGridWidth(), height = getGridHeight();
        float x = getFretWidth(), y = getStringHeight() + getHeadHeight();
        // 绘制横线
        float ry = y;
        float rh = (height - mGridLineWidth * (row + 1)) / (row);
        for (int i = 0; i < row + 1; i++) {
            ry = i != 0 ? ry + rh + mGridLineWidth : ry + mGridLineWidth / 2;
            canvas.drawLine(x, ry, x + width, ry, mPaint);
        }
        // 绘制竖线
        float cw = (width - mGridLineWidth * STRING) / (STRING - 1);
        for (int i = 0; i < STRING; i++) {
            x = i != 0 ? x + cw + mGridLineWidth : x + mGridLineWidth / 2;
            canvas.drawLine(x, y, x, y + height, mPaint);
        }
    }

    /**
     * 绘制节点。
     *
     * @param canvas 画布对象
     */
    private void drawNotes(Canvas canvas) {
        if (mChord == null) return;

        int[] frets = mChord.getFrets();
        int[] fingers = mChord.getFingers();

        // 绘制横按
        int[] barreChord = mChordHelper.getBarreChordData(mChord);
        int barreFret = 0, barreString = 0;
        // 判断是否有横按情况
        if (barreChord != null) {
            // 有横按情况，绘制横按矩形
            barreFret = barreChord[0];
            barreString = barreChord[1];

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mBarreColor);
            mPaint.setAlpha(mBarreAlpha);

            float left = getFretWidth() + mGridLineWidth / 2 + (getGridColumnWidth() * (STRING - barreString));
            float top = getStringHeight() + getHeadHeight();
            if (isExceedDefaultFret()) {
                // 显示在 1 品位置
                top += getGridRowHeight() / 2 - mNoteRadius;
            } else {
                // 显示在最小品位置
                top += getGridRowHeight() * barreFret - (getGridRowHeight() / 2) - mNoteRadius;
            }
            float right = left + getGridColumnWidth() * (barreString - 1);
            float bottom = top + mNoteRadius * 2;
            canvas.drawRect(left, top, right, bottom, mPaint);

            // 绘制横按边框
            if (mBarreStrokeWidth > 0) {
                mPaint.setAlpha(255);
                mPaint.setColor(mBarreStrokeColor);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(mBarreStrokeWidth);
                canvas.drawLine(left, top + mBarreStrokeWidth / 2, right, top + mBarreStrokeWidth / 2, mPaint);
                canvas.drawLine(left, bottom - mBarreStrokeWidth / 2, right, bottom - mBarreStrokeWidth / 2, mPaint);
            }

            // 绘制横按两端节点
            drawNote(canvas, barreFret, STRING, fingers != null ? 1 : 0, 255, 0, 0);
            drawNote(canvas, barreFret, STRING - (barreString - 1), fingers != null ? 1 : 0, 255, 0, 0);
        }
        // 绘制其他节点
        for (int index = 0; index < frets.length; index++) {
            int fret = frets[index];
            // 不绘制闭弦和空弦情况
            if (fret < 1) {
                continue;
            }
            // 不绘制横按区域的节点
            if (barreChord != null && barreFret == fret && frets.length - index <= barreString) {
                continue;
            }
            drawNote(canvas, frets[index], index + 1, fingers != null ? fingers[index] : 0, mNoteAlpha, mNoteStrokeWidth, mNoteStrokeColor);
        }
    }

    private void drawNote(Canvas canvas, int fret, int string, int finger, int alpha, float strokeWidth, int strokeColor) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mNoteColor);
        mPaint.setAlpha(alpha);
        // 绘制节点实心圆
        float columnWidth = getGridColumnWidth();
        float rowHeight = getGridRowHeight();
        float cx = ((getFretWidth() + mGridLineWidth / 2) + (columnWidth * (string - 1)))
                - (string == STRING ? mGridLineWidth : mGridLineWidth / 2);

        int f = 1;
        int leastFret = getLeastFret();
        if (isExceedDefaultFret()) {
            if (fret != leastFret) {
                int result = fret % leastFret;
                f = result != 0 ? result + 1 : fret - leastFret + 1;
            }
        } else {
            f = fret;
        }
        float cy = (getStringHeight() + getHeadHeight()) + (rowHeight * f) - (rowHeight / 2);
        canvas.drawCircle(cx, cy, mNoteRadius, mPaint);
        // 绘制节点文字
        if (mShowMode != SIMPLE_SHOW_MODE && finger > 0) {
            mPaint.setColor(mNoteTextColor);
            mPaint.setTextSize(mNoteTextSize);
            String fingerStr = String.valueOf(finger);
            canvas.drawText(fingerStr, cx - mPaint.measureText(fingerStr) / 2,
                    cy - (mPaint.ascent() + mPaint.descent()) / 2, mPaint);
        }
        // 绘制节点边框
        if (strokeWidth > 0) {
            mPaint.setStrokeWidth(strokeWidth);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(strokeColor);
            mPaint.setAlpha(255);
            canvas.drawCircle(cx, cy, mNoteRadius, mPaint);
        }
    }

    /**
     * 获取品文字区域的宽度。
     *
     * @return 品文字区域的宽度。
     */
    private float getFretWidth() {
        if (mChord != null) {
            mPaint.setTextSize(mFretTextSize);
            String largestFret = String.valueOf(getLeastFret() + getRow() - 1);
            return mPaint.measureText(largestFret) + mFretTextOffsetX;
        }
        return 0f;
    }

    /**
     * 获取最小品。
     *
     * @return 最小品。默认 1。
     */
    private int getLeastFret() {
        return mChord != null ? mChord.getLeastFret() : 1;
    }

    /**
     * 获取最大品。
     *
     * @return 最大品，默认为 1。
     */
    private int getLargestFret() {
        return mChord != null ? mChord.getLargestFret() : 1;
    }

    /**
     * 通过指定的弦获取其对应的空弦或闭弦表现图片 Bitmap。
     *
     * @param string 弦（0 = 6 弦，1 = 5 弦，以此类推）
     * @return 空弦或闭弦表现图片。
     */
    @Nullable
    private Bitmap getStringBitmap(int string) {
        if (mChord != null) {
            int[] frets = mChord.getFrets();
            int fret = frets[string];
            if (fret == -1) { // 闭弦
                return mClosedStringBitmap;
            } else if (fret == 0) { // 空弦
                return mEmptyStringBitmap;
            }
        }
        return null;
    }

    /**
     * 获取弦区域的高度。
     *
     * @return 弦区域的高度。
     */
    private float getStringHeight() {
        return isDrawStrings() ? Math.max(bitmapHeight(mClosedStringBitmap),
                bitmapHeight(mEmptyStringBitmap)) + mStringOffsetY : 0f;
    }

    /**
     * 是否需要绘制弦区域。
     *
     * @return 返回 true，表示需要绘制，不需要绘制返回 false。
     */
    private boolean isDrawStrings() {
        // 只要和弦中有闭弦或空弦则需要绘制
        return mChord != null && (mChord.isClosedString() || mChord.isEmptyString());
    }

    /**
     * 是否需要绘制琴头。
     *
     * @return 返回 true，表示需要绘制，不需要绘制返回 false。
     */
    private boolean isDrawHead() {
        // 如果最大品未超过 5 品则认为需要绘制
        return mChord != null && mChord.getLargestFret() <= 5;
    }

    /**
     * 获取琴头高度。
     *
     * @return 琴头的高度，如果琴头不绘制时直接返回 0。
     */
    private float getHeadHeight() {
        return isDrawHead() ? mHeadRadius : 0;
    }

    /**
     * 获取网格区域宽度。
     *
     * @return 网格区域宽度。
     */
    private float getGridWidth() {
        return getWidth() - getFretWidth() - mNoteRadius;
    }

    /**
     * 获取网格区域高度。
     *
     * @return 网格区域高度。
     */
    private float getGridHeight() {
        return getHeight() - getStringHeight() - getHeadHeight();
    }

    /**
     * 获取网格每格的宽度。
     *
     * @return 网格每格的宽度。
     */
    private float getGridColumnWidth() {
        int column = STRING - 1;
        return getGridWidth() / column;
    }

    /**
     * 获取网格每格的高度。
     *
     * @return 网格每格的高度。
     */
    private float getGridRowHeight() {
        return getGridHeight() / getRow();
    }

    /**
     * 获取指定 Bitmap 的宽度。
     *
     * @param bitmap 指定的 Bitmap 对象
     * @return Bitmap 的宽度。
     */
    private int bitmapWidth(Bitmap bitmap) {
        return bitmap != null ? bitmap.getWidth() : 0;
    }

    /**
     * 获取指定 Bitmap 的高度。
     *
     * @param bitmap 指定的 Bitmap 对象
     * @return Bitmap 的高度。
     */
    private int bitmapHeight(Bitmap bitmap) {
        return bitmap != null ? bitmap.getHeight() : 0;
    }

    /**
     * 获取行数。
     *
     * @return 行。
     */
    private int getRow() {
        // 简单模式下，如果和弦中最大品和最小品的跨度未超过三品，且 1 品为最低品，则行数就为三行
        if (mShowMode == SIMPLE_SHOW_MODE) {
            int leastFret = getLeastFret();
            int largestFret = getLargestFret();
            int diffFret = largestFret - leastFret;
            if (diffFret < 3 && leastFret == 1) {
                return 3;
            }
        }
        return 4;
    }

    /**
     * 最大品是否超过了默认显示品。
     *
     * @return 超过了，返回 true，否则返回 false。
     */
    private boolean isExceedDefaultFret() {
        return getLargestFret() > FRET;
    }

    private void drawDebug(Canvas canvas) {
        // draw grid rect
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(2f);
        float left = getFretWidth();
        float top = getStringHeight() + getHeadHeight();
        canvas.drawRect(left, top, left + getGridWidth(), top + getGridHeight(), mPaint);
    }

}
