/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.junit.scriptable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/** Fake response used to acquire content from Sling */
public class HttpResponse implements HttpServletResponse {

    private int status = 200;
    private String message;
    private String encoding = "UTF-8";
    private String contentType;
    private final TestServletOutputStream outputStream;
    private final PrintWriter writer;

    HttpResponse() throws UnsupportedEncodingException {
        outputStream = new TestServletOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, encoding));
    }

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public void addDateHeader(String name, long date) {
    }

    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public void addIntHeader(String name, int value) {
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        status = sc;
        message = msg;
    }

    @Override
    public void sendError(int sc) throws IOException {
        status = sc;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
    }

    @Override
    public void setDateHeader(String name, long date) {
    }

    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public void setIntHeader(String name, int value) {
    }

    @Override
    public void setStatus(int sc, String sm) {
        status = sc;
        message = sm;
    }

    @Override
    public void setStatus(int sc) {
        status = sc;
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
        return encoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
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
    public void setBufferSize(int size) {
    }

    @Override
    public void setCharacterEncoding(String charset) {
        encoding = charset;
    }

    @Override
    public void setContentLength(int len) {
    }

    @Override
    public void setContentType(String type) {
        contentType = type;
    }

    @Override
    public void setLocale(Locale loc) {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public int getStatus() {
        return status;
    }

    public String getContent() {
        writer.flush();
        return outputStream.toString();
    }

    @Override
    public void setContentLengthLong(long len) {
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }
}
