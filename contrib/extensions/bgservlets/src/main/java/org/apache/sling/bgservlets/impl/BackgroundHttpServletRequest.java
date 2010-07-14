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
package org.apache.sling.bgservlets.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class BackgroundHttpServletRequest implements SlingHttpServletRequest {

	private final String contextPath;
	private final String method;
	private final String pathInfo;
	private final RequestProgressTracker requestProgressTracker;
	
	private final Map<String, Object> attributes;
	private final Map<String, ?> parameters;
	
	static class IteratorEnumeration<T> implements Enumeration<T> {
		private final Iterator<T> it;
		IteratorEnumeration(Iterator<T> it) {
			this.it = it;
		}
		public boolean hasMoreElements() {
			return it.hasNext();
		}
		public T nextElement() {
			return it.next();
		}
	}
	
	/** We throw this for any method for which we do not have data that's
	 * 	safe to use outside of the container's request/response cycle.
	 * 	Start by throwing this everywhere and implement methods as needed,
	 * 	if their data is safe to use.
	 */
	@SuppressWarnings("serial")
	class UnsupportedBackgroundOperationException extends UnsupportedOperationException {
		UnsupportedBackgroundOperationException() {
			super("This operation is not supported for background requests");
		}
	}
	
	@SuppressWarnings("unchecked")
	BackgroundHttpServletRequest(HttpServletRequest r) {
		
		final SlingHttpServletRequest sr = (r instanceof SlingHttpServletRequest ? (SlingHttpServletRequest)r : null);
		
		// Store objects which are safe to use outside
		// of the container's request/response cycle - the
		// goal is to release r once this request starts
		// executing in the background
		contextPath = r.getContextPath();
		method = r.getMethod();
		pathInfo = r.getPathInfo();
		
		requestProgressTracker = (sr == null ? null : sr.getRequestProgressTracker());
		
		attributes = new HashMap<String, Object>();
		final Enumeration<?> e = r.getAttributeNames();
		while(e.hasMoreElements()) {
			final String key = (String)e.nextElement();
			attributes.put(key, r.getAttribute(key));
		}
		
		parameters = new HashMap<String, String>();
		parameters.putAll(r.getParameterMap());
	}
	
	public String getAuthType() {
		return null;
	}

	public String getContextPath() {
		return contextPath;
	}

	public Cookie[] getCookies() {
		return null;
	}

	public long getDateHeader(String arg0) {
		return 0;
	}

	public String getHeader(String arg0) {
		return null;
	}

	public Enumeration<?> getHeaderNames() {
		return null;
	}

	public Enumeration<?> getHeaders(String name) {
		return null;
	}

	public int getIntHeader(String name) {
		return 0;
	}

	public String getMethod() {
		return method;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getPathTranslated() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getQueryString() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getRemoteUser() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getRequestedSessionId() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getRequestURI() {
		throw new UnsupportedBackgroundOperationException();
	}

	public StringBuffer getRequestURL() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getServletPath() {
		throw new UnsupportedBackgroundOperationException();
	}

	public HttpSession getSession() {
		throw new UnsupportedBackgroundOperationException();
	}

	public HttpSession getSession(boolean arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public Principal getUserPrincipal() {
		throw new UnsupportedBackgroundOperationException();
	}

	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedBackgroundOperationException();
	}

	public boolean isRequestedSessionIdFromUrl() {
		throw new UnsupportedBackgroundOperationException();
	}

	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedBackgroundOperationException();
	}

	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedBackgroundOperationException();
	}

	public boolean isUserInRole(String arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public Enumeration<?> getAttributeNames() {
		return new IteratorEnumeration<String>(attributes.keySet().iterator());
	}

	public String getCharacterEncoding() {
		throw new UnsupportedBackgroundOperationException();
	}

	public int getContentLength() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getContentType() {
		throw new UnsupportedBackgroundOperationException();
	}

	public ServletInputStream getInputStream() throws IOException {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getLocalAddr() {
		throw new UnsupportedBackgroundOperationException();
	}

	public Locale getLocale() {
		throw new UnsupportedBackgroundOperationException();
	}

	public Enumeration getLocales() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getLocalName() {
		throw new UnsupportedBackgroundOperationException();
	}

	public int getLocalPort() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getParameter(String name) {
		final Object obj = parameters.get(name);
		if(obj instanceof String[]) {
			return ((String[])obj)[0];
		}
		return (String)obj;
	}

	public Map<?,?> getParameterMap() {
		return parameters;
	}

	public Enumeration<?> getParameterNames() {
		return new IteratorEnumeration<String>(parameters.keySet().iterator());
	}

	public String[] getParameterValues(String key) {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getProtocol() {
		throw new UnsupportedBackgroundOperationException();
	}

	public BufferedReader getReader() throws IOException {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getRealPath(String arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getRemoteAddr() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getRemoteHost() {
		throw new UnsupportedBackgroundOperationException();
	}

	public int getRemotePort() {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getScheme() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getServerName() {
		throw new UnsupportedBackgroundOperationException();
	}

	public int getServerPort() {
		throw new UnsupportedBackgroundOperationException();
	}

	public boolean isSecure() {
		throw new UnsupportedBackgroundOperationException();
	}

	public void removeAttribute(String arg0) {
		throw new UnsupportedBackgroundOperationException();
		
	}

	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}

	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		throw new UnsupportedBackgroundOperationException();
		
	}

	public Cookie getCookie(String arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestDispatcher getRequestDispatcher(Resource arg0,
			RequestDispatcherOptions arg1) {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestDispatcher getRequestDispatcher(Resource arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestDispatcher getRequestDispatcher(String arg0,
			RequestDispatcherOptions arg1) {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestParameter getRequestParameter(String arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestParameterMap getRequestParameterMap() {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestParameter[] getRequestParameters(String arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestPathInfo getRequestPathInfo() {
		throw new UnsupportedBackgroundOperationException();
	}

	public RequestProgressTracker getRequestProgressTracker() {
		return requestProgressTracker;
	}

	public Resource getResource() {
		throw new UnsupportedBackgroundOperationException();
	}

	public ResourceBundle getResourceBundle(Locale arg0) {
		throw new UnsupportedBackgroundOperationException();
	}

	public ResourceBundle getResourceBundle(String arg0, Locale arg1) {
		throw new UnsupportedBackgroundOperationException();
	}

	public ResourceResolver getResourceResolver() {
		throw new UnsupportedBackgroundOperationException();
	}

	public String getResponseContentType() {
		throw new UnsupportedBackgroundOperationException();
	}

	public Enumeration<String> getResponseContentTypes() {
		throw new UnsupportedBackgroundOperationException();
	}

	public <AdapterType> AdapterType adaptTo(Class<AdapterType> arg0) {
		throw new UnsupportedBackgroundOperationException();
	}
}
