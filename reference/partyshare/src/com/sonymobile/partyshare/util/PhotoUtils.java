/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.util;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.sonymobile.partyshare.httpd.PartyShareCommand;
import com.sonymobile.partyshare.httpd.PostPhotoContent;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.session.ConnectionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PhotoUtils {

    private static final String ACTION_MUSIC_SLIDESHOW =
            "com.sonymobile.musicslideshow.intent.action.MUSIC_SLIDESHOW";
    private static final String EXTRA_AUTO_PLAY =
            "com.sonymobile.musicslideshow.intent.extra.AUTO_PLAY";
    private static final String EXTRA_IMAGES = "com.sonymobile.musicslideshow.intent.extra.IMAGES";

    private static final String THUMBNAIL_DIR = "/thumbnail/";

    /** Download state : before. */
    public static final int DOWNLOAD_STATE_BEFORE = 0;
    /** Download state : during. */
    public static final int DOWNLOAD_STATE_DURING = 1;
    /** Download state : after. */
    public static final int DOWNLOAD_STATE_AFTER = 2;

    // If we need to save file in safety, we can add some margin size. e.g. 1MB = 1 * 1024*1024
    private static final int DEFAULT_THRESHOLD_MAX_BYTES = 0;

    public static final int DOWNLOADABLE_CAPACITY_SIZE = 2 * 1024 * 1024;

    public static final String ADD_PHOTOS_DIR_NAME = "AddPhotos";

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            LogUtil.d(LogUtil.LOG_TAG, "copyFile : " + e);
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
            }
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        return true;
    }

    public static String getExtension(String mimeType) {
        if ("image/jpeg".equalsIgnoreCase(mimeType)) {
            return ".jpg";
        }
        return ".png";
    }

    public static void scanFile(
            Context context, String filePath, OnScanCompletedListener listener) {
        String[] paths = {
            filePath
        };
        MediaScannerConnection.scanFile(context, paths, null, listener);
    }

    public static String createSaveFolderPath(Context context) {
        String dirname = ConnectionManager.getSessionName()
                + "_" + ConnectionManager.getStartSessionTime();
        LogUtil.v(LogUtil.LOG_TAG, "createSaveFolderPath() dirname : " + dirname);
        dirname = replaceNotUseChar(dirname);
        LogUtil.v(LogUtil.LOG_TAG, "createSaveFolderPath() replaced dirname : " + dirname);

        String saveDirPath = Environment.getExternalStorageDirectory().toString()
                + "/Pictures/PartyShare/" + dirname;
        LogUtil.v(LogUtil.LOG_TAG, "createSaveFolderPath saveDirPath : " + saveDirPath);
        return saveDirPath;
    }

    public static String createTempFolderPath() {
        String tempDirPath = Environment.getExternalStorageDirectory().toString()
                + "/Pictures/PartyShare/Temp";
        LogUtil.v(LogUtil.LOG_TAG, "createTempFolderPath tempDirPath : " + tempDirPath);
        return tempDirPath;
    }

    public static void deleteThumbnailFolder(Context context) {
        File appDir = context.getFilesDir();
        File thumDir = null;
        try {
            thumDir = new File(appDir.getPath() + THUMBNAIL_DIR);
        } catch (NullPointerException npe) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "deleteThumbnailFolder NullPointerException occur : " + npe);
            return;
        }
        deleteFile(thumDir);
    }

    private static void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isFile()) {
            if (file.delete()) {
                LogUtil.d(LogUtil.LOG_TAG, "deleteFile() delete success");
            }
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }

            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }
            if (file.delete()) {
                LogUtil.d(LogUtil.LOG_TAG, "deleteFile() delete success");
            }
        }
    }

    public static String createThumbnailFolder(Context context) {
        File appDir = context.getFilesDir();
        File thumDir = null;
        try {
            thumDir = new File(appDir.getPath() + THUMBNAIL_DIR);
        } catch (NullPointerException npe) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "createThumbnailFolder NullPointerException occur : " + npe);
            return null;
        }

        if (!thumDir.exists()) {
            if (!thumDir.mkdirs()) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "Thumbnail directory create error");
                return null;
            }
        }

        return thumDir.getPath();
    }

    public static String createMyPhotoFolder(Context context, String dirName) {
        LogUtil.v(LogUtil.LOG_TAG, "createMyPhotoFolder()");

        if (dirName == null || dirName.isEmpty()) {
            LogUtil.w(LogUtil.LOG_TAG, "Create my photo folder. Illegal directory name.");
            return null;
        }

        String path = Setting.getPhotoSaveFolder(context) + "/" + dirName;
        LogUtil.v(LogUtil.LOG_TAG, "createMyPhotoFolder() path = " + path);
        File directory = null;
        try {
            directory = new File(path);
        } catch (NullPointerException npe) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "createMyPhotoFolder NullPointerException occur : " + npe);
            return null;
        }

        // Make directory.
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                LogUtil.e(LogUtil.LOG_TAG, "make directory failure. path = " + path);
                return null;
            }
        }

        File file = new File(directory, ".nomedia");
        LogUtil.v(LogUtil.LOG_TAG, "createMyPhotoFolder() file.getPath() : " + file.getPath());
        try {
            boolean ret = file.createNewFile();
            LogUtil.v(LogUtil.LOG_TAG,
                    "createMyPhotoFolder() file.createNewFile() : " + ret);
        } catch (IOException ioe) {
            LogUtil.w(LogUtil.LOG_TAG, "Failed create .nomedia file  : " + ioe);
            return null;
        }

        return path;
    }

    public static synchronized String createUniqueFileName(String dir, String filename) {
        LogUtil.v(LogUtil.LOG_TAG, "createUniqueFileName()");

        File directory = null;
        try {
            directory = new File(dir);
        } catch (NullPointerException npe) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "createUniqueFileName NullPointerException occur : " + npe);
            return null;
        }

        // Make directory.
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                LogUtil.e(LogUtil.LOG_TAG, "make directory failure. dir = " + dir);
                return null;
            }
        }

        File file = new File(directory, filename);
        LogUtil.v(LogUtil.LOG_TAG, "createUniqueFileName() file.getPath() : " + file.getPath());
        if (!file.exists()) {
            return file.getPath();
        }

        // Get the extension of the file, if any.
        int index = filename.lastIndexOf('.');
        String name = filename;
        String extension = "";
        if (index != -1) {
            name = filename.substring(0, index);
            extension = filename.substring(index);
        }

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            file = new File(directory, name + "_" + i + extension);
            if (!file.exists()) {
                return file.getPath();
            }
        }

        return null;
    }

    public static String getMimeType(String filePath) {
        String mimeType = null;
        String extension = null;
        extension = MimeTypeMap.getFileExtensionFromUrl(filePath);

        if(extension != null){
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            mimeType = mime.getMimeTypeFromExtension(extension);
        }
        return mimeType;
    }

    public static void deleteFile(final String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            LogUtil.d(LogUtil.LOG_TAG, "deleteFile Parameter is illegal. filePath is empty.");
            return;
        }

        File deleteFile = null;
        try {
            deleteFile = new File(filePath);
        } catch (NullPointerException npe) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "deleteFile NullPointerException occur : " + npe);
            return;
        }

        deleteFile(deleteFile);
    }

    public static ExifInterface getExifInterface(String filePath) {
        ExifInterface ex = null;
        try {
            ex = new ExifInterface(filePath);
        } catch (IOException ioe) {
        }
        return ex;
    }

    /**
     * Change character that can not use for folder and file name.
     * @param fileName original file name.
     * @return Replaced string.
     */
    public static String replaceNotUseChar(String name) {
        // Remove can not use character.
        final String str = "\\/:*?<>|\".'";
        for(int i = 0; i <= str.length() -1; i++) {
            name = name.replace(String.valueOf(str.charAt(i)), "");
        }

        // Remove line break.
        name = name.replaceAll("(\r\n|\n)", "");

        return name;
    }

    public static boolean canLaunchSlideShow(Context context) {
        boolean ret = false;
        Intent intent = new Intent();
        intent.setAction(ACTION_MUSIC_SLIDESHOW);
        List<ResolveInfo> resolveInfos = null;
        resolveInfos = context.getPackageManager().queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos != null && resolveInfos.size() != 0) {
            ret = true;
        }
        LogUtil.d(LogUtil.LOG_TAG, "canLaunchSlideShow : " + ret);
        return ret;
    }

    public static boolean launchSlideshow(Context context) {
        LogUtil.d(LogUtil.LOG_TAG, "launchSlideshow");

        ArrayList<Uri> list = createImagesContentUriList(context);
        if (list == null || list.size() == 0) {
            LogUtil.d(LogUtil.LOG_TAG,
                    "launchSlideshow There is no photo that can be displayed.");
            return false;
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_MUSIC_SLIDESHOW);
        intent.putExtra(EXTRA_AUTO_PLAY, true);
        intent.putParcelableArrayListExtra(EXTRA_IMAGES, list);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "ActivityNotFoundException occur when launch slideshow : "
                    + e.toString());
        }

        return true;
    }

    public static ArrayList<Uri> createImagesContentUriList(Context context) {
        LogUtil.v(LogUtil.LOG_TAG, "createImagesContentUriList()");

        String path = Setting.getPhotoSaveFolder(context);
        // Check exist file on directory.
        File[] files = getlistFiles(path);
        if (files == null || files.length == 0) {
            LogUtil.d(LogUtil.LOG_TAG, "Not exist file in save folder.");
            return null;
        }

        LogUtil.d(LogUtil.LOG_TAG, "Number of files in save folder : " + files.length);

        // Get the content uri of the image that is in the PartyShare folder
        String folders[] = path.split("/");
        String folderName = folders[folders.length -1];
        LogUtil.v(LogUtil.LOG_TAG, "save folder folderName : " + folderName);

        ArrayList<Uri> uriList = new ArrayList<Uri>();
        Uri uri = null;
        Cursor cursor = null;
        try {
            String[] columns = { MediaStore.Images.Media._ID };
            cursor = ProviderUtil.query(context,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?",
                    new String[] { folderName }, null);
            if (cursor != null && cursor.moveToFirst()) {
                LogUtil.d(LogUtil.LOG_TAG, "save folder : cursor.getcount : " + cursor.getCount());

                do {
                    String id = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media._ID));
                    LogUtil.v(LogUtil.LOG_TAG, "save folder _ID : " + id);

                    uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    LogUtil.d(LogUtil.LOG_TAG, "save folder uri : " + uri);
                    if (uri != null && Uri.EMPTY != uri && !uriList.contains(uri)) {
                        uriList.add(uri);
                    }
                } while (cursor.moveToNext());
            } else {
                LogUtil.d(LogUtil.LOG_TAG, "save folder : No data");
            }
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "Failed get the uri of the image that is in the PartyShare folder : "
                    + e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        LogUtil.d(LogUtil.LOG_TAG,
                "createImagesContentUriList PartyShare folder uriList.size() : "
                + uriList.size());

        // Get the content uri of the image that is in the AddPhotos folder
        path = Setting.getPhotoSaveFolder(context) + "/" + ADD_PHOTOS_DIR_NAME;
        // Check exist file on AddPhotos directory.
        files = getlistFiles(path);
        if (files == null || files.length == 0) {
            LogUtil.d(LogUtil.LOG_TAG, "Not exist file in AddPhotos folder.");
            return uriList;
        }

        LogUtil.d(LogUtil.LOG_TAG, "Number of files in AddPhotos folder : " + files.length);

        for (File file : files) {
            LogUtil.v(LogUtil.LOG_TAG, "file path in AddPhotos folder : " + file.getPath());
            // If directory, skip.
            if (file.isDirectory()) {
                LogUtil.v(LogUtil.LOG_TAG, "Skip directory path : " + file.getPath());
                continue;
            }

            String filename = file.getName();
            // Get the name of the file.
            int index = filename.lastIndexOf('.');
            // If not exist extention, skip.
            if (index == -1) {
                LogUtil.v(LogUtil.LOG_TAG,
                        "Skip not exist extention of file name : " + filename);
                continue;
            }
            String name = filename.substring(0, index);
            String[] keys = name.split("_");
            if (2 > keys.length) {
                LogUtil.v(LogUtil.LOG_TAG,
                        "Skip illegal file name pattern : " + filename);
                continue;
            }

            String id = keys[0];
            String fileSize = keys[1];
            LogUtil.v(LogUtil.LOG_TAG, "Query key id : " + id);
            LogUtil.v(LogUtil.LOG_TAG, "Query key fileSize : " + fileSize);

            try {
                String[] columns = { MediaStore.Images.Media._ID };
                cursor = ProviderUtil.query(context,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
                        MediaStore.Images.Media._ID + " = " + id
                        + " and " + MediaStore.Images.Media.SIZE + " = " + fileSize,
                        null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    LogUtil.d(LogUtil.LOG_TAG, "AddPhotos folder : cursor.getcount : "
                            + cursor.getCount());
                    String getId = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media._ID));
                    LogUtil.v(LogUtil.LOG_TAG, "AddPhotos folder getId : " + getId);

                    uri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, getId);
                    LogUtil.d(LogUtil.LOG_TAG, "AddPhotos folder uri : " + uri);
                    if (uri != null && Uri.EMPTY != uri && !uriList.contains(uri)) {
                        uriList.add(uri);
                    }
                } else {
                    LogUtil.d(LogUtil.LOG_TAG, "Can not find in the media DB.");
                }
            } catch (Exception e) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "Failed get the uri of the image that is in the AddPhotos folder : "
                        + e.toString());
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        LogUtil.d(LogUtil.LOG_TAG,
                "createImagesContentUriList all uriList.size() : " + uriList.size());

        return uriList;
    }

    public static File[] getlistFiles(String path) {
        File directory = null;
        try {
            directory = new File(path);
        } catch (NullPointerException npe) {
            LogUtil.d(LogUtil.LOG_TAG,
                    "getlistFiles NullPointerException occur : " + npe);
            return null;
        }

        // Check exist directory.
        if (!directory.exists()) {
            LogUtil.d(LogUtil.LOG_TAG, "Not exist save folder.");
            return null;
        }

        return directory.listFiles();
    }

    /**
     * Check whether available storage capacity is enough.
     * @param size size.
     * @return true if enough.
     */
    public static boolean isEnoughStorage(long size) {
        long availbleSize = getAvailableCapacity();
        LogUtil.v(LogUtil.LOG_TAG, "isEnoughStorage() availbleSize :" + availbleSize);
        if (availbleSize >= size + DEFAULT_THRESHOLD_MAX_BYTES) {
            return true;
        }
        return false;
    }

    /**
     * Get available storage capacity.
     * @return Available capacity.
     */
    public static long getAvailableCapacity() {
        long blockSize = 0l;
        long availableBlocks = 0l;
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().toString());
            blockSize = stat.getBlockSizeLong();
            availableBlocks = stat.getAvailableBlocksLong();
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG, "Exception occurs in getAvailableCapacity() :" + e);
        }

        return blockSize * availableBlocks;
    }

    public static void removePhoto(final Context context, final String localThumbPath,
            final int id, final String masterFilePath, final String ownerAddress) {
        LogUtil.v(LogUtil.LOG_TAG, "removePhoto()");
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtil.v(LogUtil.LOG_TAG, "removePhoto() run()");

                // Delete thumbnail file.
                PhotoUtils.deleteFile(localThumbPath);

                String localQueryPath = String.format("%s:%s", ownerAddress, masterFilePath);
                ProviderUtil.delete(context,
                        PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                        PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                        new String[] { localQueryPath });

                // Delete photo from db.
                Uri resultUri = ContentUris.withAppendedId(PhotoProvider.CONTENT_URI, id);
                int num = ProviderUtil.delete(context, resultUri, null, null);
                if (0 >= num) {
                    LogUtil.w(LogUtil.LOG_TAG, "Failed remove photo.");
                    return;
                }

                // Post remove photo.
                HashMap<String, String> param = new HashMap<String, String>();
                param.put(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, masterFilePath);
                param.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, ownerAddress);
                new PostPhotoContent(context).remove(param);
            }
        }).start();
    }

    public static void copyExifInterface(ExifInterface orgEx, ExifInterface newEx) {
        LogUtil.v(LogUtil.LOG_TAG, "copyExifInterface");

        // Set to new exif from original exif by exif tag.
        // TAG_IMAGE_LENGTH and TAG_IMAGE_WIDTH is not set because already set.
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_APERTURE);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_DATETIME);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_EXPOSURE_TIME);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_FLASH);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_FOCAL_LENGTH);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_ALTITUDE);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_ALTITUDE_REF);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_DATESTAMP);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_LATITUDE);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_LATITUDE_REF);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_LONGITUDE);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_LONGITUDE_REF);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_PROCESSING_METHOD);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_GPS_TIMESTAMP);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_ISO);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_MAKE);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_MODEL);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_ORIENTATION);
        setExifInterfaceValue(orgEx, newEx, ExifInterface.TAG_WHITE_BALANCE);
    }

    public static void setExifInterfaceValue(
            ExifInterface orgEx, ExifInterface newEx, String tag) {
        String value = orgEx.getAttribute(tag);
        if (value != null) {
            newEx.setAttribute(tag, value);
        }
    }

    public static void dumpExifInterface(ExifInterface ex, String msg) {
        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface START " + msg);

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_APERTURE : "
                + ex.getAttribute(ExifInterface.TAG_APERTURE));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_DATETIME : "
                + ex.getAttribute(ExifInterface.TAG_DATETIME));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_EXPOSURE_TIME : "
                + ex.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_FLASH : "
                + ex.getAttribute(ExifInterface.TAG_FLASH));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_FOCAL_LENGTH : "
                + ex.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_ALTITUDE : "
                + ex.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_ALTITUDE_REF : "
                + ex.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_DATESTAMP : "
                + ex.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_LATITUDE : "
                + ex.getAttribute(ExifInterface.TAG_GPS_LATITUDE));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_LATITUDE_REF : "
                + ex.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_LONGITUDE : "
                + ex.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_LONGITUDE_REF : "
                + ex.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_PROCESSING_METHOD : "
                + ex.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_GPS_TIMESTAMP : "
                + ex.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_IMAGE_LENGTH : "
                + ex.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_IMAGE_WIDTH : "
                + ex.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_ISO : "
                + ex.getAttribute(ExifInterface.TAG_ISO));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_MAKE : "
                + ex.getAttribute(ExifInterface.TAG_MAKE));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_MODEL : "
                + ex.getAttribute(ExifInterface.TAG_MODEL));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_ORIENTATION : "
                + ex.getAttribute(ExifInterface.TAG_ORIENTATION));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface TAG_WHITE_BALANCE : "
                + ex.getAttribute(ExifInterface.TAG_WHITE_BALANCE));

        LogUtil.v(LogUtil.LOG_TAG, "dumpExifInterface END " + msg);
    }
}