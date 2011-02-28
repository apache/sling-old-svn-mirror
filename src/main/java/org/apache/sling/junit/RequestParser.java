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
    private final String methodName;
    private final String extension;
    private final HttpServletRequest request;
    private static final String EMPTY_STRING = "";

    /** Build from a request using standard parsing */
    public RequestParser(HttpServletRequest request) {
        this(parsePathInfo(request.getPathInfo()), request);
    }
    
    /** Build from pre-parsed values */
    public RequestParser(HttpServletRequest request, String testSelector, String outputExtension, String testMethodName) {
        this.request = request;
        this.testSelector = testSelector;
        this.extension = outputExtension;
        this.methodName = testMethodName;
    }
    
    RequestParser(String [] s, HttpServletRequest request) {
        this.request = request;
        testSelector = s[0];
        extension = s[1];
        methodName = s[2];
    }
    
    static String [] parsePathInfo(String pathInfo) {
        final String [] result = new String[3];
        
        if (pathInfo != null) {
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            
            // Split at last dot to find extension
            {
                final int pos = pathInfo.lastIndexOf('.');
                if (pos >= 0) {
                    result[0] = pathInfo.substring(0, pos);
                    result[1] = pathInfo.substring(pos+1);
                } else {
                    result[0] = pathInfo;
                }
            }
            
            // And if extension contains a /, what follows is test method selector
            final String ext = result[1];
            if(ext != null) {
                final int pos = ext.indexOf('/');
                if(pos >= 0) {
                    result[1] = ext.substring(0, pos);
                    result[2] = ext.substring(pos+1);
                }
            }
        }
        
        for(int i=0; i < result.length; i++) {
            if(result[i] == null) {
                result[i] = EMPTY_STRING;
            }
        }
        
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() 
                + ", testSelector [" + testSelector + "]"
                + ", methodName [" + methodName + "]"
                + ", extension [" + extension + "]"
                ;
    }

    public String getTestSelector() {
        return testSelector;
    }

    public String getExtension() {
        return extension;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public HttpServletRequest getRequest() {
        return request;
    }   
}