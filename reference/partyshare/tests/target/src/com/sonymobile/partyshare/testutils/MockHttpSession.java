/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.testutils;

import com.sonymobile.partyshare.httpd.NanoHTTPD.CookieHandler;
import com.sonymobile.partyshare.httpd.NanoHTTPD.IHTTPSession;
import com.sonymobile.partyshare.httpd.NanoHTTPD.Method;
import com.sonymobile.partyshare.httpd.NanoHTTPD.ResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MockHttpSession implements IHTTPSession {
    private Method mMethod;
    private String mUri;
    private Map<String, String> mHeader;
    private Map<String, String> mParams;

    @Override
    public void execute() throws IOException {
    }

    @Override
    public CookieHandler getCookies() {
        return null;
    }

    @Override
    public Map<String, String> getHeaders() {
        return mHeader;
    }

    public void setHeaders(Map<String, String> header) {
        mHeader = header;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public Method getMethod() {
        return mMethod;
    }

    public void setMethod(Method method) {
        mMethod = method;
    }

    @Override
    public Map<String, String> getParms() {
        return mParams;
    }

    public void setParams(Map<String, String> params) {
        mParams = params;
    }

    @Override
    public String getQueryParameterString() {
        return null;
    }

    @Override
    public String getUri() {
        return mUri;
    }

    public void setUri(String uri) {
        mUri = uri;
    }

    @Override
    public void parseBody(Map<String, String> files) throws IOException,
            ResponseException {
    }
}
