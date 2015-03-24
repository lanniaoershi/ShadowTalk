/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.bitmapfun.ImageResizer;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.PhotoUtils;

public class ImageCursorAdapter extends CursorAdapter {

    private final Context mContext;

    private int mItemHeight = 0;

    private GridView.LayoutParams mViewLayoutParams;

    private ImageResizer mImageResizer;

    private LayoutInflater mInflater;

    private int mLayoutId = 0;

    public static class ViewHolder {
        ImageView thumbnail;
        ImageView stateIcon;
    }

    public ImageCursorAdapter(Context context, Cursor cursor, int flag,
            ImageResizer ImageResizer, int layoutId) {
        super(context, cursor, flag);
        mContext = context;
        mImageResizer = ImageResizer;
        mViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mInflater = LayoutInflater.from(mContext);
        mLayoutId = layoutId;
    }

    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        View view = mInflater.inflate(mLayoutId, null);
        view.setLayoutParams(mViewLayoutParams);

        ViewHolder holder = new ViewHolder();
        holder.thumbnail = (ImageView)view.findViewById(R.id.photo_image);
        holder.stateIcon = (ImageView)view.findViewById(R.id.dl_state);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        if (view.getLayoutParams().height != mItemHeight) {
            view.setLayoutParams(mViewLayoutParams);
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        String filePath = c.getString(c.getColumnIndex(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH));
        mImageResizer.loadImage(filePath, holder.thumbnail);

        // Set rotation from exif info of file.
        holder.thumbnail.setRotation(getFileRotate(filePath));

        int state = c.getInt(c.getColumnIndex(PhotoProvider.COLUMN_DL_STATE));
        setDownloadStateIcon(holder.stateIcon, state);
    }

    /**
     * Sets the item height. Useful for when we know the column width so the
     * height can be set to match.
     *
     * @param height
     */
    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }

        mItemHeight = height;
        LogUtil.v(LogUtil.LOG_TAG, "ImageCursorAdapter.setItemHeight mItemHeight : " + mItemHeight);
        mViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
        mImageResizer.setImageSize(height);
        notifyDataSetChanged();
    }

    private float getFileRotate(String filePath) {
        ExifInterface exifInterface = PhotoUtils.getExifInterface(filePath);
        if (exifInterface == null) {
            return 0;
        }

        float rotate = 0;
        int orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);
        LogUtil.v(LogUtil.LOG_TAG, "ImageCursorAdapter orientation : " + orientation);
        switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_180:
            rotate = 180f;
            break;
        case ExifInterface.ORIENTATION_TRANSPOSE:
            rotate = 90f;
            break;
        case ExifInterface.ORIENTATION_ROTATE_90:
            rotate = 90f;
            break;
        case ExifInterface.ORIENTATION_TRANSVERSE:
            rotate = -90f;
            break;
        case ExifInterface.ORIENTATION_ROTATE_270:
            rotate = -90f;
            break;
        case ExifInterface.ORIENTATION_UNDEFINED:
        case ExifInterface.ORIENTATION_NORMAL:
        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        default:
            break;
        }
        LogUtil.v(LogUtil.LOG_TAG, "ImageCursorAdapter rotate : " + rotate);
        return rotate;
    }

    private void setDownloadStateIcon(ImageView imageView, int state) {
        LogUtil.v(LogUtil.LOG_TAG, "ImageCursorAdapter setDownloadStateIcon : " + state);

        switch (state) {
        case PhotoUtils.DOWNLOAD_STATE_BEFORE:
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.photo_download_not);
            break;
        case PhotoUtils.DOWNLOAD_STATE_DURING:
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.photo_download);
            break;
        case PhotoUtils.DOWNLOAD_STATE_AFTER:
            imageView.setVisibility(View.INVISIBLE);
            break;
        default:
            imageView.setVisibility(View.INVISIBLE);
            break;
        }
    }
}
