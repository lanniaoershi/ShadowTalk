/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.sonymobile.partyshare.httpd.NanoHTTPD.Response.IStatus;
import com.sonymobile.partyshare.httpd.NanoHTTPD.Response.Status;
import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.MusicProviderUtil;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.PhotoUtils;
import com.sonymobile.partyshare.util.Utility;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP server for PartyShare.
 */
public class PartyShareHttpd extends NanoHTTPD {
    /** Post number of party share http server. */
    public static final int PORT = 51000;
    /** Status code of playlist maximum. */
    public static final int STATUS_CODE_PLAYLIST_MAXIMUM = 700;
    /** Context. */
    private final Context mContext;
    /** Etag of content file. */
    private String mEtag = "";

    /** HTTP response status codes of playlist maximum. */
    private static enum PlaylistStatus implements IStatus {
        PLAYLIST_MAXIMUM(STATUS_CODE_PLAYLIST_MAXIMUM, "Playlist Maximum");

        private final int requestStatus;
        private final String description;

        PlaylistStatus(int requestStatus, String description) {
            this.requestStatus = requestStatus;
            this.description = description;
        }

        @Override
        public int getRequestStatus() {
            return this.requestStatus;
        }

        @Override
        public String getDescription() {
            return "" + this.requestStatus + " " + description;
        }
    }

    public PartyShareHttpd(Context context, int port, String hostName) throws IOException {
        super(hostName, port);
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd port : " + port);
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd hostName : " + hostName);
        mContext = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.serve uri : " + uri);
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.serve method : " + method);

        if (uri.isEmpty()) {
            return new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "uri is empty");
        }

        Map<String, String> files = new HashMap<String, String>();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException e) {
                return new Response(
                        Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "SERVER INTERNAL ERROR: IOException: " + e.getMessage());
            } catch (ResponseException e) {
                return new Response(e.getStatus(), MIME_PLAINTEXT, e.getMessage());
            }
        }

        Map<String, String> params = session.getParms();

        if (method.equals(Method.GET)) {
            if (uri.contains(MusicJsonFile.FILENAME) || uri.contains(PhotoJsonFile.FILENAME)) {
                return createResponse(uri, MIME_PLAINTEXT);
            } else if (uri.contains("/thumbnail/") || uri.contains("/file/")) {
                return createResponseForPhoto(uri, params, session.getHeaders());
            } else if (uri.contains("/music/")) {
                return createResponseForMusic(uri, session.getHeaders());
            } else {
                execute(getCommand(params), params);
            }
        } else if (method.equals(Method.POST)) {
            if (!ConnectionManager.isGroupOwner()) {
                return new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "Not Group owner");
            }

            int command = getCommand(params);
            if (command == PartyShareCommand.CMD_ADD_MUSIC_CONTENT) {
                if (MusicProviderUtil.isPlayListMaximum(mContext)) {
                    return new Response(
                            PlaylistStatus.PLAYLIST_MAXIMUM, MIME_PLAINTEXT,
                            "Playlist Maximum");
                }
            }
            execute(command, params);
        }

        // Response status is HTTP_OK.
        return new Response("Processing of server is successful");
    }

    /**
     * Create response which added the DataInputStream.
     * @param uri Server uri.
     * @param mimetype Content mimetype.
     * @return Response.
     */
    private Response createResponse(String uri, String mimetype) {
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(uri));
            return new Response(Status.OK, mimetype, dis);
        } catch (FileNotFoundException e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "PartyShareHttpd.createResponse FileNotFoundException : " + e.toString());
            return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, "File not found : " + uri);
        }
    }

    /**
     * Create response for photo.
     * @param uri Server uri.
     * @param params Content parameter.
     * @param header Http header.
     * @return Response.
     */
    private Response createResponseForPhoto(
            String uri, Map<String, String> params, Map<String, String> header) {
        Cursor cursor = null;
        try {
            String id = Uri.parse(uri).getLastPathSegment();
            if (uri.contains("/thumbnail/")) {
                cursor = ProviderUtil.query(
                        mContext, PhotoProvider.CONTENT_URI, null,
                        PhotoProvider._ID + " = ?", new String[] { id }, null);
            } else if (uri.contains("/file/")) {
                cursor = ProviderUtil.query(
                        mContext, PhotoProvider.CONTENT_URI_LOCAL_PHOTO, null,
                        PhotoProvider._ID + " = ?", new String[] { id }, null);
            }

            if (cursor == null || cursor.getCount() == 0) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PartyShareHttpd.createResponseForPhoto : data not found");
                return new Response(
                        Status.NOT_FOUND, MIME_PLAINTEXT, "Data not found : " + uri);
            }

            cursor.moveToFirst();

            String path = "";
            if (uri.contains("/thumbnail/")) {
                path = cursor.getString(cursor.getColumnIndex(
                        PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH));
            } else if (uri.contains("/file/")) {
                path = cursor.getString(cursor.getColumnIndex(
                        PhotoProvider.COLUMN_FILE_PATH));

                if (!existsFile(path)) {
                    LogUtil.d(LogUtil.LOG_TAG,
                            "PartyShareHttpd.createResponseForPhoto file does not exists.");
                    removePhoto(uri);
                    return new Response(
                            Status.NOT_FOUND, MIME_PLAINTEXT, "Media file does not exist.");
                }
            }

            if (params != null && !params.isEmpty()) {
                String event = params.get(PartyShareEvent.PARAM_EVENT_CODE);
                if (Integer.parseInt(event) == PartyShareEvent.EVENT_GET_FILESIZE) {
                    // in case request is size getting, so return response with size.
                    long size = getPhotoSize(path);
                    LogUtil.d(LogUtil.LOG_TAG,
                            "PartyShareHttpd.createResponseForPhoto file size : " + size);
                    return new Response(Status.OK, MIME_PLAINTEXT, String.valueOf(size));
                }
            }

            LogUtil.d(LogUtil.LOG_TAG,
                    "PartyShareHttpd.createResponseForPhoto path : " + path);
            if (!path.isEmpty()) {
                File file = new File(path);
                return serveFile(uri, header, file, "image/jpeg");
            } else {
                return new Response(
                    Status.NOT_FOUND, MIME_PLAINTEXT, "Data not found : " + uri);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Create response for music.
     * @param uri Server uri.
     * @param header Http header.
     * @return Response.
     */
    private Response createResponseForMusic(String uri, Map<String, String> header) {
        Cursor cursor = null;
        try {
            String localId = Uri.parse(uri).getLastPathSegment();
            cursor = ProviderUtil.query(
                    mContext, MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    null,
                    MusicProvider._ID + " = ?",
                    new String[] { localId }, null);

            if (cursor == null || cursor.getCount() == 0) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PartyShareHttpd.createResponseForMusic : data not found");
                return new Response(
                        Status.NOT_FOUND, MIME_PLAINTEXT, "Data not found : " + uri);
            }

            cursor.moveToFirst();
            String path = cursor.getString(
                    cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_LOCAL_PATH));
            String mimetype = cursor.getString(
                    cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_MIMETYPE));
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.createResponseForMusic path : " + path);
            LogUtil.d(LogUtil.LOG_TAG,
                    "PartyShareHttpd.createResponseForMusic mimetype : " + mimetype);

            if (!path.isEmpty()) {
                File file = new File(path);
                return serveFile(uri, header, file, mimetype);
            } else {
                return new Response(
                        Status.NOT_FOUND, MIME_PLAINTEXT, "Data not found : " + uri);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        DataInputStream dis = null;
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified()
                    + "" + file.length()).hashCode());
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.serveFile etag : " + etag);

            if (!mEtag.equals(etag)) {
                LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.serveFile mEtag : " + mEtag);
                header.put("range", null);
            }
            mEtag = etag;

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.serveFile range : " + range);

            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(
                            Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    dis = new DataInputStream(new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    });
                    long skip = dis.skip(startFrom);
                    LogUtil.d(LogUtil.LOG_TAG,
                            "PartyShareHttpd.serveFile skip request: " + startFrom);
                    LogUtil.d(LogUtil.LOG_TAG,
                            "PartyShareHttpd.serveFile skip actual : " + skip);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, dis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range",
                            "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match"))) {
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                } else {
                    res = createResponse(Response.Status.OK, mime,
                            new DataInputStream(new FileInputStream(file)));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (NumberFormatException e) {
            LogUtil.e(LogUtil.LOG_TAG, "PartyShareHttpd.serveFile : " + e.toString());
            res = getForbiddenResponse("Reading file failed.");
        } catch (IOException ioe) {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG, "PartyShareHttpd.serveFile : " + e.toString());
            }
            res = getForbiddenResponse("Reading file failed.");
        }

        return res;
    }

    private Response createResponse(Response.Status status, String mimeType, InputStream message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    protected Response getForbiddenResponse(String s) {
        return createResponse(
                Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    /**
     * Execute processing according to command and event.
     * @param params Content parameter.
     */
    private void execute(int command, Map<String, String> params) {
        if (params == null) {
            LogUtil.e(LogUtil.LOG_TAG, "PartyShareHttpd.execute : params is null");
            return;
        }

        try {
            switch (command) {
                case PartyShareCommand.CMD_ADD_MUSIC_CONTENT:
                    MusicPlayListController.addMusicContent(mContext, params);
                    break;
                case PartyShareCommand.CMD_REMOVE_MUSIC_CONTENT:
                    MusicPlayListController.removeMusic(mContext, params);
                    break;
                case PartyShareCommand.CMD_PLAY_NEXT:
                    String musicId = params.get(PartyShareCommand.PARAM_MUSIC_ID);
                    LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.execute musicId : " + musicId);
                    MusicPlayListController.playNextMusic(mContext, Integer.parseInt(musicId));
                    break;
                case PartyShareCommand.CMD_ADD_PHOTO_CONTENT:
                    PhotoListController.addPhotoContent(mContext, params);
                    break;
                case PartyShareCommand.CMD_REMOVE_PHOTO_CONTENT:
                    PhotoListController.removePhoto(mContext, params);
                    break;
                case PartyShareCommand.CMD_DELETE_CLIENT_DATA:
                    deleteClientData(params);
                    break;
                case PartyShareCommand.CMD_CLIENT_RECV_EVENT:
                    String event = params.get(PartyShareEvent.PARAM_EVENT_CODE);
                    params.remove(PartyShareEvent.PARAM_EVENT_CODE);
                    LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.execute event : " + event);

                    switch (Integer.parseInt(event)) {
                        case PartyShareEvent.EVENT_LOAD_PLAYLIST:
                            MusicPlayListController.reloadPlayList(mContext);
                            break;
                        case PartyShareEvent.EVENT_LOAD_PHOTOLIST:
                            PhotoListController.reloadPhotoList(mContext, false);
                            break;
                        case PartyShareEvent.EVENT_REMOVE_LOCAL_MUSIC:
                            MusicPlayListController.removeLocalMusic(mContext, params);
                            break;
                        case PartyShareEvent.EVENT_REMOVE_PHOTO:
                            PhotoListController.removePhoto(mContext, params);
                            break;
                        case PartyShareEvent.EVENT_REMOVE_LOCAL_PHOTO:
                            PhotoListController.removeLocalPhoto(mContext, params);
                            break;
                        default :
                            break;
                    }
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException e) {
           LogUtil.e(LogUtil.LOG_TAG, "PartyShareHttpd.execute : " + e.toString());
        }
    }

    /**
     * Get command from parameter.
     * @param params Content parameter.
     * @return command.
     */
    private int getCommand(Map<String, String> params) {
        int command = -1;
        try {
            if (params != null) {
                command = Integer.parseInt(params.get(PartyShareCommand.PARAM_CMD));
                params.remove(PartyShareCommand.PARAM_CMD);
            }
        } catch (NumberFormatException e) {
           LogUtil.e(LogUtil.LOG_TAG, "PartyShareHttpd.getCommand command : " + e.toString());
        }

        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.getCommand command : " + command);
        return command;
    }

    /**
     * Check whether the file exists.
     * @param path File path.
     * @return true if file exists, otherwise false.
     */
    private boolean existsFile(String path) {
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.existsFile path : " + path);
        File file = new File(path);
        return file.exists();
    }

    /**
     * Get photo file size.
     * @param path File path.
     * @return file size.
     */
    private long getPhotoSize(String path) {
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.getPhotoSize path : " + path);

        if (path.isEmpty()) {
            return -1;
        }

        File file = new File(path);
        if (!file.exists()) {
            return -1;
        }

        return file.length();
    }

    /**
     * Remove photo when shared data is deleted.
     * @param uri Server uri.
     */
    private void removePhoto(String uri) {
        Cursor cursor = null;
        try {
            String selection = PhotoProvider.COLUMN_OWNER_ADDRESS + " = ? AND ";
            if (uri.contains("/thumbnail/")) {
                selection += PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH + " = ?";
            } else if (uri.contains("/file/")) {
                selection += PhotoProvider.COLUMN_MASTER_FILE_PATH + " = ?";
            }

            String address = Utility.getMacAddress(mContext, ConnectionManager.getLocalAddress());
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.removePhoto address : " + address);
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.removePhoto uri     : " + uri);

            cursor = ProviderUtil.query(
                    mContext, PhotoProvider.CONTENT_URI,
                    new String[] {
                            PhotoProvider._ID,
                            PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH,
                            PhotoProvider.COLUMN_MASTER_FILE_PATH },
                    selection,
                    new String[] { address, uri },
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(
                        cursor.getColumnIndex(PhotoProvider._ID));
                String localThumbPath = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH));
                String masterFilePath = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_MASTER_FILE_PATH));

                LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.removePhoto id : " + id);
                LogUtil.d(LogUtil.LOG_TAG,
                        "PartyShareHttpd.removePhoto localThumbPath : " + localThumbPath);
                LogUtil.d(LogUtil.LOG_TAG,
                        "PartyShareHttpd.removePhoto masterFilePath : " + masterFilePath);

                PhotoUtils.removePhoto(
                        mContext, localThumbPath, id, masterFilePath, address);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Delete the data which the client shared from host.
     * @param param Content parameter.
     */
    private void deleteClientData(Map<String, String> param) {
        String address = param.get(PartyShareCommand.PARAM_OWNER_ADDRESS);
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.deleteClientData address : " + address);

        int musicCnt = ProviderUtil.delete(
                mContext, MusicProvider.CONTENT_URI,
                MusicProvider.COLUMN_OWNER_ADDRESS + " = ?",
                new String[] { address });

        if (musicCnt > 0) {
            MusicJsonFile.refreshJsonFile(mContext);
        }

        int photoCnt = ProviderUtil.delete(
                mContext, PhotoProvider.CONTENT_URI,
                PhotoProvider.COLUMN_OWNER_ADDRESS + " = ?",
                new String[] { address });

        if (photoCnt > 0) {
            JsonUtil.removeContent(mContext, JsonUtil.CONTENT_TYPE_PHOTO, param);
        }

        if (musicCnt > 0 || photoCnt > 0) {
            PartyShareEventNotifier.notifyEvent(
                    mContext, PartyShareEvent.EVENT_REMOVE_PHOTO, param);
        }
    }
}
