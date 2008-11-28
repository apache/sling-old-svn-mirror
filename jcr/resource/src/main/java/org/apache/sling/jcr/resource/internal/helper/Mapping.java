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

import java.util.StringTokenizer;

/**
 * The <code>Mapping</code> class conveys the mapping configuration used by
 * the
 * {@link org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl}.
 */
public class Mapping {

    /**
     * defines the 'inbound' direction, that is mapping request path to item
     * path
     */
    public static final int INBOUND = 1;

    /** defined the 'outbound' direction, that is mapping item path to URL path */
    public static final int OUTBOUND = 2;

    /** defines the 'both' direction */
    public static final int BOTH = 3;

    /** Simple mapper instance mapping path to URLs 1:1 in both directions */
    public static final Mapping DIRECT = new Mapping("", "", BOTH) {

        @Override
        public String mapHandle(String handle) {
            return handle;
        }

        @Override
        public boolean mapsInbound() {
            return true;
        }

        @Override
        public boolean mapsOutbound() {
            return true;
        }

        @Override
        public String mapUri(String uriPath) {
            return uriPath;
        }
    };

    /** the 'from' (inside, repository) mapping */
    private final String from;

    /** the 'to' (outside, URL) mapping */
    private final String to;

    /** the length of the 'from' field */
    private final int fromLength;

    /** the length of the 'to' field */
    private final int toLength;

    /** the mapping direction */
    private final int direction;

    /**
     * Creates a new instance of a mapping.
     *
     * @param from Handle prefix possible valid in the ContentBus.
     * @param to URI path prefix to be replaced by from to get a possibly valid
     *            handle.
     * @param dir the direction of the mapping. either "inwards", "outwards" or
     *            "both".
     * @throws NullPointerException if either <code>from</code> or
     *             <code>to</code> is <code>null</code>.
     */
    public Mapping(String from, String to, String dir) {
        this(from, to, "in".equals(dir) ? Mapping.INBOUND : ("out".equals(dir)
                ? Mapping.OUTBOUND
                : Mapping.BOTH));
    }

    /**
     * Creates a new instance of a mapping.
     *
     * @param from Handle prefix possible valid in the ContentBus.
     * @param to URI path prefix to be replaced by from to get a possibly valid
     *            handle.
     * @throws NullPointerException if either <code>from</code> or
     *             <code>to</code> is <code>null</code>.
     */
    public Mapping(String from, String to) {
        this(from, to, Mapping.BOTH);
    }

    public Mapping(String config) {
        this(split(config));
    }

    public Mapping(String[] parts) {
        this.from = parts[0];
        this.to = parts[2];
        this.fromLength = this.from.length();
        this.toLength = this.to.length();

        this.direction = ">".equals(parts[1])
                ? Mapping.INBOUND
                : ("<".equals(parts[1]) ? Mapping.OUTBOUND : Mapping.BOTH);
    }
    
    @Override
    public String toString() {
        return "Mapping (from=" + from + ", to=" + to + ", direction=" + direction 
            + ", lengths=" + fromLength + "/" + toLength;
    }

    /**
     * Replaces the prefix <em>to</em> by the new prefix <em>from</em>, if
     * and only if <code>uriPath</code> starts with the <em>to</em> prefix.
     * If <code>uriPath</code> does not start with the <em>to</em> prefix,
     * or if this mapping is not defined as a 'inward' mapping,
     * <code>null</code> is returned.
     *
     * @param uriPath The URI path for which to replace the <em>to</em> prefix
     *            by the <em>from</em> prefix.
     * @return The string after replacement or <code>null</code> if the
     *         <code>uriPath</code> does not start with the <em>to</em>
     *         prefix, or {@link #mapsInbound()} returns <code>false</code>.
     */
    public String mapUri(String uriPath) {
        return (this.mapsInbound() && uriPath.startsWith(this.to)) ? this.from
            + uriPath.substring(this.toLength) : null;
    }

    /**
     * Replaces the prefix <em>from</em> by the new prefix <em>to</em>, if
     * and only if <code>handle</code> starts with the <em>from</em> prefix.
     * If <code>uriPath</code> does not start with the <em>from</em> prefix,
     * or if this mapping is not defined as a 'outward' mapping,
     * <code>null</code> is returned.
     *
     * @param handle The URI path for which to replace the <em>from</em>
     *            prefix by the <em>to</em> prefix.
     * @return The string after replacement or <code>null</code> if the
     *         <code>handle</code> does not start with the <em>from</em>
     *         prefix, or {@link #mapsOutbound()} returns <code>false</code>.
     */
    public String mapHandle(String handle) {
        return (this.mapsOutbound() && handle.startsWith(this.from)) ? this.to
            + handle.substring(this.fromLength) : null;
    }

    // TODO: temporary
    public String getFrom() {
        return from;
    }
    
    // TODO: temporary
    public String getTo() {
        return to;
    }
    
    /**
     * Checks, if this mapping is defined for inbound mapping.
     *
     * @return <code>true</code> if this mapping is defined for inbound
     *         mapping; <code>false</code> otherwise
     */
    public boolean mapsInbound() {
        return (this.direction & Mapping.INBOUND) > 0;
    }

    /**
     * Checks, if this mapping is defined for outbound mapping.
     *
     * @return <code>true</code> if this mapping is defined for outbound
     *         mapping; <code>false</code> otherwise
     */
    public boolean mapsOutbound() {
        return (this.direction & Mapping.OUTBOUND) > 0;
    }

    /**
     * Constructs a new mapping with the given mapping string and the direction
     */
    private Mapping(String from, String to, int dir) {
        this.from = from;
        this.to = to;
        this.fromLength = from.length();
        this.toLength = to.length();
        this.direction = dir;
    }

    public static String[] split(String map) {
        String[] res = new String[3]; // src, op, dst
        StringTokenizer st = new StringTokenizer(map, "-<>", true);

        for (int i = 0; i < res.length; i++) {
            res[i] = st.hasMoreTokens() ? st.nextToken() : "";
        }

        return res;
    }

}
