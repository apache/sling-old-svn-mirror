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
package org.apache.sling.junit;

import javax.servlet.http.HttpServletRequest;

/** Parse information from a request, to define which
 *  tests to run and which renderer to select.
 *  
 *  We do not use the Sling API to to that, in order to 
 *  keep the junit core module reusable in other OSGi 
 *  environments.
 */
public class RequestParser {
    private final String testSelector;
    private final String extension;
    private final HttpServletRequest request;

    public RequestParser(HttpServletRequest request) {
        this.request = request;
        String pathinfo = request.getPathInfo();
        if (pathinfo == null) {
            pathinfo = "";
        } else if (pathinfo.startsWith("/")) {
            pathinfo = pathinfo.substring(1);
        }

        final int pos = pathinfo.lastIndexOf('.');
        if (pos >= 0) {
            testSelector = pathinfo.substring(0, pos);
            extension = pathinfo.substring(pos+1);
        } else {
            testSelector = pathinfo;
            extension = "";
        }
    }

    public String toString() {
        return getClass().getSimpleName() + ": testSelector=[" + testSelector
                + "], extension=[" + extension + "]";
    }

    public String getTestSelector() {
        return testSelector;
    }

    public String getExtension() {
        return extension;
    }
    
    public HttpServletRequest getRequest() {
        return request;
    }
    
}