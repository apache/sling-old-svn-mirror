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
package org.apache.sling.reqanalyzer.impl.gui;

public class RequestTrackerFileEntry {

    /*
     * Request Tracker Info Line:
     * 
     * :1340236961239:235:GET:/healthck/healthck.html:text/html; charset=UTF-8:200
     */

    private final long start;
    private final long duration;
    private final String method;
    private final String url;
    private final String contentType;
    private final int status;
    private final long offset;

    static int getColumnCount() {
        return 6;
    }

    static String getColumnName(final int idx) {
        switch (idx) {
            case 0:
                return "Start";
            case 1:
                return "Duration";
            case 2:
                return "Method";
            case 3:
                return "URL";
            case 4:
                return "Content Type";
            case 5:
                return "Status";
            default:
                throw new IllegalArgumentException("Unknown field index " + idx);
        }
    }

    static Class<?> getColumnClass(final int idx) {
        switch (idx) {
            case 0:
                return Long.class;
            case 1:
                return Long.class;
            case 2:
                return String.class;
            case 3:
                return String.class;
            case 4:
                return String.class;
            case 5:
                return Integer.class;
            default:
                throw new IllegalArgumentException("Unknown field index " + idx);
        }
    }

    RequestTrackerFileEntry(final String statusLine, final long offset) {
        String[] parts = statusLine.split(":");
        this.start = Long.parseLong(parts[1]);
        this.duration = Long.parseLong(parts[2]);
        this.method = parts[3];
        
        String url = parts[4];
        for (int i = 5; i < parts.length - 2; i++) {
            url += ":" + parts[i];
        }
        this.url = url;
        
        this.contentType = parts[parts.length-2];
        this.status = Integer.parseInt(parts[parts.length-1]);
        this.offset = offset;
    }

    Object getField(final int idx) {
        switch (idx) {
            case 0:
                return start;
            case 1:
                return duration;
            case 2:
                return method;
            case 3:
                return url;
            case 4:
                return contentType;
            case 5:
                return status;
            default:
                throw new IllegalArgumentException("Unknown field index " + idx);
        }
    }

    long getOffset() {
        return offset;
    }
}
