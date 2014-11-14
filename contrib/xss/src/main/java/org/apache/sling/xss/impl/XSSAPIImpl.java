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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.xss.ProtectionContext;
import org.apache.sling.xss.XSSAPI;
import org.apache.sling.xss.XSSFilter;
import org.owasp.encoder.Encode;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Validator;

@Component
@Service(value = XSSAPI.class)
public class XSSAPIImpl implements XSSAPI {

    // =============================================================================================
    // VALIDATORS
    //

    @Reference
    private XSSFilter xssFilter = null;

    private Validator validator = ESAPI.validator();

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidInteger(String, int)
     */
    public Integer getValidInteger(String integer, int defaultValue) {
        try {
            if (integer == null || integer.length() == 0) {
                return defaultValue;
            } else {
                return validator.getValidInteger("XSS", integer, -2000000000, 2000000000, false);
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidLong(String, long)
     */
    public Long getValidLong(String source, long defaultValue) {
        try {
            if (source == null || source.length() == 0) {
                return defaultValue;
            } else {
                return validator.getValidNumber("XSS", source, -9000000000000000000L, 9000000000000000000L, false).longValue();
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidDimension(String, String)
     */
    public String getValidDimension(String dimension, String defaultValue) {
        try {
            if (dimension == null || dimension.length() == 0) {
                return defaultValue;
            } else if (dimension.matches("['\"]?auto['\"]?")) {
                return "\"auto\"";
            }
            return validator.getValidInteger("XSS", dimension, -10000, 10000, false).toString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static final String LINK_PREFIX = "<a href=\"";
    private static final String LINK_SUFFIX = "\"></a>";

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
    public String getValidHref(final String url) {
        try {
            // Percent-encode characters that are not allowed in unquoted
            // HTML attributes: ", ', >, <, ` and space. We don't encode =
            // since this would break links with query parameters.
            String encodedUrl = url.replaceAll("\"", "%22")
                    .replaceAll("'", "%27")
                    .replaceAll(">", "%3E")
                    .replaceAll("<", "%3C")
                    .replaceAll("`", "%60")
                    .replaceAll(" ", "%20");
            String testHtml = LINK_PREFIX + mangleNamespaces(encodedUrl) + LINK_SUFFIX;
            // replace all & with &amp; because filterHTML will also apply this encoding
            testHtml = testHtml.replaceAll("&(?!amp)", "&amp;");
            final String safeHtml = xssFilter.filter(ProtectionContext.HTML_HTML_CONTENT, testHtml);
            // if the xssFilter didn't like the input string we just return ""
            // otherwise we return the mangled url without encoding
            if (!safeHtml.equals(testHtml)) {
                return "";
            } else {
                return mangleNamespaces(encodedUrl);
            }
        } catch (final NullPointerException e) {
            // ProtectionContext was null - simply return an empty string
            return "";
        }
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidJSToken(String, String)
     */
    public String getValidJSToken(String token, String defaultValue) {
        token = token.trim();
        String q = token.substring(0, 1);
        if (q.matches("['\"]") && token.endsWith(q)) {
            String literal = token.substring(1, token.length() - 1);
            return q + encodeForJSString(literal) + q;
        } else if (token.matches("[0-9a-zA-Z_$][0-9a-zA-Z_$.]*")) {
            return token;
        } else {
            return defaultValue;
        }
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getValidCSSColor(String, String)
     */
    public String getValidCSSColor(String color, String defaultColor) {
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
        return defaultColor;
    }

    // =============================================================================================
    // ENCODERS
    //

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForHTML(String)
     */
    public String encodeForHTML(String source) {
        return Encode.forHtml(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForHTMLAttr(String)
     */
    public String encodeForHTMLAttr(String source) {
        return Encode.forHtmlAttribute(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForXML(String)
     */
    public String encodeForXML(String source) {
        return Encode.forXml(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForXMLAttr(String)
     */
    public String encodeForXMLAttr(String source) {
        return Encode.forXmlAttribute(source);
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#encodeForJSString(String)
     */
    public String encodeForJSString(String source) {
        return Encode.forJavaScript(source);
    }

    // =============================================================================================
    // FILTERS
    //

    /**
     * @see org.apache.sling.xss.XSSAPI#filterHTML(String)
     */
    public String filterHTML(String source) {
        return xssFilter.filter(ProtectionContext.HTML_HTML_CONTENT, source);
    }

    // =============================================================================================
    // JCR-NAMESPACE MANGLING
    //

    /**
     * @see org.apache.sling.xss.XSSAPI#getRequestSpecificAPI(org.apache.sling.api.SlingHttpServletRequest)
     */
    public XSSAPI getRequestSpecificAPI(final SlingHttpServletRequest request) {
        return this;
    }

    /**
     * @see org.apache.sling.xss.XSSAPI#getResourceResolverSpecificAPI(org.apache.sling.api.resource.ResourceResolver)
     */
    public XSSAPI getResourceResolverSpecificAPI(final ResourceResolver resourceResolver) {
        return this;
    }
}
