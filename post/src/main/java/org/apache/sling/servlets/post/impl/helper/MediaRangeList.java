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

package org.apache.sling.servlets.post.impl.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facilitates parsing of the Accept HTTP request header.
 * See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC 2616 section 14.1</a>
 */
public class MediaRangeList extends TreeSet<MediaRangeList.MediaRange> {
    public static final String HEADER_ACCEPT = "Accept";
    public static final String PARAM_ACCEPT = ":http-equiv-accept";
    public static final String WILDCARD = "*";
    boolean matchesAll = false;

    private static final Logger log = LoggerFactory.getLogger(MediaRangeList.class);

    /**
     * Constructs a <code>MediaRangeList</code> using information from the supplied <code>HttpServletRequest</code>.
     * if the request contains a {@link #PARAM_ACCEPT} query parameter, the query parameter value overrides any
     * {@link #HEADER_ACCEPT} header value.
     * If the request contains no {@link #PARAM_ACCEPT} parameter, or the parameter value is empty, the value of the
     * {@link #HEADER_ACCEPT} is used. If both values are missing, it is assumed that the client accepts all media types,
     * as per the RFC. See also {@link MediaRangeList#MediaRangeList(java.lang.String)}
     * @param request The <code>HttpServletRequest</code> to extract a <code>MediaRangeList</code> from
     */
    public MediaRangeList(HttpServletRequest request) {
        String queryParam = request.getParameter(PARAM_ACCEPT);
        if (queryParam != null && queryParam.trim().length() != 0) {
            init(queryParam);
        } else {
           init(request.getHeader(HEADER_ACCEPT));
        }
    }

    /**
     * Constructs a <code>MediaRangeList</code> using a list of media ranges specified in a <code>java.lang.String</code>.
     * The string is a comma-separated list of media ranges, as specified by the RFC.<br />
     * Examples:
     * <ul>
     * <li><code>text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, *&#47;*;q=0.5</code></li>
     * <li><code>text/html;q=0.8, application/json</code></li>
     * </ul>
     *
     * @param listStr The list of media range specifications
     */
    public MediaRangeList(String listStr) {
        try {
            init(listStr);
        } catch (Throwable t) {
            log.error("Error building MediaRangeList from '" + listStr + "' - will assume client accepts all media types", t);
            init(null);
        }
    }

    private void init(String headerValue) {
        if (headerValue == null || headerValue.trim().length() == 0) {
            // RFC 2616: "If no Accept header field is present,
            // then it is assumed that the client accepts all media types."
            this.matchesAll = true;
            this.add(new MediaRange(WILDCARD + "/" + WILDCARD));
        } else {
            String[] mediaTypes = headerValue.split(",");
            for (String type : mediaTypes) {
                try {
                    MediaRange range = new MediaRange(type);
                    this.add(range);
                    if (range.matchesAll()) {
                        this.matchesAll = true;
                    }
                } catch (Throwable throwable) {
                    log.warn("Error registering media type " + type, throwable);
                }
            }
        }
    }

    /**
     * Determines if this MediaRangeList contains a given media type.
     * @param mediaType A string on the form <code>type/subtype</code>. Neither <code>type</code>
     * or <code>subtype</code> should be wildcard (<code>*</code>).
     * @return <code>true</code> if this <code>MediaRangeList</code> contains a media type that matches
     * <code>mediaType</code>, <code>false</code> otherwise
     * @throws IllegalArgumentException if <code>mediaType</code> is not on an accepted form
     * @throws NullPointerException if <code>mediaType</code> is <code>null</code>
     */
    public boolean contains(String mediaType) {
        //noinspection SuspiciousMethodCalls
        MediaRange comp = new MediaRange(mediaType);
        return this.matchesAll || this.contains(comp);
    }

    /**
     * Given a list of media types, returns the one is preferred by this <code>MediaRangeList</code>.
     * @param mediaRanges An array of possible {@link org.apache.sling.servlets.post.impl.helper.MediaRangeList.MediaRange}s
     * @return One of the <code>mediaRanges</code> that this <code>MediaRangeList</code> prefers;
     * or <code>null</code> if this <code>MediaRangeList</code> does not contain any of the <code>mediaRanges</code>
     * @throws NullPointerException if <code>mediaRanges</code> is <code>null</code> or contains a <code>null</code> value
     */
    public MediaRange prefer(Set<MediaRange> mediaRanges) {
        for (MediaRange range : this) {
            for (MediaRange mediaType : mediaRanges) {
                if (range.equals(mediaType)) {
                    return mediaType;
                }
            }
        }
        return null;
    }

    /**
     * Determines which of the <code>mediaRanges</code> specifiactions is prefered by this <code>MediaRangeList</code>.
     * @param mediaRanges String representations of <code>MediaRange</code>s. The strings must be
     * on the form required by {@link MediaRange#MediaRange(String)}
     * @see #prefer(java.util.Set)
     * @return the <code>toString</code> representation of the prefered <code>MediaRange</code>, or <code>null</code>
     * if this <code>MediaRangeList</code> does not contain any of the <code>mediaRanges</code>
     */
    public String prefer(String... mediaRanges) {
        Set<MediaRange> ranges = new HashSet<MediaRange>();
        for (String mediaRange : mediaRanges) {
            ranges.add(new MediaRange(mediaRange));
        }
        final MediaRange preferred = prefer(ranges);
        return(preferred == null ? null : preferred.toString());
    }

    /**
     * A code <code>MediaRange</code> represents an entry in a <code>MediaRangeList</code>.
     * The <code>MediaRange</code> consists of a <code>supertype</code> and a <code>subtype</code>,
     * optionally a quality factor parameter <code>q</code> and other arbitrary parameters.
     */
    public class MediaRange implements Comparable<MediaRange> {
        private String supertype;
        private double q = 1;
        private Map<String, String> parameters;
        private String subtype;

        /**
         * Constructs a <code>MediaRange</code> from a <code>String</code> expression.
         * @param exp The <code>String</code> to constuct the <code>MediaRange</code> from. The string is
         * expected to be on the form ( "*&#47;*"
         *               | ( type "/" "*" )
         *               | ( type "/" subtype )
         *               ) *( ";" parameter )<br/>
         * as specified by RFC 2616, section 14.1. <br/>
         * Examples:
         * <ul>
         * <li><code>text/html;q=0.8</code></li>
         * <li><code>text/html</code></li>
         * <li><code>text/html;level=3</code></li>
         * <li><code>text/html;level=3;q=0.7</code></li>
         * <li><code>text/*</code></li>
         * <li><code>*&#47;*</code></li>
         * </ul>
         * Note that if the supertype component is wildcard (<code>*</code>), then the subtype component
         * must also be wildcard.<br />
         * The quality factor parameter must be between <code>0</code> and <code>1</code>, inclusive
         * (see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.9">RFC 2616 section 3.9</a>).
         * If the expression does not contain a <code>q</code> parameter, the <code>MediaRange</code> is given
         * a default quality factor of <code>1</code>.
         * @throws IllegalArgumentException if <code>exp</code> can not be parsed to a valid media range
         * @throws NullPointerException if <code>exp</code> is <code>null</code>
         */
        public MediaRange(String exp) {
            String[] parts = exp.split(";");
            this.setType(parts[0].trim());
            if (parts.length > 1) {
                this.parameters  = new HashMap<String, String>(parts.length - 1);
            }
            for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
                String parameter = parts[i];
                String[] keyValue = parameter.split("=");
                if (keyValue[0].equals("q")) {
                    this.q = Double.parseDouble(keyValue[1]);
                    if (this.q < 0 || this.q > 1) {
                        throw new IllegalArgumentException("Quality factor out of bounds: " + exp);
                    }
                }
                this.parameters.put(keyValue[0], keyValue[1]);
            }
        }

        /**
         * Constructs a <code>MediaRange</code> of the given <code>supertype</code> and <code>subtype</code>.
         * The quality factor is given the default value of <code>1</code>.
         * @param supertype The super type of the media range
         * @param subtype The sub type of the media range
         */
        MediaRange(String supertype, String subtype) {
            this.setType(supertype, subtype);
        }


        /**
         * Returns <code>true</code> if this is a catch-all media range (<code>*&#47;*</code>).
         * @return <code>true</code> if this range is a catch-all media range, <code>false</code> otherwise
         */
        public boolean matchesAll() {
            return this.supertype.equals(WILDCARD) && this.subtype.equals(WILDCARD);
        }

        private void setType(String supertype, String subtype) {
            this.supertype = supertype == null ? WILDCARD : supertype;
            this.subtype = subtype == null ? WILDCARD : subtype;
            if (this.supertype.equals(WILDCARD) && !this.subtype.equals(WILDCARD)) {
                throw new IllegalArgumentException("Supertype cannot be wildcard if subtype is not");
            }
        }

        private void setType(String typeDef) {
            String[] parts = typeDef.split("/");
            String superType = parts[0];
            String subType = WILDCARD;
            if(parts.length > 1){
                subType = parts[1];
            }
            this.setType(superType,subType);
        }

        MediaRange(String supertype, String subtype, double q) {
            this(supertype, subtype);
            this.q = q;
        }


        public String getParameter(String key) {
            if (parameters != null) {
                return parameters.get(key);
            } else {
                return null;
            }
        }

        public String getSupertype() {
            return supertype;
        }

        public String getSubtype() {
            return subtype;
        }

        /**
         * Get the value of the quality factor parameter (<code>q</code>).
         * @return the quality factor
         */
        public double getQ() {
            return q;
        }

        public Map<String, String> getParameters() {
            return parameters != null ? parameters : new HashMap<String, String>(0);
        }

        /* -- Comparable implementation -- */
        public int compareTo(MediaRange o) {
            double diff = this.q - o.getQ();
            if (diff == 0) {
                // Compare parameters
                int paramDiff = o.getParameters().size() - this.getParameters().size();
                if (paramDiff != 0) {
                    return paramDiff;
                }
                // Compare wildcards
                if (this.supertype.equals(WILDCARD) && !o.getSupertype().equals(WILDCARD)) {
                    return 1;
                } else if (!this.supertype.equals(WILDCARD) && o.getSupertype().equals(WILDCARD)) {
                    return -1;
                }
                if (this.subtype.equals(WILDCARD) && !o.getSubtype().equals(WILDCARD)) {
                    return 1;
                } else if (!this.subtype.equals(WILDCARD) && o.getSubtype().equals(WILDCARD)) {
                    return -1;
                }
                // Compare names
                return this.toString().compareTo(o.toString());
            } else {
                return diff > 0 ? -1 : 1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MediaRange) {
                MediaRange mr = (MediaRange) obj;
                return mr.getSupertype().equals(this.supertype) && mr.getSubtype().equals(this.subtype);
            }
            return super.equals(obj);
        }

        public boolean equals(String s) {
            return (this.supertype + "/" + this.subtype).equals(s);
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder(this.supertype);
            buf.append('/');
            buf.append(this.subtype);
            if (parameters != null) {
                String delimiter = ";";
                for (String key : parameters.keySet()) {
                    buf.append(delimiter);
                    buf.append(key).append("=").append(parameters.get(key));
                }
            }
            return buf.toString();
        }
    }
}
