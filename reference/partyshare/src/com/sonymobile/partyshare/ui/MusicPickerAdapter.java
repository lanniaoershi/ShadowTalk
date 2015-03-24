/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.net.Uri;

import com.sonymobile.partyshare.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/*
 * CursorAdapter for MusicPicker.
 */
public class MusicPickerAdapter extends CursorAdapter {

    public static class ViewHolder {
        public TextView text1;
        public TextView text2;
        public CheckBox checkbox;
        public String id;
    }

    // Saving checked state. key:ID value:isChecked.
    private final LinkedHashMap<String, Boolean> mMarkedList =
            new LinkedHashMap<String, Boolean>();

    MusicPickerAdapter(Context context, Cursor cursor) {
        super(context, cursor, true);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder vh = (ViewHolder)view.getTag();
        vh.text1.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        vh.text2.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
        vh.id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        vh.checkbox.setChecked(isCheckedCheckbox(vh.id));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = View.inflate(context, R.layout.music_picker_listitem, null);
        ViewHolder vh = new ViewHolder();
        view.setTag(vh);
        view.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ViewHolder vh = (ViewHolder)view.getTag();
                boolean newCheck = !vh.checkbox.isChecked();
                vh.checkbox.setChecked(newCheck);
                mMarkedList.put(vh.id, Boolean.valueOf(newCheck));
            }
        });

        vh.text1 = (TextView)view.findViewById(R.id.text1);
        vh.text2 = (TextView)view.findViewById(R.id.text2);
        vh.checkbox = (CheckBox)view.findViewById(R.id.checkbox);

        return view;
    }

    private boolean isCheckedCheckbox(String id) {
        boolean ret = false;
        if (mMarkedList.containsKey(id)) {
            ret = mMarkedList.get(id).booleanValue();
        }
        return ret;
    }

    public ArrayList<Parcelable> getMarkedUriList() {
        ArrayList<Parcelable> markedUriList = new ArrayList<Parcelable>();
        for (Entry<String, Boolean> item : mMarkedList.entrySet()) {
            if (item.getValue().booleanValue()) {
                Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        item.getKey());
                markedUriList.add(uri);
            }
        }
        return markedUriList;
    }
}
