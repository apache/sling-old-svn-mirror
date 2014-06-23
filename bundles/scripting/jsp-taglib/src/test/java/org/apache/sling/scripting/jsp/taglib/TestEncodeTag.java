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
package org.apache.sling.scripting.jsp.taglib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Tests for the Class EscapeTag.
 * 
 * @see org.apache.sling.scripting.jsp.taglib.EscapeTag
 */
public class TestEncodeTag {

	private static final Logger log = LoggerFactory
			.getLogger(TestEncodeTag.class);
	private EncodeTag encodeTag;
	private MockPageContext pageContext;
	private StringBuilder sb;
	private static final String VAR_KEY = "properties";

	/**
	 * Initializes the fields for this test.
	 */
	@SuppressWarnings("serial")
	@Before
	public void init() {
		log.info("init");
		encodeTag = new EncodeTag();

		sb = new StringBuilder();
		final JspWriter w = new JspWriter(0, false){
			public void newLine() throws IOException {
				throw new UnsupportedOperationException();
			}
			public void print(boolean paramBoolean) throws IOException {
				sb.append(paramBoolean);
			}
			public void print(char paramChar) throws IOException {
				sb.append(paramChar);
			}
			public void print(int paramInt) throws IOException {
				sb.append(paramInt);
			}
			public void print(long paramLong) throws IOException {
				sb.append(paramLong);
			}
			public void print(float paramFloat) throws IOException {
				sb.append(paramFloat);
			}
			public void print(double paramDouble) throws IOException {
				sb.append(paramDouble);
			}
			public void print(char[] paramArrayOfChar) throws IOException {
				sb.append(paramArrayOfChar);
			}
			public void print(String paramString) throws IOException {
				sb.append(paramString);
			}
			public void print(Object paramObject) throws IOException {
				sb.append(paramObject);
			}
			public void println() throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(boolean paramBoolean) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(char paramChar) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(int paramInt) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(long paramLong) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(float paramFloat) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(double paramDouble) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(char[] paramArrayOfChar) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(String paramString) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void println(Object paramObject) throws IOException {
				throw new UnsupportedOperationException();
			}
			public void clear() throws IOException {
				throw new UnsupportedOperationException();
			}
			public void clearBuffer() throws IOException {
				throw new UnsupportedOperationException();
			}
			public void flush() throws IOException {
				throw new UnsupportedOperationException();
			}
			public void close() throws IOException {
				throw new UnsupportedOperationException();
			}
			public int getRemaining() {
				throw new UnsupportedOperationException();
			}
			public void write(char[] cbuf, int off, int len) throws IOException {
				sb.append(cbuf);
			}
			
		};
		pageContext = new MockPageContext(){
			public JspWriter getOut() {
				return w;
			}
		};
		encodeTag.setPageContext(pageContext);
		log.info("init Complete");
	}

	/**
	 * Tests the adapt object Tag functionality.
	 * @throws JspException 
	 */
	@Test
	public void testEncode() throws JspException {
		log.info("testAdaptObject");

		log.info("Setting up tests");
		encodeTag.setValue("&amp;Hello World!");
		encodeTag.setMode("html");
		encodeTag.doStartTag();
		encodeTag.doEndTag();

		log.info("Checking result");
		assertEquals("&amp;amp&#x3b;Hello World&#x21;",sb.toString().trim());

		log.info("Test successful!");
	}
}
