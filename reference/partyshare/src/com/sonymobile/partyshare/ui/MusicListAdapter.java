/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.httpd.MusicPlayListController;
import com.sonymobile.partyshare.httpd.PartyShareCommand;
import com.sonymobile.partyshare.httpd.PostMusicContent;
import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.service.MusicService;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.Utility;

import java.util.HashMap;

public class MusicListAdapter extends SimpleCursorAdapter {

    private Context mContext;
    private int mLayout;
    private LayoutInflater mInflater;
    private PopupMenu mPopup;

    public MusicListAdapter(Context context, int layout, Cursor c,
            String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        mLayout = layout;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(mLayout, null);
        ViewHolder holder = new ViewHolder();
        holder.musicTitle = (TextView)view.findViewById(R.id.text_playlist_music_title);
        holder.artistName = (TextView)view.findViewById(R.id.text_playlist_artist_name);
        holder.ownerName = (TextView)view.findViewById(R.id.text_playlist_owner_name);
        holder.albumArt = (ImageView)view.findViewById(R.id.image_playlist_album_art);
        holder.menuView = view.findViewById(R.id.view_playlist_option_menu);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(final View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        ViewHolder holder = (ViewHolder) view.getTag();
        String title =
                cursor.getString(cursor.getColumnIndexOrThrow(MusicProvider.COLUMN_TITLE));
        String artist =
                cursor.getString(cursor.getColumnIndexOrThrow(MusicProvider.COLUMN_ARTIST));
        final String owner =
                cursor.getString(cursor.getColumnIndexOrThrow(MusicProvider.COLUMN_OWNER_ADDRESS));
        String ownerName =
                cursor.getString(cursor.getColumnIndexOrThrow(MusicProvider.COLUMN_OWNER_NAME));

        if (title == null || title.isEmpty()) {
            title = mContext.getResources()
                    .getString(R.string.party_share_strings_unknown_txt);
        }
        if (artist == null || artist.isEmpty()) {
            artist = mContext.getResources()
                    .getString(R.string.party_share_strings_unknown_txt);
        }

        holder.musicTitle.setText(title);
        holder.artistName.setText(artist);

        holder.ownerName.setText(String.format(mContext.getResources()
                .getString(R.string.party_share_strings_music_list_add_by_txt), ownerName));

        holder.playlistId =
                cursor.getInt(cursor.getColumnIndexOrThrow(MusicProvider.COLUMN_MUSIC_ID));
        int isPlaying =
                cursor.getInt(cursor.getColumnIndexOrThrow(MusicProvider.COLUMN_STATUS));

        if (isPlaying == MusicProvider.MUSIC_STATUS_FOCUSED) {
            holder.musicTitle.setTextAppearance(context, R.style.MusicListFocusedMedium);
            holder.artistName.setTextAppearance(context, R.style.MusicListFocusedSmall);
            holder.ownerName.setTextAppearance(context, R.style.MusicListFocusedSmall);
        } else {
            holder.musicTitle.setTextAppearance(context, R.style.MusicListMedium);
            holder.artistName.setTextAppearance(context, R.style.MusicListSmall);
            holder.ownerName.setTextAppearance(context, R.style.MusicListSmall);
        }

        // get resId of albumArt
        int resId = Utility.getAlbumArtResId(mContext, title);

        if(holder.albumId != resId) {
            holder.albumId = resId;
            holder.albumArt.setImageDrawable(null);
            holder.albumArt.setImageDrawable(mContext.getResources().getDrawable(resId));
        }

        final int musicId = holder.playlistId;
        boolean showDelete = true;
        if (!ConnectionManager.isGroupOwner()) {
            // Set if deletable
            showDelete = ConnectionManager.getMyAddress().equals(owner);
        }

        final boolean isShowDelete = showDelete;

        if (isShowPopupMenu(musicId, isShowDelete)) {
            holder.menuView.setVisibility(View.VISIBLE);
        } else {
            holder.menuView.setVisibility(View.INVISIBLE);
        }

        holder.menuView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show menu.
                showPopupMenu(view, musicId, isShowDelete);
            }
        });
    }

    private void showPopupMenu(View view, final int musicId,
            final boolean isShowDelete) {
        mPopup = new PopupMenu(mContext, view, Gravity.RIGHT);
        boolean isCurrent = musicId == MusicFragment.getCurrentId();

        if (ConnectionManager.isGroupOwner()) {
            mPopup.getMenuInflater().inflate(R.menu.music_list_popup_owner,
                    mPopup.getMenu());
            if (isCurrent) {
                mPopup.getMenu().getItem(0).setVisible(false);
            }
        } else {
            mPopup.getMenuInflater().inflate(R.menu.music_list_popup_client,
                    mPopup.getMenu());
            if (isCurrent) {
                mPopup.getMenu().getItem(0).setVisible(false);
            }
            if (!isShowDelete) {
                mPopup.getMenu().getItem(1).setVisible(false);
            }
        }

        mPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                case R.id.delete_music:
                    MusicPlayListController.removeLocalMusic(mContext, musicId);
                    if (ConnectionManager.isGroupOwner()) {
                        intent = new Intent(mContext, MusicService.class);
                        intent.setAction(MusicService.ACTION_REMOVE);
                        intent.putExtra("delete_id", musicId);
                        mContext.startService(intent);
                    } else {
                        HashMap<String, String> param = new HashMap<String, String>();
                        param.put(PartyShareCommand.PARAM_MUSIC_ID,
                                String.valueOf(musicId));
                        new PostMusicContent(mContext).remove(param);
                    }
                    break;

                case R.id.play_next:
                    if (ConnectionManager.isGroupOwner()) {
                        intent = new Intent(mContext, MusicService.class);
                        intent.putExtra("play_next", musicId);
                        intent.setAction(MusicService.ACTION_PLAY_NEXT);
                        mContext.startService(intent);
                    } else {
                        new PostMusicContent(mContext).playNext(musicId);
                    }
                    break;
                default:
                    break;
                }
                return true;
            }
        });
        mPopup.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(PopupMenu menu) {
                mPopup = null;
            }
        });
        mPopup.show();
    }

    private boolean isShowPopupMenu(int musicId, final boolean isShowDelete) {
        boolean isShowPopupMenu = true;

        if (!ConnectionManager.isGroupOwner()
                && musicId == MusicFragment.getCurrentId() && !isShowDelete) {
            isShowPopupMenu = false;
        }

        return isShowPopupMenu;
    }

    public PopupMenu getPopup() {
        return mPopup;
    }

    public static class ViewHolder {
        TextView musicTitle;
        TextView artistName;
        TextView ownerName;
        ImageView albumArt;
        View menuView;
        int albumId = 0;
        int playlistId = -1;
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
    }
}
