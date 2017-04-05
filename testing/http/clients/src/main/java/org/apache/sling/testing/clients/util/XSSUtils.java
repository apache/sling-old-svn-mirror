/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.util;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.sling.xss.XSSAPI;
import org.apache.sling.xss.impl.XSSAPIImpl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Basic class for XSS Testing
 * The reliability of these methods are not critical
 */
public class XSSUtils {

    /**
     * Use to ensure that HTTP query strings are in proper form, by escaping
     * special characters such as spaces.
     *
     * @param urlString the string to be encoded
     * @return the encoded string
     */
    public static String encodeUrl(String urlString) {
        try {
            return URLEncoder.encode(urlString, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
    }

    /**
     * Use to encapsulate old-style escaping of HTML (using StringEscapeUtils).
     * NB: newer code uses XSSAPI (based on OWASP's ESAPI).
     *
     * @param htmlString the string to be escaped
     * @return the escaped string
     */
    public static String escapeHtml(String htmlString) {
        return StringEscapeUtils.escapeHtml4(htmlString);
    }

    /**
     * Use to encapsulate old-style escaping of XML (with JSTL encoding rules).
     * NB: newer code uses XSSAPI (based on OWASP's ESAPI).
     *
     * @param xmlString the string to be escaped
     * @return the escaped string
     */
    public static String escapeXml(String xmlString) {
        String xssString = xmlString;
        if (xmlString != null) {
            xssString = xssString.replace(";", "&#x3b;");
            xssString = xssString.replace(" ", "&#x20;");
            xssString = xssString.replace("'", "&#x27;");
            xssString = xssString.replace("\"", "&quot;");
            xssString = xssString.replace(">", "&gt;");
            xssString = xssString.replace("<", "&lt;");
            xssString = xssString.replace("/", "&#x2f;");
            xssString = xssString.replace("(", "&#x28;");
            xssString = xssString.replace(")", "&#x29;");
            xssString = xssString.replace(":", "&#x3a;");
        }
        return xssString;
    }

    /**
     * Use to encapsulate new-style (XSSAPI-based) encoding for HTML element content.
     *
     * @param source the string to be encoded
     * @return the encoded string
     */
    public static String encodeForHTML(String source) {
        XSSAPI xssAPI = new XSSAPIImpl();
        return xssAPI.encodeForHTML(source);
    }

    /**
     * Use to encapsulate new-style (XSSAPI-based) encoding for HTML attribute values.
     *
     * @param source the string to be encoded
     * @return the encoded string
     */
    public static String encodeForHTMLAttr(String source) {
        XSSAPI xssAPI = new XSSAPIImpl();
        return xssAPI.encodeForHTMLAttr(source);
    }

    /**
     * Use to encapsulate new-style (XSSAPI-based) encoding for XML element content.
     *
     * @param source the string to be encoded
     * @return the encoded string
     */
    public static String encodeForXML(String source) {
        XSSAPI xssAPI = new XSSAPIImpl();
        return xssAPI.encodeForXML(source);
    }

    /**
     * Use to encapsulate new-style (XSSAPI-based) encoding for XML attribute values.
     *
     * @param source the string to be encoded
     * @return the encoded string
     */
    public static String encodeForXMLAttr(String source) {
        XSSAPI xssAPI = new XSSAPIImpl();
        return xssAPI.encodeForXMLAttr(source);
    }

    /**
     * Use to encapsulate new-style (XSSAPI-based) encoding for JavaScript strings.
     *
     * @param source the string to be encoded
     * @return the encoded string
     */
    public static String encodeForJSString(String source) {
        XSSAPI xssAPI = new XSSAPIImpl();
        return xssAPI.encodeForJSString(source);
    }

}
