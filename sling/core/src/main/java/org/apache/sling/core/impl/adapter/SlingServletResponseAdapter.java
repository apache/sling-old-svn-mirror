/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.core.impl.adapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

/**
 * The <code>SlingServletResponseAdapter</code> class is a
 * <code>ComponentResponseWrapper</code> which does not delegate to a wrapped
 * <code>ComponentResponse</code> but to a wrapped
 * <code>HttpServletResponse</code>. This is required if any user of the
 * <code>RequestDispatcher.include</code> method uses a
 * <code>HttpServletResponseWrapper</code> instead of a
 * <code>ComponentResponseWrapper</code>. One such case is the Jasper runtime
 * which does this.
 */
public class SlingServletResponseAdapter extends SlingHttpServletResponseWrapper {

    private final HttpServletResponse response;

    public SlingServletResponseAdapter(SlingHttpServletResponse delegatee,
            HttpServletResponse response) {
        super(delegatee);
        this.response = response;
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        response.setCharacterEncoding(charset);
    }

    @Override
    public void addCookie(Cookie cookie) {
        response.addCookie(cookie);
    }

    @Override
    public void addDateHeader(String name, long date) {
        response.addDateHeader(name, date);
    }

    @Override
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        response.addIntHeader(name, value);
    }

    @Override
    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return response.encodeRedirectUrl(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return response.encodeRedirectURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return response.encodeUrl(url);
    }

    @Override
    public String encodeURL(String url) {
        return response.encodeURL(url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        response.sendError(sc, msg);
    }

    @Override
    public void sendError(int sc) throws IOException {
        response.sendError(sc);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        response.sendRedirect(location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        response.setDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        response.setIntHeader(name, value);
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm) {
        response.setStatus(sc, sm);
    }

    @Override
    public void setStatus(int sc) {
        response.setStatus(sc);
    }

    @Override
    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    @Override
    public int getBufferSize() {
        return response.getBufferSize();
    }

    @Override
    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public ServletResponse getResponse() {
        return response;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    @Override
    public boolean isCommitted() {
        return response.isCommitted();
    }

    @Override
    public void reset() {
        response.reset();
    }

    @Override
    public void resetBuffer() {
        response.resetBuffer();
    }

    @Override
    public void setBufferSize(int size) {
        response.setBufferSize(size);
    }

    @Override
    public void setContentLength(int len) {
        response.setContentLength(len);
    }

    @Override
    public void setContentType(String type) {
        response.setContentType(type);
    }

    @Override
    public void setLocale(Locale loc) {
        response.setLocale(loc);
    }
}
