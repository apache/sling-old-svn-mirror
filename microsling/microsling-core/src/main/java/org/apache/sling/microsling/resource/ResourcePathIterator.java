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
package org.apache.sling.microsling.resource;

import java.util.Iterator;

/** Iterate over the the HTTP request path by creating shorter segments
 *  of that path using "." and "/" as separators.
 *  
 *  For example, if path = /some/stuff.a4.html/xyz the sequence is:
 *  <ol>
 *    <li>
 *      /some/stuff.a4.html/xyz
 *    </li>
 *    <li>
 *      /some/stuff.a4.html
 *    </li>
 *    <li>
 *      /some/stuff.a4
 *    </li>
 *    <li>
 *      /some/stuff
 *    </li>
 *    <li>
 *      /some
 *    </li>
 *  </ol>
 *  
 *  The above rules are not valid for GET or HEAD requests, where
 *  we do not go up the path: for those requests, the path is
 *  only split at dots that follow the last slash
 */
class ResourcePathIterator implements Iterator<String> {

    private String nextPath;
    private final String httpMethod;
    private final int lastSlashPos;
    
    ResourcePathIterator(String path,String httpMethod) {
        nextPath = path;
        this.httpMethod = httpMethod;
        lastSlashPos = (path == null ? -1 : path.lastIndexOf('/'));
    }
    
    public boolean hasNext() {
        return nextPath != null && nextPath.length() > 0;
    }

    public String next() {
        final String result = nextPath;
        
        int pos = -1;
        if("GET".equals(httpMethod) || "HEAD".equals(httpMethod)) {
            // SLING-117: for GET and POST, do not go up the path to resolve resources,
            // only split at dots that follow the last slash
            pos = result.lastIndexOf('.');
            if(pos < lastSlashPos) {
                pos = -1;
            }
        } else {
            pos = Math.max(result.lastIndexOf('.'),result.lastIndexOf('/'));
        }
        
        if(pos < 0) {
            nextPath = null;
        } else {
            nextPath = result.substring(0,pos);
        }
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("Cannot remove() on ResourcePathIterator");
    }

}
