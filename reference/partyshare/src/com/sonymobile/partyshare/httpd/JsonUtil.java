/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.Context;

import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for JSON file.
 */
public class JsonUtil {
    /** Content type of music. */
    public static final int CONTENT_TYPE_MUSIC = 0;
    /** Content type of photo. */
    public static final int CONTENT_TYPE_PHOTO = 1;

    /**
     * Get json object from host.
     * @param context Context.
     * @param type Content type.
     * @return object JSONObject.
     */
    public static ArrayList<JSONObject> getJsonObjectFromHost(Context context, int type) {
        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.getJsonObjectFromHost type : " + type);

        String filename = null;
        String jsonArray = null;
        switch (type) {
            case CONTENT_TYPE_MUSIC:
                filename = MusicJsonFile.FILENAME;
                jsonArray = MusicJsonFile.KEY_JSON_ARRAY;
                break;
            case CONTENT_TYPE_PHOTO:
                filename = PhotoJsonFile.FILENAME;
                jsonArray = PhotoJsonFile.KEY_JSON_ARRAY;
                break;
            default:
                break;
        }

        String jsonUrl = String.format("http://%s:%s%s",
                ConnectionManager.getGroupOwnerAddress(),
                PartyShareHttpd.PORT,
                context.getFilesDir() + "/" + filename);
        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.getJsonObjectFromHost jsonUrl : " + jsonUrl);

        JSONObject json = null;
        HttpURLConnection http = null;
        BufferedReader br = null;
        try {
            URL url = new URL(jsonUrl);
            http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("GET");
            http.setConnectTimeout(5000);
            http.setReadTimeout(5000);
            http.connect();
            boolean result = (http.getResponseCode() == HttpURLConnection.HTTP_OK);
            if (!result) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "JsonUtil.getJsonObjectFromHost http error : " + http.getResponseCode());
                return null;
            }

            StringBuffer jsonData = new StringBuffer();
            br = new BufferedReader(
                    new InputStreamReader(http.getInputStream(), Charset.forName("UTF-8")));
            String line = "";
            while ((line = br.readLine()) != null) {
                jsonData.append(line);
            }
            json = new JSONObject(jsonData.toString());
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "JsonUtil.getJsonObjectFromHost : " + e.toString());
        } catch (JSONException e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "JsonUtil.getJsonObjectFromHost : " + e.toString());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.getJsonObjectFromHost : " + e.toString());
                }
            }
            if (http != null) {
                http.disconnect();
                http = null;
            }
        }

        ArrayList<JSONObject> object = new ArrayList<JSONObject>();
        if (json != null) {
            try {
                JSONArray array = json.getJSONArray(jsonArray);
                for (int i = 0; i < array.length(); i++) {
                    object.add(array.getJSONObject(i));
                }
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "JsonUtil.getJsonObjectFromHost : " + e.toString());
            }
        }
        return object;
    }

    /**
     * Load JSONObject from json file.
     * @param jsonFile JsonFile.
     * @return object JSONObject.
     */
    public static JSONObject loadJsonFile(File jsonFile) {
        StringBuffer jsonData = new StringBuffer();
        JSONObject object = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(jsonFile), Charset.forName("UTF-8")));
            String line = "";
            while ((line = br.readLine()) != null) {
                jsonData.append(line);
            }
            object = new JSONObject(jsonData.toString());
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.loadJsonFile : " +  e.toString());
        } catch (JSONException e) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.loadJsonFile : " +  e.toString());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.loadJsonFile : " +  e.toString());
            }
        }
        return object;
    }

    /**
     * Add content to json file.
     * @param context Context.
     * @param type Content type.
     * @param param Content parameter.
     * @return true if success, otherwise false.
     */
    public static synchronized boolean addContent(
            Context context, int type, Map<String, String> param) {
        File jsonFile = getJsonFile(context, type);
        if (jsonFile == null) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.addContent : json file error");
            return false;
        }
        if (!jsonFile.exists()) {
            if (!createJsonFile(context, type, jsonFile)) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.addContent : file create error");
                return false;
            }
        }

        boolean ret = false;
        JSONObject json = loadJsonFile(jsonFile);
        if (json != null) {
            try {
                JSONArray array = json.getJSONArray(getArrayName(type));
                array.put(new JSONObject(param));
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.addContent : " + e.toString());
            }
            ret = writeToFile(json, jsonFile);
        }

        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.addContent return " + ret);
        return ret;
    }

    /**
     * Update object of JSONArray which matches search key.
     * @param context Context.
     * @param type Content type.
     * @param searchParam Search key and value.
     * @param updateParam Update key and value.
     */
    public static synchronized void updateContent(Context context, int type,
            Map<String, String> searchParam, Map<String, String> updateParam) {
        List<String> searchKey = new ArrayList<String>();
        List<String> searchValue = new ArrayList<String>();

        Set<Map.Entry<String, String>> searchEntrySet = searchParam.entrySet();
        Iterator<Map.Entry<String, String>> searchIterator = searchEntrySet.iterator();
        while (searchIterator.hasNext()) {
            Map.Entry<String, String> entry = searchIterator.next();
            searchKey.add(entry.getKey());
            searchValue.add(entry.getValue());
        }

        List<String> updateKey = new ArrayList<String>();
        List<String> updateValue = new ArrayList<String>();

        Set<Map.Entry<String, String>> updateEntrySet = updateParam.entrySet();
        Iterator<Map.Entry<String, String>> udpateIterator = updateEntrySet.iterator();
        while (udpateIterator.hasNext()) {
            Map.Entry<String, String> entry = udpateIterator.next();
            updateKey.add(entry.getKey());
            updateValue.add(entry.getValue());
        }

        File jsonFile = getJsonFile(context, type);
        if (jsonFile == null) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.updateContent : json file error");
            return;
        }
        if (!jsonFile.exists()) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.updateContent : file is not exists.");
            return;
        }

        JSONObject json = loadJsonFile(jsonFile);
        if (json != null) {
            try {
                JSONArray array = json.getJSONArray(getArrayName(type));
                for (int cnt = 0; cnt < array.length(); cnt++) {
                    boolean isUpdate = true;
                    JSONObject object = array.getJSONObject(cnt);
                    for (int i = 0; i < searchKey.size(); i++) {
                        String value = object.getString(searchKey.get(i));
                        if (!value.equals(searchValue.get(i))) {
                            isUpdate = false;
                            break;
                        }
                    }

                    if (!isUpdate) {
                        continue;
                    }

                    for (int j = 0; j < updateKey.size(); j++) {
                        object.remove(updateKey.get(j));
                        object.put(updateKey.get(j), updateValue.get(j));
                    }
                }
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.updateContent : " + e.toString());
            }
            writeToFile(json, jsonFile);
        }
    }

    /**
     * Remove object from JSONArray which matches both key and value.
     * @param context Context.
     * @param type Content type.
     * @param param Remove key and value.
     * @return true if success, otherwise false.
     */
    public static synchronized boolean removeContent(
            Context context, int type, Map<String, String> param) {
        List<String> key = new ArrayList<String>();
        List<String> value = new ArrayList<String>();

        Set<Map.Entry<String, String>> entrySet = param.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            key.add(entry.getKey());
            value.add(entry.getValue());
        }

        File jsonFile = getJsonFile(context, type);
        if (jsonFile == null || !jsonFile.exists()) {
            LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.removeContent : no json file");
            return false;
        }

        boolean ret = false;
        JSONObject json = loadJsonFile(jsonFile);
        if (json != null) {
            try {
                JSONArray array = json.getJSONArray(getArrayName(type));
                int i = 0;
                while (i < array.length()) {
                    boolean isRemove = true;
                    JSONObject objct = array.getJSONObject(i);
                    for (int cnt = 0; cnt < key.size(); cnt++) {
                        if (!objct.get(key.get(cnt)).equals(value.get(cnt))) {
                            isRemove = false;
                            break;
                        }
                    }
                    if (isRemove) {
                        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.removeContent : remove data");
                        array.remove(i);
                        i--;
                    }
                    i++;
                }
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.removeContent : " + e.toString());
            }
            ret = writeToFile(json, jsonFile);
        }

        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.removeContent return " + ret);
        return ret;
    }

    /**
     * Delete all object from json file.
     * @param context Context.
     * @param type Content type.
     * @return true if success, otherwise false.
     */
    public static synchronized boolean clearJsonFile(Context context, int type) {
        File jsonFile = getJsonFile(context, type);
        if (jsonFile == null || !jsonFile.exists()) {
            LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.clearJsonFile : no file");
            return false;
        }

        boolean ret = false;
        JSONObject json = loadJsonFile(jsonFile);
        if (json != null) {
            try {
                JSONArray array = json.getJSONArray(getArrayName(type));
                for (int i = array.length() - 1; i >= 0; i--) {
                    array.remove(i);
                }
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.clearJsonFile : " + e.toString());
            }
            ret = writeToFile(json, jsonFile);
        }

        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.clearJsonFile return " + ret);
        return ret;
    }

    /**
     * Delete json file.
     * @param context Context.
     * @param type Content type.
     */
    public static void deleteJsonFile(Context context, int type) {
        File jsonFile = getJsonFile(context, type);
        if (jsonFile == null || !jsonFile.exists()) {
            LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.deleteJsonFile : no file");
            return;
        }

        if (!jsonFile.delete()) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.deleteJsonFile : delete error");
        }
    }

    /**
     * Write JSONObject to json file.
     * @param object JSONObject.
     * @param jsonFile JsonFile.
     * @return true when writing of json file is successful, otherwise false.
     */
    private static boolean writeToFile(JSONObject object, File jsonFile) {
        boolean result = false;
        FileOutputStream fos = null;
        try {
            File tmpFile = new File(jsonFile.getParentFile(), "tmp");
            if (tmpFile.exists() && !tmpFile.delete()) {
                LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.writeToFile : delete error");
            }

            fos = new FileOutputStream(tmpFile);
            String data = object.toString(4);
            fos.write(data.getBytes(Charset.forName("UTF-8")));

            if (jsonFile.exists() && !jsonFile.delete()) {
                LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.writeToFile : delete error");
            }

            result = tmpFile.renameTo(jsonFile);
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.writeToFile : " + e.toString());
        } catch (JSONException e) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.writeToFile : " + e.toString());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.writeToFile : " + e.toString());
            }
        }

        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.writeToFile result : " + result);
        return result;
    }

    /**
     * Create json file.
     * @param context Context.
     * @param type Content type.
     * @param jsonFile JsonFile.
     * @return true when file creation is successful, otherwise false.
     */
    private static boolean createJsonFile(Context context, int type, File jsonFile) {
        boolean result = false;
        Writer writer = null;
        PrintWriter pw = null;
        try {
            if (jsonFile.createNewFile()) {
                writer = new OutputStreamWriter(new FileOutputStream(jsonFile), "UTF-8");
                pw = new PrintWriter(writer);
                pw.println("{");
                pw.println("    \"" + getArrayName(type) + "\":[");
                pw.println("    ]");
                pw.println("}");
                return true;
            } else {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.createJsonFile : create error");
            }
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.createJsonFile : " + e.toString());
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG, "JsonUtil.createJsonFile : " + e.toString());
            }
        }

        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.writeToFile result : " + result);
        return result;
    }

    /**
     * Get json file.
     * @param context Context.
     * @param type Content type.
     * @return jsonFile Json file.
     */
    private static File getJsonFile(Context context, int type) {
        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.getJsonFile type : " + type);

        File jsonFile = null;
        switch (type) {
            case CONTENT_TYPE_MUSIC:
                jsonFile = new File(context.getFilesDir(), MusicJsonFile.FILENAME);
                break;
            case CONTENT_TYPE_PHOTO:
                jsonFile = new File(context.getFilesDir(), PhotoJsonFile.FILENAME);
                break;
            default:
                break;
        }
        return jsonFile;
    }

    /**
     * Get json array name.
     * @param type Content type.
     * @return arrayName JSONArray name.
     */
    private static String getArrayName(int type) {
        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.getArrayName type : " + type);

        String arrayName = null;
        switch (type) {
            case CONTENT_TYPE_MUSIC:
                arrayName = MusicJsonFile.KEY_JSON_ARRAY;
                break;
            case CONTENT_TYPE_PHOTO:
                arrayName = PhotoJsonFile.KEY_JSON_ARRAY;
                break;
            default:
                break;
        }

        LogUtil.d(LogUtil.LOG_TAG, "JsonUtil.getArrayName arryaName : " + arrayName);
        return arrayName;
    }
}
