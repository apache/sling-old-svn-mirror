/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.http.Cookie;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.Content;
import org.apache.sling.content.ContentManager;
import org.apache.sling.core.impl.RequestData;


public class RequestUtil {

    /** format for RFC 1123 date string -- "Sun, 06 Nov 1994 08:49:37 GMT" */
    private final static SimpleDateFormat rfc1123Format = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    /** format for RFC 850 date string -- "Sunday, 06-Nov-94 08:49:37 GMT" */
    private final static SimpleDateFormat rfc850Format = new SimpleDateFormat(
        "EEEEEEEEE, dd-MMM-yy HH:mm:ss z", Locale.US);

    /** format for C asctime() date string -- "Sun Nov 6 08:49:37 1994" */
    private final static SimpleDateFormat asctimeFormat = new SimpleDateFormat(
        "EEE MMM d HH:mm:ss yyyy", Locale.US);

    static {
        TimeZone zone = TimeZone.getTimeZone("GMT");
        rfc1123Format.setTimeZone(zone);
        rfc850Format.setTimeZone(zone);
        asctimeFormat.setTimeZone(zone);
    }

    /**
     * Returns the <code>ContentManager</code> used by the given
     * <code>request</code>. If the content manager has not been assigned yet
     * to the request, this method returns <code>null</code>.
     *
     * @param request The <code>ComponentRequest</code> whose content manager
     *      is required.
     *
     * @return The <code>ContentManager</code> of the given request or
     *      <code>null</code> if the request has no content manager yet.
     */
    public static ContentManager getContentManager(ComponentRequest request) {
        try {
            return RequestData.getRequestData(request).getContentManager();
        } catch (ComponentException ce) {
            // may be thrown if request data cannot be unpacked from the request
            // in this case, we try the request attribute as a fallback
            return (ContentManager) request.getAttribute(Constants.ATTR_CONTENT_MANAGER);
        }
    }

    /**
     * Tries to retrieve the named cookie from the request.
     *
     * @param req The {@link DeliveryHttpServletRequest}
     *            from which to retrieve the cookie.
     * @param cookieName The name of the cookie to retrieve from the request.
     * @return The requested <code>Cookie</code> or <code>null</code> if the
     *         request does not contain a cookie of that name.
     */
    public static Cookie getCookie(ComponentRequest req, String cookieName) {

        // get and check cookies
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            // log.debug("getCookie: No cookies in request");
            return null;
        }

        // find session cookie
        for (int i = 0; i < cookies.length; i++) {
            if (cookieName.equals(cookies[i].getName())) {
                // log.debug("getCookie: Found Cookie {0}, value=''{1}''",
                // cookieName, cookies[i].getValue());
                return cookies[i];
            }
        }

        // no session cookie found
        // log.debug("getCookie: Cookie {0} not in request", cookieName);
        return null;
    }

    /**
     * Returns the string converted to a time value represented as the number of
     * milliseconds since January 1, 1970 or -1 if the string is
     * <code>null</code>.
     * <p>
     * This method supports the following date formats compliant with section
     * 3.3.1, Full Date, of <a href="http://www.faqs.org/rfcs/rfc2616.html">RFC
     * 2616</a>:
     * <ol>
     * <li>RFC 1123 (obsoletes RFC 822) of the form <i>Sun, 06 Nov 1994
     * 08:49:37 GMT</i>
     * <li>RFC 850 (obsoleted by RFC 1036) of the form <i>Sunday, 06-Nov-94
     * 08:49:37 GMT</i>
     * <li>C asctime() of the form <i>Sun Nov 6 08:49:37 1994</i>
     * </ol>
     * All time values are always interpreted in the GMT time zone and
     * interpreted as being the same as UTC (Universal Time Coordinated).
     * <p>
     * If the string cannot be converted to a date, this method throws an
     * <code>IllegalArgumentException</code>.
     *
     * @param name a String specifying the name of the header
     * @return a long value representing the date specified in the header
     *         expressed as the number of milliseconds since January 1, 1970
     *         GMT, or -1 if the named header was not included with the request
     * @throws IllegalArgumentException If the header value can�t be converted
     *             to a date
     */
    public static long toDateValue(String dateString) {

        // unknown date if null
        if (dateString == null) {
            return -1;
        }

        // Check preferred RFC 1123 (updated RFC 822) format first
        synchronized (rfc1123Format) {
            try {
                return rfc1123Format.parse(dateString).getTime();
            } catch (ParseException e) {
            } catch (NumberFormatException e) {
            }
        }

        // Check obsolete RFC 850 format
        synchronized (rfc850Format) {
            try {
                return rfc850Format.parse(dateString).getTime();
            } catch (ParseException e) {
            } catch (NumberFormatException e) {
            }
        }

        // finally check C asctime format
        synchronized (asctimeFormat) {
            try {
                return asctimeFormat.parse(dateString).getTime();
            } catch (ParseException e) {
            } catch (NumberFormatException e) {
            }
        }

        throw new IllegalArgumentException("invalid date");
    }

    /**
     * Converts the time value given as the number of milliseconds since January
     * 1, 1970 to a date and time string compliant with RFC 1123 date
     * specification. The resulting string is compliant with section 3.3.1, Full
     * Date, of <a href="http://www.faqs.org/rfcs/rfc2616.html">RFC 2616</a>
     * and may thus be used as the value of date header such as
     * <code>Date</code>.
     *
     * @param date The date value to convert to a string
     * @return The string representation of the date and time value.
     */
    public static String toDateString(long date) {
        synchronized (rfc1123Format) {
            return rfc1123Format.format(new Date(date));
        }
    }

    /**
     * Parses a header of the form:
     *
     * <pre>
     *            Header = Token { &quot;,&quot; Token } .
     *            Token = name { &quot;;&quot; Parameter } .
     *            Paramter = name [ &quot;=&quot; value ] .
     * </pre>
     *
     * "," and ";" are not allowed within name and value
     *
     * @param value
     * @return A Map indexed by the Token names where the values are Map
     *         instances indexed by parameter name
     */
    public static Map<String, Map<String, String>> parserHeader(String value) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        String[] tokens = value.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String[] parameters = tokens[i].split(";");
            String name = parameters[0].trim();
            Map<String, String> parMap;
            if (parameters.length > 0) {
                parMap = new HashMap<String, String>();
                for (int j = 1; j < parameters.length; j++) {
                    String[] content = parameters[j].split("=", 2);
                    if (content.length > 1) {
                        parMap.put(content[0].trim(), content[1].trim());
                    } else {
                        parMap.put(content[0].trim(), content[0].trim());
                    }
                }
            } else {
                parMap = Collections.emptyMap();
            }
            result.put(name, parMap);
        }
        return result;
    }

    /**
     * Parses an <code>Accept-*</code> header of the form:
     *
     * <pre>
     *            Header = Token { &quot;,&quot; Token } .
     *            Token = name { &quot;;&quot; &quot;q&quot; [ &quot;=&quot; value ] } .
     *            Paramter =  .
     * </pre>
     *
     * "," and ";" are not allowed within name and value
     *
     * @param value
     * @return A Map indexed by the Token names where the values are
     *         <code>Double</code> instances providing the value of the
     *         <code>q</code> parameter.
     */
    public static Map<String, Double> parserAcceptHeader(String value) {
        Map<String, Double> result = new HashMap<String, Double>();
        String[] tokens = value.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String[] parameters = tokens[i].split(";");
            String name = parameters[0];
            Double qVal = new Double(1.0);
            if (parameters.length > 1) {
                for (int j = 1; j < parameters.length; j++) {
                    String[] content = parameters[j].split("=", 2);
                    if (content.length > 1 && "q".equals(content[0])) {
                        try {
                            qVal = Double.valueOf(content[1]);
                        } catch (NumberFormatException nfe) {
                            // don't care
                        }
                    }
                }
            }
            if (qVal != null) {
                result.put(name, qVal);
            }
        }
        return result;
    }

    /**
     * Normalize the path by resolving an relative path parts (<code>.</code>
     * and <code>..</code>). If the path contains no relative path parts, the
     * same path is returned unmodified. If the path is <code>null</code> or
     * empty, the same path is returned unmodified. If the path is absolute the
     * returned path is also absolute, otherwise if the path is relative the
     * returned path is also relative.
     *
     * @param path The path to normalize.
     * @return The normalized path.
     *
     * @throws IllegalArgumentException If the path cannot be normalized because
     *             relative parent path parts (<code>..</code>) would point
     *             above the first path element of <code>path</code>.
     */
    public static String normalize(String path) {
        // nothing to do on null or empty path
        if (path == null || path.length() == 0) {
            return path;
        }

        if (path.indexOf("/.") >= 0 || path.indexOf("/.") >= 0) {
            // potiential . and .. path elements

            StringBuffer buf = new StringBuffer();

            if (path.charAt(0) == '/') {
                buf.append('/');
            }

            StringTokenizer tokener = new StringTokenizer(path, "/");
            while (tokener.hasMoreTokens()) {
                String token = tokener.nextToken();
                if ("..".equals(token)) {
                    int last = buf.lastIndexOf("/");
                    if (last > 0) {
                        buf.setLength(last);
                    } else if (last == 0) {
                        buf.setLength(1);
                    } else {
                        throw new IllegalArgumentException("Cannot normalize path " + path);
                    }
                } else if (".".equals(token)) {
                    // ignore, just continue

                } else {

                    // add separator if not root
                    if (buf.length() > 1) {
                        buf.append('/');
                    }

                    buf.append(token);
                }
            }

            // use normalized path now
            path = buf.toString();
        }

        // return path unmodified
        return path;
    }

    /**
     * Utility method returns the parent path of the given <code>path</code>,
     * which is normalized by {@link #normalize(String)} before resolving the
     * parent.
     *
     * @param path The path whose parent is to be returned.
     *
     * @return <code>null</code> if <code>path</code> is the root path (<code>/</code>)
     *      or if <code>path</code> is a single name containing no slash (<code>/</code>)
     *      characters.
     *
     * @throws IllegalArgumentException If the path cannot be normalized by
     *      the {@link #normalize(String)} method.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     */
    public static String getParent(String path) {
        if ("/".equals(path)) {
            return null;
        }

        // normalize path (remove . and ..)
        path = normalize(path);

        // if normalized to root, there is no parent
        if ("/".equals(path)) {
            return null;
        }

        // find the last slash, after which to cut off
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            // no slash in the path
            return null;
        }

        return path.substring(0, lastSlash);
    }

    /**
     * Utility method returns the name of the given <code>path</code>, which
     * is normalized by {@link #normalize(String)} before resolving the name.
     *
     * @param path The path whose name (the last path element) is to be
     *            returned.
     * @return The empty string if <code>path</code> is the root path (<code>/</code>)
     *         or if <code>path</code> is a single name containing no slash (<code>/</code>)
     *         characters.
     * @throws IllegalArgumentException If the path cannot be normalized by the
     *             {@link #normalize(String)} method.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     */
    public static String getName(String path) {
        if ("/".equals(path)) {
            return "";
        }

        // normalize path (remove . and ..)
        path = normalize(path);
        if ("/".equals(path)) {
            return "";
        }

        // find the last slash
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash) : path;
    }

    /**
     * Utility methods returns the parent <code>Content</code> object of the
     * <code>Content</code> of the given <code>request</code>. If the
     * <code>Content</code> object of the request represents the root of the
     * workspace, <code>null</code> is returned as the root has no parent.
     * <p>
     * Calling this method is exactly equivalent to:
     * <pre>
     *    String path = request.getContent().getPath();
     *    path = getParent(path);
     *    Content parent = (path == null) ? null : request.getContent(path);
     * </pre>
     *
     * @param request The <code>ComponentRequest</code> denoting the content
     *      object whose parent is requested.
     *
     * @return The parent <code>Content</code> object of the request's content
     *      or <code>null</code> if the request's content is the root.
     *
     * @throws ComponentException If accessing the repository to load the content
     *      fails.
     */
    public static Content getParentContent(ComponentRequest request)
            throws ComponentException {
        String path = request.getContent().getPath();
        path = getParent(path);
        if (path == null) {
            return null;
        }
        return request.getContent(path);
    }
}
