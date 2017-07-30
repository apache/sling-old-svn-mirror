/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss.impl;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonReaderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSAPI;
import org.apache.sling.xss.XSSFilter;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.owasp.encoder.Encode;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

@Component(service = XSSAPI.class,
           property = {
                Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })

public class XSSAPIImpl implements XSSAPI {

    private final Logger LOGGER = LoggerFactory.getLogger(XSSAPIImpl.class);

    @Reference
    private XSSFilter xssFilter;

    private final Validator validator = ESAPI.validator();

    private static final Pattern PATTERN_AUTO_DIMENSION = Pattern.compile("['\"]?auto['\"]?");

    private SAXParserFactory factory;

    private volatile JsonReaderFactory jsonReaderFactory;

    @Activate
    protected void activate() {
        factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (Exception e) {
            LOGGER.error("SAX parser configuration error: " + e.getMessage(), e);
        }
        Map<String, Object> config = new HashMap<>();
        config.put("org.apache.johnzon.supports-comments", true);
        jsonReaderFactory = Json.createReaderFactory(config);
    }

    @Deactivate
    protected void deactivate() {
        factory = null;
        jsonReaderFactory = null;
    }

    // =============================================================================================
    // VALIDATORS
    //

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidInteger(String, int)
     */
    @Override
    public Integer getValidInteger(String integer, int defaultValue) {
        if (integer != null && integer.length() > 0) {
            try {
                return validator.getValidInteger("XSS", integer, -2000000000, 2000000000, false);
            } catch (Exception e) {
                // ignore
            }
        }

        // fall through to default if empty, null, or validation failure
        return defaultValue;
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidLong(String, long)
     */
    @Override
    public Long getValidLong(String source, long defaultValue) {
        if (source != null && source.length() > 0) {
            try {
                LongValidationRule ivr = new LongValidationRule( "number", ESAPI.encoder(), -9000000000000000000L, 9000000000000000000L );
                ivr.setAllowNull(false);
                return ivr.getValid("XSS", source);
            } catch (Exception e) {
                // ignore
            }
        }

        // fall through to default if empty, null, or validation failure
        return defaultValue;
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidDouble(String, double)
     */
    @Override
    public Double getValidDouble(String source, double defaultValue) {
        if (source != null && source.length() > 0) {
            try {
                return validator.getValidDouble("XSS", source, 0d, Double.MAX_VALUE, false);
            } catch (Exception e) {
                // ignore
            }
        }

        // fall through to default if empty, null, or validation failure
        return defaultValue;
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidDimension(String, String)
     */
    @Override
    public String getValidDimension(String dimension, String defaultValue) {
        if (dimension != null && dimension.length() > 0) {
            if (PATTERN_AUTO_DIMENSION.matcher(dimension).matches()) {
                return "\"auto\"";
            }

            try {
                return validator.getValidInteger("XSS", dimension, -10000, 10000, false).toString();
            } catch (Exception e) {
                // ignore
            }
        }

        // fall through to default if empty, null, or validation failure
        return defaultValue;
    }

    private static final String MANGLE_NAMESPACE_OUT_SUFFIX = ":";

    private static final String MANGLE_NAMESPACE_OUT = "/([^:/]+):";

    private static final Pattern MANGLE_NAMESPACE_PATTERN = Pattern.compile(MANGLE_NAMESPACE_OUT);

    private static final String MANGLE_NAMESPACE_IN_SUFFIX = "_";

    private static final String MANGLE_NAMESPACE_IN_PREFIX = "/_";

    private static final String SCHEME_PATTERN = "://";

    private String mangleNamespaces(String absPath) {
        if (absPath != null) {
            // check for absolute urls
            final int schemeIndex = absPath.indexOf(SCHEME_PATTERN);
            final String manglePath;
            final String prefix;
            if (schemeIndex != -1) {
                final int pathIndex = absPath.indexOf("/", schemeIndex + 3);
                if (pathIndex != -1) {
                    prefix = absPath.substring(0, pathIndex);
                    manglePath = absPath.substring(pathIndex);
                } else {
                    prefix = absPath;
                    manglePath = "";
                }
            } else {
                prefix = "";
                manglePath = absPath;
            }
            if (manglePath.contains(MANGLE_NAMESPACE_OUT_SUFFIX)) {
                final Matcher m = MANGLE_NAMESPACE_PATTERN.matcher(manglePath);

                final StringBuffer buf = new StringBuffer();
                while (m.find()) {
                    final String replacement = MANGLE_NAMESPACE_IN_PREFIX + m.group(1) + MANGLE_NAMESPACE_IN_SUFFIX;
                    m.appendReplacement(buf, replacement);
                }

                m.appendTail(buf);

                absPath = prefix + buf.toString();

            }
        }

        return absPath;
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidHref(String)
     */
    @Override
    @Nonnull
    public String getValidHref(final String url) {
        if (url != null && url.length() > 0) {
            // Percent-encode characters that are not allowed in unquoted
            // HTML attributes: ", ', >, <, ` and space. We don't encode =
            // since this would break links with query parameters.
            String encodedUrl = url.replaceAll("\"", "%22")
                    .replaceAll("'", "%27")
                    .replaceAll(">", "%3E")
                    .replaceAll("<", "%3C")
                    .replaceAll("`", "%60")
                    .replaceAll(" ", "%20");
            int qMarkIx = encodedUrl.indexOf('?');
            if (qMarkIx > 0) {
                encodedUrl = encodedUrl.substring(0, qMarkIx) + encodedUrl.substring(qMarkIx).replaceAll(":", "%3A");
            }

            encodedUrl = mangleNamespaces(encodedUrl);
            if (xssFilter.isValidHref(encodedUrl)) {
                return encodedUrl;
            }
        }

        // fall through to empty string
        return "";
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidJSToken(String, String)
     */
    @Override
    public String getValidJSToken(String token, String defaultValue) {
        if (token != null && token.length() > 0) {
            token = token.trim();
            String q = token.substring(0, 1);
            if (q.matches("['\"]") && token.endsWith(q)) {
                String literal = token.substring(1, token.length() - 1);
                return q + encodeForJSString(literal) + q;
            } else if (token.matches("[0-9a-zA-Z_$][0-9a-zA-Z_$.]*")) {
                return token;
            }
        }

        // fall through to default value
        return defaultValue;
    }

    private static final String NON_ASCII = "\\x00\\x08\\x0B\\x0C\\x0E-\\x1F";
    /** http://www.w3.org/TR/css-syntax-3/#number-token-diagram */
    private static final String NUMBER = "[+-]?[\\d]*[\\.]?[\\d]*(?:[e][+-]?\\d+)?";
    /** http://www.w3.org/TR/css-syntax-3/#hex-digit-diagram */
    private static final String HEX_DIGITS = "#[0-9a-f]*";
    /** http://www.w3.org/TR/css-syntax-3/#ident-token-diagram */
    private static final String IDENTIFIER = "-?[a-z_" + NON_ASCII + "][\\w_\\-" + NON_ASCII + "]*";
    /** http://www.w3.org/TR/css-syntax-3/#string-token-diagram */
    private static final String STRING = "\"(?:(?!javascript\\s?:)[^\"^\\\\^\\n]|(?:\\\\\"))*\"|'(?:(?!javascript\\s?:)[^'^\\\\^\\n]|(?:\\\\'))*'";
    /** http://www.w3.org/TR/css-syntax-3/#dimension-token-diagram */
    private static final String DIMENSION = NUMBER + IDENTIFIER;
    /** http://www.w3.org/TR/css-syntax-3/#percentage-token-diagram */
    private static final String PERCENT = NUMBER + "%";
    /** http://www.w3.org/TR/css-syntax-3/#function-token-diagram */
    private static final String FUNCTION = IDENTIFIER + "\\((?:(?:" + NUMBER + ")|(?:" + IDENTIFIER + ")|(?:[\\s]*)|(?:,))*\\)";
    /** http://www.w3.org/TR/css-syntax-3/#url-unquoted-diagram */
    private static final String URL_UNQUOTED = "[^\"^'^\\(^\\)^[" + NON_ASCII + "]]*";
    /** http://www.w3.org/TR/css-syntax-3/#url-token-diagram */
    private static final String URL = "url\\((?:(?:" + URL_UNQUOTED + ")|(?:" + STRING + "))\\)";
    /** composite regular expression for style token validation */
    private static final String CSS_TOKEN = "(?i)" // case insensitive
            + "(?:" + NUMBER + ")"
            + "|(?:" + DIMENSION + ")"
            + "|(?:" + PERCENT + ")"
            + "|(?:" + HEX_DIGITS + ")"
            + "|(?:" + IDENTIFIER + ")"
            + "|(?:" + STRING + ")"
            + "|(?:" + FUNCTION + ")"
            + "|(?:" + URL + ")";

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidStyleToken(String, String)
     */
    @Override
    public String getValidStyleToken(String token, String defaultValue) {
        if (token != null && token.length() > 0 && token.matches(CSS_TOKEN)) {
            return token;
        }

        return defaultValue;
   }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidCSSColor(String, String)
     */
    @Override
    public String getValidCSSColor(String color, String defaultColor) {
        if (color != null && color.length() > 0) {
            color = color.trim();
            /*
             * Avoid security implications by including only the characters required to specify colors in hex
             * or functional notation. Critical characters disallowed: x (as in expression(...)),
             * u (as in url(...)) and semi colon (as in escaping the context of the color value).
             */
            if (color.matches("(?i)[#a-fghlrs(+0-9-.%,) \\t\\n\\x0B\\f\\r]+")) {
                return color;
            }
            // named color values
            if (color.matches("(?i)[a-zA-Z \\t\\n\\x0B\\f\\r]+")) {
                return color;
            }
        }

        return defaultColor;
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidMultiLineComment(String, String)
     */
    @Override
    public String getValidMultiLineComment(String comment, String defaultComment) {
        if (comment != null && !comment.contains("*/")) {
            return comment;
        }
        return defaultComment;
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidJSON(String, String)
     */
    @Override
    public String getValidJSON(String json, String defaultJson) {
        if (json == null) {
            return getValidJSON(defaultJson, "");
        }
        json = json.trim();
        if ("".equals(json)) {
            return "";
        }
        int curlyIx = json.indexOf("{");
        int straightIx = json.indexOf("[");
        if (curlyIx >= 0 && (curlyIx < straightIx || straightIx < 0)) {
            try {
                StringWriter output = new StringWriter();
                Json.createGenerator(output).write(jsonReaderFactory.createReader(new StringReader(json)).readObject()).close();
                return output.getBuffer().toString();
            } catch (Exception e) {
                LOGGER.debug("JSON validation failed: " + e.getMessage(), e);
            }
        } else {
            try {
                StringWriter output = new StringWriter();
                Json.createGenerator(output).write(jsonReaderFactory.createReader(new StringReader(json)).readArray()).close();
                return output.getBuffer().toString();
            } catch (Exception e) {
                LOGGER.debug("JSON validation failed: " + e.getMessage(), e);
            }
        }
        return getValidJSON(defaultJson, "");
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidXML(String, String)
     */
    @Override
    public String getValidXML(String xml, String defaultXml) {
        if (xml == null) {
            return getValidXML(defaultXml, "");
        }
        xml = xml.trim();
        if ("".equals(xml)) {
            return "";
        }

        try {
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.parse(new InputSource(new StringReader(xml)));
            return xml;
        } catch (Exception e) {
            LOGGER.debug("XML validation failed: " + e.getMessage(), e);
        }
        return getValidXML(defaultXml, "");
    }

    // =============================================================================================
    // ENCODERS
    //

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForHTML(String)
     */
    @Override
    public String encodeForHTML(String source) {
        return source == null ? null : Encode.forHtml(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForHTMLAttr(String)
     */
    @Override
    public String encodeForHTMLAttr(String source) {
        return source == null ? null : Encode.forHtmlAttribute(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForXML(String)
     */
    @Override
    public String encodeForXML(String source) {
        return source == null ? null : Encode.forXml(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForXMLAttr(String)
     */
    @Override
    public String encodeForXMLAttr(String source) {
        return source == null ? null : Encode.forXmlAttribute(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForJSString(String)
     */
    @Override
    public String encodeForJSString(String source) {
        return source == null ? null : Encode.forJavaScript(source).replace("\\-", "\\u002D");
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForCSSString(String)
     */
    @Override
    public String encodeForCSSString(String source) {
        return source == null ? null : Encode.forCssString(source);
    }

    // =============================================================================================
    // FILTERS
    //

    /**
     * @see org.apache.sling.xss.XSSAPI#filterHTML(String)
     */
    @Override
    @Nonnull
    public String filterHTML(String source) {
        return xssFilter.filter(ProtectionContext.HTML_HTML_CONTENT, source);
    }
}
