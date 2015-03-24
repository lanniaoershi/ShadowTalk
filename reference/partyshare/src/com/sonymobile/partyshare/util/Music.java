/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.util;

public class Music {
    private String mMusicTitle;
    private String mArtistName;
    private String mOwner;
    private String mOwnerName;
    private int mTime;
    private String mUri;
    private int mId = -1;
    private int mOrder = 0;

    public Music(String musicTitle, String artistName, String owner, String name,
            int time, String uri) {
        mMusicTitle = musicTitle;
        mArtistName = artistName;
        mOwner = owner;
        mOwnerName = name;
        mTime = time;
        mUri = uri;
    }

    public Music(String musicTitle, String artistName, String owner, String name,
            int time, String uri, int id, int order) {
        mMusicTitle = musicTitle;
        mArtistName = artistName;
        mOwner = owner;
        mOwnerName = name;
        mTime = time;
        mUri = uri;
        mId = id;
        mOrder = order;
    }

    public String getMusicTitle() {
        return mMusicTitle;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getOwner() {
        return mOwner;
    }

    public String getName() {
        return mOwnerName;
    }

    public int getTime() {
        return mTime;
    }

    public String getURI() {
        return mUri;
    }

    public int getId() {
        return mId;
    }

    public void setOrder(int reqOrder) {
        mOrder = reqOrder;
    }

    public int getOrder() {
        return mOrder;
    }
}
