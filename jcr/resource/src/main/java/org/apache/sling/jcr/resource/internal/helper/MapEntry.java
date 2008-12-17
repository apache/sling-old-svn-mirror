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
package org.apache.sling.jcr.resource.internal.helper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver2;

/**
 * The <code>MapEntry</code> class represents a mapping entry in the mapping
 * configuration tree at <code>/etc/map</code>.
 * <p>
 * 
 * @see http://cwiki.apache.org/SLING/flexible-resource-resolution.html
 */
public class MapEntry implements Comparable<MapEntry> {

    private static final Pattern[] PATH_TO_URL_MATCH = {
        Pattern.compile("http/([^/]+)\\.80(/.*)?$"),
        Pattern.compile("https/([^/]+)\\.443(/.*)?$"),
        Pattern.compile("([^/]+)/([^/]+)\\.(\\d+)(/.*)?$") };

    private static final String[] PATH_TO_URL_REPLACEMENT = { "http://$1$2",
        "https://$1$2", "$1://$2:$3$4" };

    private final Pattern urlPattern;

    private final String[] redirect;

    private final int status;

    public static String appendSlash(String path) {
        if (!path.endsWith("/")) {
            path = path.concat("/");
        }
        return path;
    }
    
    /**
     * Returns a string used for matching map entries against the given request
     * or URI parts.
     * 
     * @param scheme The URI scheme
     * @param host The host name
     * @param port The port number. If this is negative, the default value used
     *            is 80 unless the scheme is "https" in which case the default
     *            value is 443.
     * @param path The (absolute) path
     * @return The request path string {scheme}://{host}:{port}{path}.
     */
    public static String getURI(String scheme, String host, int port, String path) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        if (port > 0 && !(port == 80 && "http".equals(scheme))
            && !(port == 443 && "https".equals(scheme))) {
            sb.append(':').append(port);
        }
        sb.append(path);
        
        return sb.toString();
    }

    public static URI toURI(String uriPath) {
        for (int i = 0; i < PATH_TO_URL_MATCH.length; i++) {
            Matcher m = PATH_TO_URL_MATCH[i].matcher(uriPath);
            if (m.find()) {
                String newUriPath = m.replaceAll(PATH_TO_URL_REPLACEMENT[i]);
                try {
                    return new URI(newUriPath);
                } catch (URISyntaxException use) {
                    // ignore, just don't return the uri as such
                }
            }
        }

        return null;
    }

    public static MapEntry createResolveEntry(String url, Resource resource, boolean trailingSlash) {
        ValueMap props = resource.adaptTo(ValueMap.class);
        if (props != null) {
            String redirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_EXTERNAL, String.class);
            if (redirect != null) {
                int status = props.get(
                    JcrResourceResolver2.PROP_REDIRECT_EXTERNAL_STATUS, 302);
                return new MapEntry(url, redirect, status, trailingSlash);
            }

            String[] internalRedirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_INTERNAL, String[].class);
            if (internalRedirect != null) {
                return new MapEntry(url, internalRedirect, -1, trailingSlash);
            }
        }

        return null;
    }

    public static List<MapEntry> createMapEntry(String url, Resource resource, boolean trailingSlash) {
        ValueMap props = resource.adaptTo(ValueMap.class);
        if (props != null) {
            String redirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_EXTERNAL, String.class);
            if (redirect != null) {
                // ignoring external redirects for mapping
                return null;
            }

            String[] internalRedirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_INTERNAL, String[].class);
            if (internalRedirect != null) {

                int status = -1;
                URI extPathPrefix = toURI(url);
                if (extPathPrefix != null) {
                    url = getURI(extPathPrefix.getScheme(),
                        extPathPrefix.getHost(), extPathPrefix.getPort(),
                        extPathPrefix.getPath());
                    status = 302;
                }

                List<MapEntry> prepEntries = new ArrayList<MapEntry>(
                    internalRedirect.length);
                for (String redir : internalRedirect) {
                    if (!redir.contains("$")) {
                        prepEntries.add(new MapEntry(redir, url, status, trailingSlash));
                    }
                }

                if (prepEntries.size() > 0) {
                    return prepEntries;
                }
            }
        }

        return null;
    }

    public MapEntry(String url, String redirect, int status, boolean trailingSlash) {
        this(url, new String[] { redirect }, status, trailingSlash);
    }

    public MapEntry(String url, String[] redirect, int status, boolean trailingSlash) {
        
        // ensure trailing slashes on redirects if the url
        // ends with a trailing slash
        if (trailingSlash) {
            url = appendSlash(url);
            for (int i = 0; i < redirect.length; i++) {
                redirect[i] = appendSlash(redirect[i]);
            }
        }
        
        this.urlPattern = Pattern.compile(url);
        this.redirect = redirect;
        this.status = status;
    }

    public Matcher getMatcher(String value) {
        return urlPattern.matcher(value);
    }

    // Returns the replacement or null if the value does not match
    public String[] replace(String value) {
        Matcher m = urlPattern.matcher(value);
        if (m.find()) {
            String[] redirects = getRedirect();
            String[] results = new String[redirects.length];
            for (int i = 0; i < redirects.length; i++) {
                results[i] = m.replaceFirst(redirects[i]);
            }
            return results;
        }

        return null;
    }

    public String getPattern() {
        return urlPattern.toString();
    }
    
    public String[] getRedirect() {
        return redirect;
    }

    public boolean isInternal() {
        return getStatus() < 0;
    }

    public int getStatus() {
        return status;
    }

    // ---------- Comparable

    public int compareTo(MapEntry m) {
        if (this == m) {
            return 0;
        }

        int tlen = urlPattern.toString().length();
        int mlen = m.urlPattern.toString().length();
        if (tlen < mlen) {
            return 1;
        } else if (tlen > mlen) {
            return -1;
        }

        // lentghs are equal, but the entries are not
        // so order m after this
        return 1;
    }

    // ---------- Object overwrite
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("MapEntry: match:").append(urlPattern);

        buf.append(", replacement:");
        if (getRedirect().length == 1) {
            buf.append(getRedirect()[0]);
        } else {
            buf.append(Arrays.asList(getRedirect()));
        }

        if (isInternal()) {
            buf.append(", internal");
        } else {
            buf.append(", status:").append(getStatus());
        }
        return buf.toString();
    }
}
