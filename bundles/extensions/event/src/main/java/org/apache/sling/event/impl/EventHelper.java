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
package org.apache.sling.event.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Helper class defining some constants and utility methods.
 */
public abstract class EventHelper {

    /** The name of the thread pool for the eventing stuff. */
    public static final String THREAD_POOL_NAME = "SLING_EVENTING";

    /** The namespace prefix. */
    public static final String EVENT_PREFIX = "slingevent:";

    public static final String NODE_PROPERTY_TOPIC = "slingevent:topic";
    public static final String NODE_PROPERTY_APPLICATION = "slingevent:application";
    public static final String NODE_PROPERTY_CREATED = "slingevent:created";
    public static final String NODE_PROPERTY_PROPERTIES = "slingevent:properties";
    public static final String NODE_PROPERTY_PROCESSOR = "slingevent:processor";
    public static final String NODE_PROPERTY_JOBID = "slingevent:id";
    public static final String NODE_PROPERTY_FINISHED = "slingevent:finished";
    public static final String NODE_PROPERTY_TE_EXPRESSION = "slingevent:expression";
    public static final String NODE_PROPERTY_TE_DATE = "slingevent:date";
    public static final String NODE_PROPERTY_TE_PERIOD = "slingevent:period";

    public static final String EVENT_NODE_TYPE = "slingevent:Event";
    public static final String JOB_NODE_TYPE = "slingevent:Job";
    public static final String TIMED_EVENT_NODE_TYPE = "slingevent:TimedEvent";

    /** The nodetype for newly created intermediate folders */
    public static final String NODETYPE_FOLDER = "sling:Folder";

    /** The nodetype for newly created folders */
    public static final String NODETYPE_ORDERED_FOLDER = "sling:OrderedFolder";

    /** Allowed characters for a node name */
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz0123456789_,.-+*#!¤$%&()=[]?";
    /** Replacement characters for unallowed characters in a node name */
    private static final char REPLACEMENT_CHAR = '_';

    /**
     * Filter the node name for not allowed characters and replace them.
     * @param nodeName The suggested node name.
     * @return The filtered node name.
     */
    public static String filter(final String nodeName) {
        final StringBuffer sb  = new StringBuffer();
        char lastAdded = 0;

        for(int i=0; i < nodeName.length(); i++) {
            final char c = nodeName.charAt(i);
            char toAdd = c;

            if (ALLOWED_CHARS.indexOf(c) < 0) {
                if (lastAdded == REPLACEMENT_CHAR) {
                    // do not add several _ in a row
                    continue;
                }
                toAdd = REPLACEMENT_CHAR;

            } else if(i == 0 && Character.isDigit(c)) {
                sb.append(REPLACEMENT_CHAR);
            }

            sb.append(toAdd);
            lastAdded = toAdd;
        }

        if (sb.length()==0) {
            sb.append(REPLACEMENT_CHAR);
        }

        return sb.toString();
    }

    /**
     * used for the md5
     */
    public static final char[] hexTable = "0123456789abcdef".toCharArray();

    /**
     * Calculate an MD5 hash of the string given using 'utf-8' encoding.
     *
     * @param data the data to encode
     * @return a hex encoded string of the md5 digested input
     */
    public static String md5(String data) {
        try {
            return digest("MD5", data.getBytes("utf-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("MD5 digest not available???");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("UTF8 digest not available???");
        }
    }

    /**
     * Digest the plain string using the given algorithm.
     *
     * @param algorithm The alogrithm for the digest. This algorithm must be
     *                  supported by the MessageDigest class.
     * @param data      the data to digest with the given algorithm
     * @return The digested plain text String represented as Hex digits.
     * @throws java.security.NoSuchAlgorithmException if the desired algorithm is not supported by
     *                                  the MessageDigest class.
     */
    private static String digest(String algorithm, byte[] data)
    throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(data);
        StringBuffer res = new StringBuffer(digest.length * 2);
        for (int i = 0; i < digest.length; i++) {
            byte b = digest[i];
            res.append(hexTable[(b >> 4) & 15]);
            res.append(hexTable[b & 15]);
        }
        return res.toString();
    }
}
