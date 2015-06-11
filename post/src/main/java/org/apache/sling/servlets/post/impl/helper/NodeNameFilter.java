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


/**
 * Filter a String so that it can be used as a NodeName.
 */
public class NodeNameFilter {

    public static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_";
    public static final char REPLACEMENT_CHAR = '_';

    public String filter(String nodeName) {
        final StringBuilder sb  = new StringBuilder();
        char lastAdded = 0;

        nodeName = nodeName.toLowerCase();
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
