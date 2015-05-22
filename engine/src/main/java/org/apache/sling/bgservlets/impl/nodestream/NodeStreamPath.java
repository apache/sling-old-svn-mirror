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
package org.apache.sling.bgservlets.impl.nodestream;


/** Builds sequential hierachical paths to store node 
 *  streams: increment a counter and build path using
 *  CHARS_PER_LEVEL characters of the String value of
 *  the counter per path level.    
 */
class NodeStreamPath {
    private int counter;
    private String path;
    private final int CHARS_PER_LEVEL = 2;
    
    /** Property name to use for streams */
    static final String PROPERTY_NAME = "stream";
    
    /** Select the next path to use, must be
     *  called before using getPath().
     *  Not thread-safe.  */
    void selectNextPath() {
        counter++;
        final StringBuilder sb = new StringBuilder();
        final String str = String.valueOf(counter);
        for(int i = 0; i < str.length(); i++) {
            if(i> 0 && i % CHARS_PER_LEVEL == 0) {
                sb.append('/');
            }
            sb.append(str.charAt(i));
        }
        path = sb.toString();
    }
    
    /** Return the last path computed by selectNextPath() */
    String getNodePath() {
        return path;
    }
}
