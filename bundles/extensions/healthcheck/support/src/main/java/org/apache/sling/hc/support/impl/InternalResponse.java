/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.support.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class InternalResponse implements HttpServletResponse {

    private int status = 200;
    
    private static class DevNullOutputStream extends ServletOutputStream {
        @Override
        public void write(int b) {
        }
    }
    
    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new DevNullOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(new DevNullOutputStream());
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetBuffer() {
    }

    @Override
    public void setBufferSize(int arg0) {
    }

    @Override
    public void setCharacterEncoding(String arg0) {
    }

    @Override
    public void setContentLength(int arg0) {
    }

    @Override
    public void setContentType(String arg0) {
    }

    @Override
    public void setLocale(Locale arg0) {
    }

    @Override
    public void addCookie(Cookie arg0) {
    }

    @Override
    public void addDateHeader(String arg0, long arg1) {
    }

    @Override
    public void addHeader(String arg0, String arg1) {
    }

    @Override
    public void addIntHeader(String arg0, int arg1) {
    }

    @Override
    public boolean containsHeader(String arg0) {
        return false;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return url;
    }

    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeUrl(String url) {
        return url;
    }

    @Override
    public void sendError(int s, String arg1) throws IOException {
        status = s;
    }

    @Override
    public void sendError(int s) throws IOException {
        status = s;
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
    }

    @Override
    public void setDateHeader(String arg0, long arg1) {
    }

    @Override
    public void setHeader(String arg0, String arg1) {
    }

    @Override
    public void setIntHeader(String arg0, int arg1) {
    }

    @Override
    public void setStatus(int s, String arg1) {
        status = s;
    }

    @Override
    public void setStatus(int s) {
        status = s;
    }
    
    public int getStatus() {
        return status;
    }
}
