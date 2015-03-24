/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.sonymobile.partyshare.R;

import java.util.ArrayList;

public class PlayPauseButton extends View {

    // List of all bitmaps that make up the animation
    protected ArrayList<Bitmap> mBitmaps = new ArrayList<Bitmap>();

    // Bitmap displayed when pressed and not playing
    protected Bitmap mPlayPressed;

    // Bitmap displayed when pressed and playing
    protected Bitmap mPausePressed;

    // Bitmap displayed when focused and not playing
    protected Bitmap mPlayFocused;

    // Bitmap displayed when focused and playing
    protected Bitmap mPauseFocused;

    // Bitmap displayed when disabled
    protected Bitmap mPlayDisabled;

    // The state. True if playing
    private boolean mPlaying;

    // The index in the mBitmaps array of the current image
    private int mCurrentImage;

    // Paint object used to draw bitmaps with
    private Paint mPaint;

    // Runnable that handles the animation of the button
    private Runnable mAnimator = new Runnable() {
        @Override
        public void run() {
            mCurrentImage += mPlaying ? 1 : -1;
            invalidate();
            if ((mPlaying && mCurrentImage < mBitmaps.size() - 1)
                    || (!mPlaying && mCurrentImage > 0)) {
                postOnAnimationDelayed(this, 16);
            }
        }
    };

    public PlayPauseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        // we always want to be focusable
        setFocusable(true);
        mPaint = new Paint();
        loadBitmaps();
    }

    public void setPlaying(boolean playing, boolean animate) {
        if (mPlaying == playing) {
            // if it's the same state as before, just ignore
            return;
        }
        mPlaying = playing;
        if (animate) {
            removeCallbacks(mAnimator);
            postOnAnimation(mAnimator);
        } else {
            mCurrentImage = mPlaying ? mBitmaps.size() - 1 : 0;
            invalidate();
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);

        // we change bitmap when we're pressed, we need to redraw
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // we change bitmap when we're disabled
        invalidate();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        // we change bitmap when we change focus
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = mBitmaps.get(0).getWidth() + getPaddingLeft() + getPaddingRight();
        int height = mBitmaps.get(0).getHeight() + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(
                width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = getCurrentBitmap();
        if (bitmap != null) {
            // center if there is excess space
            int cx = (getWidth() - mBitmaps.get(0).getWidth()) / 2;
            int cy = (getHeight() - mBitmaps.get(0).getHeight()) / 2;
            canvas.drawBitmap(bitmap, cx, cy, mPaint);
        }
    }

    protected void loadBitmaps() {
        Resources res = getResources();
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0000_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0001_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0002_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0003_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0004_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0005_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0006_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0007_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0008_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0009_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0010_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0011_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0012_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0013_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0014_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0015_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0016_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0017_icn));
        mBitmaps.add(BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_0018_icn));

        mPlayPressed = BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_pressed_icn);
        mPausePressed = BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_pause_pressed_icn);

        mPlayFocused = BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_focused_icn);
        mPauseFocused = BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_pause_focused_icn);

        mPlayDisabled = BitmapFactory.decodeResource(
                res, R.drawable.party_share_music_list_play_disable_icn);
    }

    private Bitmap getCurrentBitmap() {
        int size = mBitmaps.size();
        if (size <= 0) {
            return null;
        }

        if (mCurrentImage < 0) {
            mCurrentImage = 0;
        } else if (mCurrentImage > size - 1) {
            mCurrentImage = size - 1;
        }
        Bitmap bitmap = mBitmaps.get(mCurrentImage);
        if (isPressed()) {
            if (mCurrentImage == 0) {
                bitmap = mPlayPressed;
            } else if (mCurrentImage == size - 1) {
                bitmap = mPausePressed;
            }
        } else if (isFocused()) {
            if (mCurrentImage == 0) {
                bitmap = mPlayFocused;
            } else if (mCurrentImage == size - 1) {
                bitmap = mPauseFocused;
            }
        } else if (!isEnabled()) {
            bitmap = mPlayDisabled;
        }
        return bitmap;
    }
}
