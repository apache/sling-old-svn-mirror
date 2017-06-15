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
package org.apache.sling.testing.clients.email;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds information retrieved from the mock SMTP server deployed in Sling
 *
 */
public final class EmailMessage {
	
	public static final String HEADER_FROM = "From";
	public static final String HEADER_TO = "To";
	public static final String HEADER_SUBJECT = "Subject";
	
	private Map<String, String> headers = new LinkedHashMap<>();
	
	private String content;

	public EmailMessage(String content) {
		this.content = content;
	}
	
	/**
	 * Adds a new header to this email message
	 * 
	 * @param key the header name
	 * @param value the header value
	 */
	public void addHeader(String key, String value) {
		headers.put(key, value);
	}
	
	/**
	 * Returns the value of one of the headers of this email
	 * 
	 * @param key the header name
	 * @return the value of the header, possibly <code>null</code>
	 */
	public String getHeader(String key) {
		return headers.get(key);
	}
	
	/**
	 * Returns an unmodifiable view over the email headers
	 * 
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}
	
	/**
	 * Returns the contents of the email
	 * 
	 * @return the email content
	 */
	public String getContent() {
		return content;
	}
}