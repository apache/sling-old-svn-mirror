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

    public static final String EVENTS_NODE_TYPE = "slingevent:Events";
    public static final String EVENT_NODE_TYPE = "slingevent:Event";
    public static final String JOBS_NODE_TYPE = "slingevent:Jobs";
    public static final String JOB_NODE_TYPE = "slingevent:Job";
    public static final String TIMED_EVENTS_NODE_TYPE = "slingevent:TimedEvents";
    public static final String TIMED_EVENT_NODE_TYPE = "slingevent:TimedEvent";

    /** The nodetype for newly created folders */
    public static final String NODETYPE_FOLDER = "sling:Folder";

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

}
