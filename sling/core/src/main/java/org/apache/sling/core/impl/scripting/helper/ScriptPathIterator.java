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
package org.apache.sling.core.impl.scripting.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.core.impl.scripting.DefaultSlingScriptResolver;

public class ScriptPathIterator implements Iterator<String> {

    private final int minPathLength;

    private String nextPath;

    public ScriptPathIterator(SlingHttpServletRequest request) {
        final StringBuffer sb = new StringBuffer();

        // the default path root
        sb.append(DefaultSlingScriptResolver.SCRIPT_BASE_PATH).append('/');

        // append the resource type derived path
        sb.append(request.getResource().getResourceType().replaceAll("\\:", "/"));

        minPathLength = sb.length();

        // add selectors in front of the filename if any, replacing dots in them
        // by slashes so that print.a4 becomes print/a4/
        String selectors = request.getRequestPathInfo().getSelectorString();
        if (selectors != null) {
            sb.append('/');
            sb.append(selectors.toLowerCase().replace('.', '/'));
        }

        nextPath = sb.toString();
    }

    public boolean hasNext() {
        return nextPath != null;
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        String result = nextPath;
        nextPath = seek();
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    private String seek() {
        int lastSlash = nextPath.lastIndexOf('/');
        return (lastSlash >= minPathLength)
                ? nextPath.substring(0, lastSlash)
                : null;
    }
}
