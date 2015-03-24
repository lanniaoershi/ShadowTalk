/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.runners;

import com.sonymobile.partyshare.httpd.UPartyShareHttpd;
import com.sonymobile.partyshare.provider.UMusicProviderTest;
import com.sonymobile.partyshare.provider.UPhotoProviderTest;
import com.sonymobile.partyshare.ui.UDownloadModeDialog;
import com.sonymobile.partyshare.ui.USortOrderPhotoDialog;
import com.sonymobile.partyshare.util.UPhotoUtils;

import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;
import junit.framework.TestSuite;

public class UnitTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getAllTests() {
        TestSuite suit = new InstrumentationTestSuite(this);
        suit.addTestSuite(UMusicProviderTest.class);
        suit.addTestSuite(UPhotoProviderTest.class);
        suit.addTestSuite(UDownloadModeDialog.class);
        suit.addTestSuite(USortOrderPhotoDialog.class);
        suit.addTestSuite(UPartyShareHttpd.class);
        suit.addTestSuite(UPhotoUtils.class);
        return suit;
    }

    @Override
    public ClassLoader getLoader() {
        return UnitTestRunner.class.getClassLoader();
    }
}
