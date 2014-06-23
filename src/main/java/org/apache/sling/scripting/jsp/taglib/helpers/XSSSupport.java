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
package org.apache.sling.scripting.jsp.taglib.helpers;

import org.apache.commons.lang.StringUtils;
import org.owasp.esapi.ESAPI;

/**
 * Support for basic XSS protection as provided by the OWASP ESAPI's escape
 * methods.
 */
public class XSSSupport {

	/**
	 * The encoding modes supported by this tag.
	 */
	public static enum ENCODING_MODE {
		/**
		 * Encodes the content as HTML
		 */
		HTML, HTML_ATTR, XML, XML_ATTR, JS
	}

	/**
	 * Encodes the unencoded string using the specified mode. This will be
	 * deferred to the corresponding OWASP ESAPI encoding method.
	 * 
	 * @param unencoded
	 *            the unencoded string
	 * @param mode
	 *            the mode with which to encode the string
	 * @return the encoded string
	 */
	public static String encode(String unencoded, ENCODING_MODE mode) {

		String encoded = null;
		switch (mode) {
		case HTML:
			encoded = ESAPI.encoder().encodeForHTML(unencoded);
			break;
		case HTML_ATTR:
			encoded = ESAPI.encoder().encodeForHTMLAttribute(unencoded);
			break;
		case XML:
			encoded = ESAPI.encoder().encodeForXML(unencoded);
			break;
		case XML_ATTR:
			encoded = ESAPI.encoder().encodeForXMLAttribute(unencoded);
			break;
		case JS:
			encoded = ESAPI.encoder().encodeForJavaScript(unencoded);
			break;
		default:
			break;
		}
		return encoded;
	}

	/**
	 * Retrieves the encoding mode associated with the specified string. Will
	 * throw an IllegalArgumentException if the mode string is not a valid mode
	 * and will throw a NullPointerException if the mode string is null.
	 * 
	 * @param modeStr
	 *            the mode string
	 * @return the encoding mode
	 */
	public static ENCODING_MODE getEncodingMode(String modeStr) {
		return ENCODING_MODE.valueOf(StringUtils.upperCase(modeStr));
	}
}
