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
 */
class ResourcePathIterator implements Iterator<String> {

    private String nextPath;
    
    ResourcePathIterator(String path) {
        nextPath = path;
    }
    
    public boolean hasNext() {
        return nextPath != null && nextPath.length() > 0;
    }

    public String next() {
        final String result = nextPath;
        final int pos = Math.max(result.lastIndexOf('.'),result.lastIndexOf('/'));
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
