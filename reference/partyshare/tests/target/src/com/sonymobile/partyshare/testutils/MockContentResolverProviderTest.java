/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.testutils;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.test.IsolatedContext;

import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.PhotoProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * MockContentResolver for provider's test.
 */
public class MockContentResolverProviderTest extends android.test.mock.MockContentResolver {
    private static final String MUSIC_AUTHORITIES = "com.sonymobile.partyshare.music";
    private static final String PHOTO_AUTHORITIES = "com.sonymobile.partyshare.photo";

    private List<Uri> mNotifyList = new ArrayList<Uri>();

    public MockContentResolverProviderTest(Context context) {
        super(context);

        final Context providerContext = new IsolatedContext(this, context);
        MusicProvider mp = new MusicProvider();
        mp.attachInfo(providerContext, null);
        addProvider(MUSIC_AUTHORITIES, mp);

        PhotoProvider pp = new PhotoProvider();
        pp.attachInfo(providerContext, null);
        addProvider(PHOTO_AUTHORITIES, pp);
    }

    @Override
    public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
        mNotifyList.add(uri);
    }

    public boolean isNotifyChange(Uri uri) {
        return mNotifyList.contains(uri);
    }

    public void clearNotifyChangeHistory() {
        mNotifyList.clear();
    }
}
