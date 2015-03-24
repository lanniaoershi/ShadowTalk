/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.bitmapfun.ImageResizer;
import com.sonymobile.partyshare.bitmapfun.ImageCache.ImageCacheParams;
import com.sonymobile.partyshare.httpd.PartyShareCommand;
import com.sonymobile.partyshare.httpd.PhotoListController;
import com.sonymobile.partyshare.httpd.PostPhotoContent;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.PhotoUtils;
import com.sonymobile.partyshare.util.Setting;
import com.sonymobile.partyshare.util.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * The list of shared photos in Party.
 */
public class PhotoFragment extends BaseFragment implements
        LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    // Photo size.
    private static final int PHOTO_MAX_WIDTH = 1920;
    private static final int PHOTO_MAX_HEIGHT = 1080;

    // Thumbnail size.
    private static final int THUMBNAIL_WIDTH = 640;
    private static final int THUMBNAIL_HEIGHT = 480;

    // Use activity result of request.
    public static final int REQUEST_TAKE_PHOTO = 0;
    public static final int REQUEST_ADD_PHOTO = 1;

    // Use handler of message.
    private static final int MSG_SHOW_ALERT = 100;
    private static final int MSG_SHOW_TOAST = 101;

    private static final String TAKEN_PHOTO_FILE_NAME = "_taken_by_me";

    private Context mContext;

    private int mImageThumbSize;

    private int mImageThumbSpacing;

    private GridView mGridView;

    private ImageCursorAdapter mAdapter;

    private ImageResizer mImageResizer;

    private TextView mEmptyView;

    private ProgressDialog mProgressDialog = null;

    private PopupMenu mPopupMenu = null;

    private Uri mSaveUri;

    private AlertDialog mDialog = null;

    private UiHandler mHandler;

    public PhotoFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onCreate");

        mContext = getActivity().getApplicationContext();

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
        ImageCacheParams cacheParams = new ImageCacheParams();

        // Set memory cache to 25% of app memory
        cacheParams.setMemCacheSizePercent(0.25f);

        // The ImageResizer takes care of loading images into our ImageView
        // children asynchronously
        mImageResizer = new ImageResizer(getActivity(), mImageThumbSize);
        mImageResizer.setLoadingImage(R.drawable.empty_photo);
        mImageResizer.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);

        mAdapter = new ImageCursorAdapter(getActivity(), null,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER,
                mImageResizer, R.layout.photo_list_item);

        mHandler = new UiHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onCreateView");
        View view = getActivity().getLayoutInflater().inflate(R.layout.photo_fragment, null);

        LinearLayout takePhoto = (LinearLayout)view.findViewById(R.id.take_photo);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.v(LogUtil.LOG_TAG, "takePhoto button onClick");
                if (!PhotoUtils.isEnoughStorage(PhotoUtils.DOWNLOADABLE_CAPACITY_SIZE)) {
                    LogUtil.e(LogUtil.LOG_TAG,
                            "Taken Photo, storage capacity is not enough.");
                    // Show error toast.
                    Utility.showToast(mContext,
                            getString(R.string.party_share_strings_photo_upload_err_txt),
                            Toast.LENGTH_SHORT);
                    return;
                }

                final Date date = new Date(System.currentTimeMillis());
                final SimpleDateFormat dataFormat = new SimpleDateFormat(
                        "yyyyMMddHHmmss", Locale.getDefault());
                final String filename = dataFormat.format(date) + TAKEN_PHOTO_FILE_NAME + ".jpg";
                String filepath = PhotoUtils.createUniqueFileName(
                        PhotoUtils.createTempFolderPath(), filename);
                if (filepath == null || TextUtils.isEmpty(filepath)) {
                    LogUtil.e(LogUtil.LOG_TAG, "Taken Photo filepath is empty.");
                    // Show error toast.
                    Utility.showToast(mContext,
                            getString(R.string.party_share_strings_photo_upload_err_txt),
                            Toast.LENGTH_SHORT);
                    return;
                }

                mSaveUri = Uri.fromFile(new File(filepath));
                LogUtil.v(LogUtil.LOG_TAG,
                        "takePhoto button onClick mSaveUri :" + mSaveUri);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mSaveUri);
                try {
                    startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                } catch (ActivityNotFoundException e) {
                    LogUtil.e(LogUtil.LOG_TAG, "ActivityNotFoundException : " + e.toString());
                }
            }
        });

        LinearLayout addPhoto = (LinearLayout)view.findViewById(R.id.add_photo);
        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.v(LogUtil.LOG_TAG, "addPhoto button onClick");
                if (!PhotoUtils.isEnoughStorage(PhotoUtils.DOWNLOADABLE_CAPACITY_SIZE)) {
                    LogUtil.e(LogUtil.LOG_TAG,
                            "Add Photo, storage capacity is not enough.");
                    // Show error toast.
                    Utility.showToast(mContext,
                            getString(R.string.party_share_strings_photo_upload_err_txt),
                            Toast.LENGTH_SHORT);
                    return;
                }

                // Launch photo picker.
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                try {
                    startActivityForResult(intent, REQUEST_ADD_PHOTO);
                } catch (ActivityNotFoundException e) {
                    LogUtil.e(LogUtil.LOG_TAG, "ActivityNotFoundException : " + e.toString());
                }
            }
        });

        mEmptyView = (TextView)view.findViewById(R.id.empty_view);

        mGridView = (GridView)view.findViewById(R.id.photo_list);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int gridWidth = mGridView.getWidth();
                        LogUtil.d(LogUtil.LOG_TAG,
                                "onCreateView - mGridView.getWidth() " + gridWidth);
                        LogUtil.d(LogUtil.LOG_TAG,
                                "onCreateView - mImageThumbSpacing " + mImageThumbSpacing);
                        if (gridWidth > 0) {
                            final int columnWidth = (gridWidth - mImageThumbSpacing) / 2;
                            mAdapter.setItemHeight(columnWidth);
                            LogUtil.d(LogUtil.LOG_TAG, "onCreateView - columnWidth " + columnWidth);
                            mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });

        getLoaderManager().initLoader(1, null, this);

        if (!ConnectionManager.isGroupOwner()) {
            // Reload photo info from GroupOwner if client user.
            PhotoListController.reloadPhotoList(getActivity(), true);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onResume");
        mImageResizer.setExitTasksEarly(false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onPause");
        mImageResizer.setPauseWork(false);
        mImageResizer.setExitTasksEarly(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onStop");
        dismissDialog();
        dismissPopupMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onDestroyView");
        dismissProgressDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onDestroy");
        mImageResizer.clearCache();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onItemClick id : " + id);

        Cursor c = mAdapter.getCursor();
        if (c != null && c.moveToPosition(position)) {
            final String filePath =
                    c.getString(c.getColumnIndex(PhotoProvider.COLUMN_LOCAL_FILE_PATH));
            if (!TextUtils.isEmpty(filePath) && (new File(filePath)).exists()) {
                // Photo is already downloaded.
                // Launch Photo Viewer.
                launchPhotoViewer(Uri.fromFile(new File(filePath)));
            } else {
                // Start downloading.
                downloadFile(id);
            }
        }
        return;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onItemLongClick id : " + id);
        Cursor c = mAdapter.getCursor();
        if (c != null && c.moveToPosition(position)) {
            showPopupMenu(view, (int)id, c);
        }
        return true;
    }

    private void launchPhotoViewer(Uri uri) {
        // Launch Photo Viewer.
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        try {
            getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogUtil.e(LogUtil.LOG_TAG, "ActivityNotFoundException : " + e.toString());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg) {
        return new CursorLoader(getActivity(), PhotoProvider.CONTENT_URI, null,
                null, null, getSortOrder());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onLoadFinished ");

        if (c != null) {
            int count = c.getCount();
            LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onLoadFinished count = " + count);
            if (count == 0) {
                // Show empty view.
                mGridView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                // Show thumbnail view.
                mEmptyView.setVisibility(View.GONE);
                mGridView.setVisibility(View.VISIBLE);
            }
        }

        mAdapter.swapCursor(c);
        // Dismiss PopupMenu when update list.
        dismissPopupMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onLoaderReset ");
        mAdapter.swapCursor(null);
    }

    private String getSortOrder() {
        String sortOrder = null;
        switch(Setting.getSortOrderPhoto(getActivity())) {
            case Setting.PHOTO_SORT_TAKEN_DATE_ASC:
                sortOrder = PhotoProvider.COLUMN_TAKEN_DATE + " ASC";
                break;
            case Setting.PHOTO_SORT_TAKEN_DATE_DESC:
                sortOrder = PhotoProvider.COLUMN_TAKEN_DATE + " DESC";
                break;
            case Setting.PHOTO_SORT_SHARED_DATE_ASC:
                sortOrder = PhotoProvider.COLUMN_SHARED_DATE + " ASC";
                break;
            case Setting.PHOTO_SORT_SHARED_DATE_DESC:
                sortOrder = PhotoProvider.COLUMN_SHARED_DATE + " DESC";
                break;
            default:
                break;
        }
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getSortOrder : " + sortOrder);
        return sortOrder;
    }

    private void showPopupMenu(View view, final int id, Cursor c) {
        final String masterFilePath =
                c.getString(c.getColumnIndex(PhotoProvider.COLUMN_MASTER_FILE_PATH));
        final String localThumbPath =
                c.getString(c.getColumnIndex(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH));
        final String ownerAddress =
                c.getString(c.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS));

        mPopupMenu = new PopupMenu(getActivity(), view);
        mPopupMenu.getMenuInflater().inflate(R.menu.photo_list_popup, mPopupMenu.getMenu());

        String ipAddress = Utility.getIpAddress(getActivity(),
                c.getString(c.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS)));
        if (ConnectionManager.isGroupOwner()) {
            mPopupMenu.getMenu().findItem(R.id.remove_photo).setEnabled(true);
        } else if (!ConnectionManager.getLocalAddress().equalsIgnoreCase(ipAddress)) {
            mPopupMenu.getMenu().findItem(R.id.remove_photo).setEnabled(false);
        }

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.remove_photo:
                        // Show dialog for confirm stop sharing.
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        mDialog = builder.setMessage(
                                R.string.party_share_strings_stop_sharing_photo_dialog_txt)
                                .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Remove photo.
                                            PhotoUtils.removePhoto(mContext, localThumbPath, id,
                                                    masterFilePath, ownerAddress);
                                        }
                                    })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create();
                        mDialog.show();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        mPopupMenu.show();
    }

    public void dismissPopupMenu() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
            mPopupMenu = null;
        }
    }

    private void downloadFile(final long id) {
        // Show ProgressDialog.
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        showProgressDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Start file download.
                PhotoDownloader.getInstance(mContext).download(id, mPhotoEventListener);
                dismissProgressDialog();
            }
        }).start();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage(getResources().getString(
                R.string.party_share_strings_downloading_photo_dialog_txt));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onActivityResult " + requestCode);
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                // Upload the taken photo.
                uploadTakenPhoto(mSaveUri.getPath());
            }
        } else if (requestCode == REQUEST_ADD_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.onActivityResult ADD_PHOTO Uri :"
                            + uri.toString());

                    // Upload the add photo.
                    uploadAddPhoto(uri);
                }
            }
        }
    }

    private void uploadTakenPhoto(final String tempPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String thumbnailPath = null;
                String filePath = null;
                Uri uri = null;
                Uri insertLocalUri = null;
                boolean isError = true;
                String mimeType = "image/jpeg";
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    LogUtil.v(LogUtil.LOG_TAG, "uploadTakenPhoto tempPath : " + tempPath);

                    // Copy to party share folder.
                    File tempFile = new File(tempPath);
                    String filename = tempFile.getName();
                    LogUtil.v(LogUtil.LOG_TAG, "uploadTakenPhoto filename : " + filename);
                    filePath = PhotoUtils.createUniqueFileName(
                            Setting.getPhotoSaveFolder(getActivity()), filename);
                    LogUtil.v(LogUtil.LOG_TAG, "uploadTakenPhoto filePath : " + filePath);
                    fis = new FileInputStream(new File(tempPath));
                    fos = new FileOutputStream(new File(filePath));
                    if (!PhotoUtils.copyFile(fis, fos)) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed copy photo");
                        return;
                    }

                    // Delete temp file.
                    if (!tempFile.delete()) {
                        LogUtil.w(LogUtil.LOG_TAG, "Temp file delete error.");
                    }

                    // Resize photo file.
                    if (!resizePhotoFile(mContext, filePath, mimeType)) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed resize photo");
                        return;
                    }

                    // Create thumbnail file.
                    thumbnailPath = saveThumbnailFile(mContext, filePath);
                    if (thumbnailPath == null || TextUtils.isEmpty(thumbnailPath)) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed create thumbnail file");
                        return;
                    }

                    // Create photo data.
                    HashMap<String, String> map = createPhotoData(
                            thumbnailPath, filePath, mimeType, null);

                    // Insert local photo data to DB.
                    insertLocalUri = insertLocalPhoto(map);
                    if (insertLocalUri == null || insertLocalUri == Uri.EMPTY) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed insert local photo");
                        return;
                    }

                    // Insert photo data to DB.
                    uri = insertPhoto(map);
                    if (uri == null || uri == Uri.EMPTY) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed insert photo");
                        return;
                    }

                    // Update master uri data to DB.
                    int ret = updateMasterUri(uri, map);
                    if (0 >= ret) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed update photo");
                        return;
                    }

                    // Scan file.
                    PhotoUtils.scanFile(mContext, filePath, null);

                    new PostPhotoContent(mContext).post(map);

                    isError = false;
                } catch (NullPointerException npe) {
                    LogUtil.w(LogUtil.LOG_TAG, "NullPointerException occur : " + npe);
                } catch (FileNotFoundException fnfe) {
                    LogUtil.w(LogUtil.LOG_TAG, "FileNotFoundException occur : " + fnfe);
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                    }

                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException e) {
                    }

                    if (isError) {
                        // Clean data.
                        cleanFailedUploadPhoto(thumbnailPath, filePath, uri, insertLocalUri);

                        // Show error toast.
                        Message msg = new Message();
                        msg.what = MSG_SHOW_TOAST;
                        msg.arg1 = R.string.party_share_strings_photo_upload_err_txt;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }).start();
    }

    private void uploadAddPhoto(final Uri fileUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String thumbnailPath = null;
                String filePath = null;
                Uri insertUri = null;
                Uri insertLocalUri = null;
                boolean isError = true;
                Cursor cursor = null;
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    String[] columns = {
                            MediaStore.Images.Media._ID,
                            MediaStore.Images.Media.SIZE,
                            MediaStore.Images.Media.DATA,
                            MediaStore.Images.Media.MIME_TYPE,
                            MediaStore.Images.Media.DATE_TAKEN
                            };
                    cursor = ProviderUtil.query(mContext, fileUri, columns, null,
                            null, null);
                    if (cursor == null || !cursor.moveToFirst()) {
                        LogUtil.d(LogUtil.LOG_TAG, "PhotoFragment.uploadAddPhoto : No data");
                        return;
                    }
                    String id = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media._ID));
                    LogUtil.v(LogUtil.LOG_TAG,
                            "PhotoFragment.uploadAddPhoto _ID : " + id);

                    String fileSize = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
                    LogUtil.v(LogUtil.LOG_TAG,
                            "PhotoFragment.uploadAddPhoto SIZE : " + fileSize);

                    String orgPath = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    LogUtil.v(LogUtil.LOG_TAG,
                            "PhotoFragment.uploadAddPhoto DATA : " + orgPath);

                    String mimeType = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
                    LogUtil.v(LogUtil.LOG_TAG,
                            "PhotoFragment.uploadAddPhoto MIME_TYPE : " + mimeType);

                    long takenDate = cursor.getLong(
                            cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));
                    LogUtil.v(LogUtil.LOG_TAG,
                            "PhotoFragment.uploadAddPhoto DATE_TAKEN : " + takenDate);
                    Date date = new Date(takenDate);
                    SimpleDateFormat dataFormat = new SimpleDateFormat(
                            "yyyyMMddHHmmss", Locale.getDefault());
                    String formatTakenDate = dataFormat.format(date);
                    LogUtil.v(LogUtil.LOG_TAG,
                            "PhotoFragment.uploadAddPhoto formatTakenDate : " + formatTakenDate);

                    // Check support file type.
                    if (TextUtils.isEmpty(mimeType)) {
                        mimeType = PhotoUtils.getMimeType(orgPath);
                    }
                    if (!("image/jpeg".equalsIgnoreCase(mimeType)
                            || "image/png".equalsIgnoreCase(mimeType))) {
                        LogUtil.w(LogUtil.LOG_TAG, "Not supported format " + mimeType);
                        isError = false;

                        // Show dialog.
                        Message msg = new Message();
                        msg.what = MSG_SHOW_ALERT;
                        msg.arg1 = R.string.party_share_strings_invalid_photo_format_err_txt;
                        mHandler.sendMessage(msg);
                        return;
                    }

                    // Copy to party share folder.
                    // Create file path
                    String dirPath = PhotoUtils.createMyPhotoFolder(
                            mContext, PhotoUtils.ADD_PHOTOS_DIR_NAME);
                    if (dirPath == null) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed create add photos folder");
                        return;
                    }

                    String ext = PhotoUtils.getExtension(mimeType);
                    final String filename = createAddPhotoFileName(id, fileSize, ext);
                    filePath = PhotoUtils.createUniqueFileName(dirPath, filename);

                    fis = new FileInputStream(new File(orgPath));
                    fos = new FileOutputStream(new File(filePath));
                    if (!PhotoUtils.copyFile(fis, fos)) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed copy photo");
                        return;
                    }

                    // Resize photo file.
                    if (!resizePhotoFile(mContext, filePath, mimeType)) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed resize photo");
                        return;
                    }

                    // Create thumbnail file.
                    thumbnailPath = saveThumbnailFile(mContext, filePath);
                    LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.uploadAddPhoto thumbnailPath : "
                            + thumbnailPath);
                    if (thumbnailPath == null || TextUtils.isEmpty(thumbnailPath)) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed create thumbnail file");
                        return;
                    }

                    // Create photo data.
                    HashMap<String, String> map = createPhotoData(
                            thumbnailPath, filePath, mimeType, formatTakenDate);

                    // Insert local photo data to DB.
                    insertLocalUri = insertLocalPhoto(map);
                    if (insertLocalUri == null || insertLocalUri == Uri.EMPTY) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed insert local photo");
                        return;
                    }

                    // Insert photo data to DB.
                    insertUri = insertPhoto(map);
                    if (insertUri == null || insertUri == Uri.EMPTY) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed insert photo");
                        return;
                    }

                    // Update master uri data to DB.
                    int rows = updateMasterUri(insertUri, map);
                    if (0 >= rows) {
                        LogUtil.w(LogUtil.LOG_TAG, "Failed update photo");
                        return;
                    }

                    new PostPhotoContent(mContext).post(map);

                    isError = false;
                } catch (NullPointerException npe) {
                    LogUtil.w(LogUtil.LOG_TAG, "NullPointerException occur : " + npe);
                } catch (FileNotFoundException fnfe) {
                    LogUtil.w(LogUtil.LOG_TAG, "FileNotFoundException occur : " + fnfe);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }

                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                    }

                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException e) {
                    }

                    if (isError) {
                        // Clean data.
                        cleanFailedUploadPhoto(thumbnailPath, filePath, insertUri, insertLocalUri);

                        // Show error toast.
                        Message msg = new Message();
                        msg.what = MSG_SHOW_TOAST;
                        msg.arg1 = R.string.party_share_strings_photo_upload_err_txt;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }).start();
    }

    private String createAddPhotoFileName(String id, String size, String ext) {
        String fileName = id + "_" + size + ext;
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.createAddPhotoFileName fileName : " + fileName);
        return fileName;
    }

    private void cleanFailedUploadPhoto(
            String thumbnailPath, String filePath, Uri uri, Uri localUri) {
        // Delete thumbnail file.
        PhotoUtils.deleteFile(thumbnailPath);

        // Delete photo file.
        PhotoUtils.deleteFile(filePath);

        // Delete photo data form db.
        if (!(uri == null || uri == Uri.EMPTY)) {
            ProviderUtil.delete(mContext, uri, null, null);
        }

        // Delete local photo data form db.
        if (!(localUri == null || localUri == Uri.EMPTY)) {
            ProviderUtil.delete(mContext, localUri, null, null);
        }
    }

    private HashMap<String, String> createPhotoData(String thumbnailPath, String filePath,
            String mimeType, String takenDate) {
        HashMap<String, String> map = new HashMap<String, String>();
        // Local thumbnail Path.
        map.put(PartyShareCommand.PARAM_PHOTO_LOCAL_THUMBNAIL_PATH, thumbnailPath);

        // Local thumbnail file name.
        String[] tmpThumb = thumbnailPath.split("/");
        String thumbName = tmpThumb[tmpThumb.length - 1];
        map.put(PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME, thumbName);

        // Local file Path.
        map.put(PartyShareCommand.PARAM_PHOTO_LOCAL_FILE_PATH, filePath);

        // Shared date.
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String sharedDate = dataFormat.format(date);
        map.put(PartyShareCommand.PARAM_PHOTO_SHARED_DATE, sharedDate);

        // Taken date.
        if (TextUtils.isEmpty(takenDate)) {
            takenDate = getTakenDate(filePath);
        }
        map.put(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE, takenDate);

        // Get mac add from Connection manager
        map.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, ConnectionManager.getMyAddress());

        // Mime type.
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = PhotoUtils.getMimeType(filePath);
        }
        map.put(PartyShareCommand.PARAM_PHOTO_MIME_TYPE, mimeType);

        return map;
    }

    private Uri insertLocalPhoto(HashMap<String, String> map) {
        // Insert local photo.
        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_THUMBNAIL_PATH,
                map.get(PartyShareCommand.PARAM_PHOTO_LOCAL_THUMBNAIL_PATH));
        values.put(PhotoProvider.COLUMN_FILE_PATH,
                map.get(PartyShareCommand.PARAM_PHOTO_LOCAL_FILE_PATH));
        Uri localPhoto = ProviderUtil.insert(mContext,
                PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values);
        if (localPhoto == null || localPhoto == Uri.EMPTY) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed insert local photo, uri is null");
            return null;
        }

        String id = localPhoto.getLastPathSegment();
        String masterFilePath = "/file/" + id;
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, masterFilePath);

        // Update query value for local photo.
        values.clear();
        String queryValue = String.format("%s:%s",
                map.get(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS), masterFilePath);
        LogUtil.v(LogUtil.LOG_TAG,
                "PhotoFragment.insertLocalPhoto queryValue : " + queryValue);
        values.put(PhotoProvider.COLUMN_QUERY_PATH, queryValue);
        int num = ProviderUtil.update(mContext, localPhoto, values, null, null);
        if (0 >= num) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed query local photo for update query value.");
            return null;
        }

        return localPhoto;
    }

    private Uri insertPhoto(HashMap<String, String> map) {
        // Insert DB (photo list)
        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH,
                map.get(PartyShareCommand.PARAM_PHOTO_LOCAL_THUMBNAIL_PATH));
        values.put(PhotoProvider.COLUMN_LOCAL_FILE_PATH,
                map.get(PartyShareCommand.PARAM_PHOTO_LOCAL_FILE_PATH));
        values.put(PhotoProvider.COLUMN_MIME_TYPE,
                map.get(PartyShareCommand.PARAM_PHOTO_MIME_TYPE));
        values.put(PhotoProvider.COLUMN_SHARED_DATE,
                map.get(PartyShareCommand.PARAM_PHOTO_SHARED_DATE));
        values.put(PhotoProvider.COLUMN_TAKEN_DATE,
                map.get(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE));
        values.put(PhotoProvider.COLUMN_OWNER_ADDRESS,
                map.get(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS));
        values.put(PhotoProvider.COLUMN_MASTER_FILE_PATH,
                map.get(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH));
        values.put(PhotoProvider.COLUMN_DL_STATE, PhotoUtils.DOWNLOAD_STATE_AFTER);

        return ProviderUtil.insert(mContext, PhotoProvider.CONTENT_URI, values);
    }

    private int updateMasterUri(Uri uri, HashMap<String, String> map) {
        String id = uri.getLastPathSegment();
        String masterThumbnailPath;
        if (ConnectionManager.isGroupOwner()) {
            masterThumbnailPath = "/host/thumbnail/" + id;
        } else {
            masterThumbnailPath = "/thumbnail/" + id;
        }
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH, masterThumbnailPath);

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH, masterThumbnailPath);
        return ProviderUtil.update(mContext,
                PhotoProvider.CONTENT_URI, values, "_id = ?", new String[] { id });
    }

    private String getTakenDate(String filePath) {
        String result = null;
        ExifInterface exifInterface = PhotoUtils.getExifInterface(filePath);
        if (exifInterface != null) {
            result = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
        }

        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getTakenDate result : " + result);
        if (result != null) {
            result = result.replaceAll(" ", "").replaceAll(":", "");
        } else {
            Date date = new Date(new File(filePath).lastModified());
            SimpleDateFormat dataFormat = new SimpleDateFormat(
                    "yyyyMMddHHmmss", Locale.getDefault());
            result = dataFormat.format(date);
        }
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getTakenDate end result : " + result);
        return result;
    }

    public boolean resizePhotoFile(Context context, String filePath, String mimeType) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.resizePhotoFile");

        Bitmap orgBitmap = null;
        Bitmap saveImage = null;
        FileOutputStream out = null;
        ExifInterface orgEx = null;
        try {
            // Check need resize.
            if (!needResizePhotoFile(filePath, PHOTO_MAX_WIDTH, PHOTO_MAX_HEIGHT)) {
                LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.resizePhotoFile not need resize.");
                return true;
            }

            // Get exif.
            if ("image/jpeg".equalsIgnoreCase(mimeType)) {
                orgEx = PhotoUtils.getExifInterface(filePath);
            }

            // Get sample down a bitmap.
            orgBitmap = ImageResizer.decodeSampledBitmapFromFile(
                    filePath, PHOTO_MAX_WIDTH, PHOTO_MAX_HEIGHT, null);
            if (orgBitmap == null) {
                LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.resizePhotoFile : cannot be decoded");
                return false;
            }

            // Set scale.
            Matrix matrix = new Matrix();
            final int width = orgBitmap.getWidth();
            final int height = orgBitmap.getHeight();
            float scale = getScale(width, height, PHOTO_MAX_WIDTH, PHOTO_MAX_HEIGHT);
            matrix.postScale(scale, scale);

            saveImage = Bitmap.createBitmap(orgBitmap, 0, 0, width, height, matrix, false);
            out = new FileOutputStream(filePath);
            // Check mime type for select compress of format.
            if ("image/jpeg".equalsIgnoreCase(mimeType)) {
                saveImage.compress(CompressFormat.JPEG, 100, out);
            } else {
                saveImage.compress(CompressFormat.PNG, 100, out);
            }
            out.flush();
        } catch(IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed resize photo. " + e);
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                }
            }
            if (orgBitmap != null) {
                orgBitmap.recycle();
                orgBitmap = null;
            }
            if (saveImage != null) {
                saveImage.recycle();
                saveImage = null;
            }
        }

        // Restore exif.
        if (orgEx != null) {
            try {
                ExifInterface newEx = PhotoUtils.getExifInterface(filePath);
                PhotoUtils.copyExifInterface(orgEx, newEx);
                newEx.saveAttributes();
            } catch (IOException ioe) {
                LogUtil.w(LogUtil.LOG_TAG, "Failed restore exif : " + ioe);
            }
        }

        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.savePhoto file path : " + filePath);
        return true;
    }

    private float getScale(int width, int height, int reqWidth, int reqHeight) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getScale width : " + width);
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getScale  height: " + height);

        float scaleX = 1;
        if (width > reqWidth) {
            scaleX = (float) reqWidth / width;
        }
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getScale scaleX : " + scaleX);
        float scaleY = 1;
        if (height > reqHeight) {
            scaleY = (float) reqHeight / height;
        }
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getScale scaleY : " + scaleY);
        float scale = Math.min(scaleX, scaleY);
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.getScale scale : " + scale);

        return scale;
    }

    private boolean needResizePhotoFile(String filename, int reqWidth, int reqHeight) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.needResizePhotoFile");

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        float scale = getScale(options.outWidth, options.outHeight, reqWidth, reqHeight);
        LogUtil.v(LogUtil.LOG_TAG,
                "PhotoFragment.needResizePhotoFile scale = " + scale);

        return 1 > scale;
    }

    public String saveThumbnailFile(Context context, String orgFile) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.saveThumbnail");

        Bitmap saveImage = null;
        FileOutputStream out = null;
        Bitmap orgBitmap = null;
        String filePath = null;
        try {
            orgBitmap = BitmapFactory.decodeFile(orgFile);
            if (orgBitmap == null) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "Failed decode a file path into a bitmap for thumbnail");
                return null;
            }

            // Create thumbnail file path.
            String thumDir = PhotoUtils.createThumbnailFolder(context);
            if (thumDir == null || TextUtils.isEmpty(thumDir)) {
                LogUtil.e(LogUtil.LOG_TAG, "Failed create thumbnail file path.");
                return null;
            }

            Date mDate = new Date();
            SimpleDateFormat fileNameDate = new SimpleDateFormat(
                    "yyyyMMddHHmmss", Locale.getDefault());
            String fileName = fileNameDate.format(mDate) + ".jpg";
            filePath = PhotoUtils.createUniqueFileName(thumDir, fileName);
            if (filePath == null || TextUtils.isEmpty(filePath)) {
                LogUtil.e(LogUtil.LOG_TAG, "Failed create file name.");
                return null;
            }

            // Set scale.
            Matrix matrix = new Matrix();
            final int width = orgBitmap.getWidth();
            final int height = orgBitmap.getHeight();
            float scale = getScale(width, height, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            matrix.postScale(scale, scale);

            // Create save bitmap.
            saveImage = Bitmap.createBitmap(orgBitmap, 0, 0, width, height, matrix, false);

            // Write to thumbnail file.
            out = new FileOutputStream(filePath);
            saveImage.compress(CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed create thumbnail. " + e);
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                }
            }
            if (orgBitmap != null) {
                orgBitmap.recycle();
                orgBitmap = null;
            }
            if (saveImage != null) {
                saveImage.recycle();
                saveImage = null;
            }
        }

        try {
            // Set exif.
            ExifInterface orgEx = PhotoUtils.getExifInterface(orgFile);
            ExifInterface newEx = PhotoUtils.getExifInterface(filePath);
            if (orgEx != null && newEx != null) {
                LogUtil.v(LogUtil.LOG_TAG,
                        "PhotoFragment.saveThumbnail orgEx.getAttribute(TAG_ORIENTATION)"
                                + orgEx.getAttribute(ExifInterface.TAG_ORIENTATION));
                LogUtil.v(LogUtil.LOG_TAG,
                        "PhotoFragment.saveThumbnail newEx.getAttribute(TAG_ORIENTATION)"
                                + newEx.getAttribute(ExifInterface.TAG_ORIENTATION));

                newEx.setAttribute(ExifInterface.TAG_ORIENTATION,
                        orgEx.getAttribute(ExifInterface.TAG_ORIENTATION));
                newEx.saveAttributes();
                LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.saveThumbnail newEx.saveAttributes()");
            }
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed set exif to thumbnail file. " + e);
        }

        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment.saveThumbnail file path : " + filePath);
        return filePath;
    }

    private void showAlertDialog(int msg) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            mDialog = builder.setMessage(msg)
                    .setPositiveButton(
                            R.string.party_share_strings_dialog_close_button_txt, null)
                    .create();
            mDialog.show();
        }
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    public PhotoEventListener getPhotoEventListener() {
        return mPhotoEventListener;
    }

    private final PhotoEventListener mPhotoEventListener = new PhotoEventListener() {
        @Override
        public void onRefreshList() {
            LogUtil.v(LogUtil.LOG_TAG, "PhotoEventListener onRefreshList called");
            refreshList();
        }

        @Override
        public void onDownloadCompleted(String filePath) {
            LogUtil.v(LogUtil.LOG_TAG, "PhotoEventListener onDownloadCompleted called");

            try {
                dismissProgressDialog();
                // Launch Photo Viewer.
                launchPhotoViewer(Uri.fromFile(new File(filePath)));
            } catch (NullPointerException npe) {
                LogUtil.e(LogUtil.LOG_TAG, "Failed Launch Photo Viewer : " + npe);
            }
        }

        @Override
        public void onDownloadFailed(int msgId) {
            LogUtil.v(LogUtil.LOG_TAG, "PhotoEventListener onDownloadFailed called");
            Message msg = new Message();
            msg.what = MSG_SHOW_ALERT;
            msg.arg1 = msgId;
            mHandler.sendMessage(msg);
        }
    };

    private void refreshList() {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment refreshList called");
        getLoaderManager().restartLoader(1, null, this);
    }

    private static class UiHandler extends Handler {
        private final WeakReference<PhotoFragment> refPhotoFragment;
        public UiHandler(PhotoFragment fragment) {
            refPhotoFragment = new WeakReference<PhotoFragment>(fragment);
        }

        public void handleMessage(Message msg) {
            PhotoFragment fragment = refPhotoFragment.get();
            LogUtil.v(LogUtil.LOG_TAG, "PhotoFragment UiHandler msg.what :" + msg.what);
            switch (msg.what) {
                case MSG_SHOW_ALERT:
                    if (fragment != null) {
                        fragment.dismissProgressDialog();
                        fragment.dismissDialog();
                        fragment.showAlertDialog(msg.arg1);
                    }
                    break;
                case MSG_SHOW_TOAST:
                    if (fragment != null) {
                        Utility.showToast(fragment.mContext,
                                fragment.mContext.getString(msg.arg1),
                                Toast.LENGTH_SHORT);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void scrollList(final int position) {
        mGridView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelection(position);
            }
        }, 100);
    }
}