/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.testing.sling;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletResponse;

public class MockSlingHttpServletResponse implements SlingHttpServletResponse {

	private StringBuffer output = new StringBuffer();
	private String contentType;
	private String encoding;
	private int status = SC_OK;

	public StringBuffer getOutput() {
		return output;
	}

	@Override
    public void addCookie(Cookie cookie) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addCookie");
	}

	@Override
    public boolean containsHeader(String s) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".containsHeader");
	}

	@Override
    public String encodeURL(String s) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeURL");
	}

	@Override
    public String encodeRedirectURL(String s) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeRedirectURL");
	}

	@Override
    @SuppressWarnings("deprecation")
    @Deprecated
    public String encodeUrl(String s) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeUrl");
	}

	@Override
    @SuppressWarnings("deprecation")
    public String encodeRedirectUrl(String s) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeRedirectUrl");
	}

	@Override
    public void sendError(int i, String s) throws IOException {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".sendError");
	}

	@Override
    public void sendError(int i) throws IOException {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".sendError");
	}

	@Override
    public void sendRedirect(String s) throws IOException {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".sendRedirect");
	}

	@Override
    public void setDateHeader(String s, long l) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setDateHeader");
	}

	@Override
    public void addDateHeader(String s, long l) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addDateHeader");
	}

	@Override
    public void setHeader(String s, String s1) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setHeader");
	}

	@Override
    public void addHeader(String s, String s1) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addHeader");
	}

	@Override
    public void setIntHeader(String s, int i) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setIntHeader");
	}

	@Override
    public void addIntHeader(String s, int i) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addIntHeader");
	}

	@Override
    public void setStatus(int i) {
		this.status = i;
	}

	@Override
    @SuppressWarnings("deprecation")
    @Deprecated
    public void setStatus(int i, String s) {
		this.status = i;
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
    public ServletOutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".getOutputStream");
	}

	@Override
    public PrintWriter getWriter() throws IOException {
		MockSlingHttpServletResponse.MockWriter writer = new MockWriter(output);
		return new PrintWriter(writer);
	}

	@Override
    public void setCharacterEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
    public void setContentLength(int i) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setContentLength");
	}

	@Override
    public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
    public void setBufferSize(int i) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setBufferSize");
	}

	@Override
    public int getBufferSize() {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".getBufferSize");
	}

	@Override
    public void flushBuffer() throws IOException {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".flushBuffer");
	}

	@Override
    public void resetBuffer() {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".resetBuffer");
	}

	@Override
    public boolean isCommitted() {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".isCommitted");
	}

	@Override
    public void reset() {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".reset");
	}

	@Override
    public void setLocale(Locale locale) {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setLocale");
	}

	@Override
    public Locale getLocale() {
		throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".getLocale");
	}

	@Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> adapterTypeClass) {
		return null;
	}

	@Override
    public int getStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getHeader(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setContentLengthLong(long len) {
        // TODO Auto-generated method stub

    }

    private class MockWriter extends Writer {
		private StringBuffer buf;

		public MockWriter(StringBuffer output) {
			buf = output;
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			buf.append(cbuf, off, len);
		}

		@Override
		public void flush() throws IOException {
			buf.setLength(0);
		}

		@Override
		public void close() throws IOException {
			buf = null;
		}
	}
}
