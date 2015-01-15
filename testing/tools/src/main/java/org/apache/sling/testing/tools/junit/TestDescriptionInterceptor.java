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
package org.apache.sling.testing.tools.junit;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.slf4j.MDC;

/**
 * HttpClient interceptor that propagates the current test name as part HTTP request headers.
 * Headers can then be logged, exported as MDC info etc. by {@link TestNameLoggingFilter}.
 * 
 * Meant to help in correlating the server side logs with the test case being executed.
 *
 * @see MDC http://www.slf4j.org/manual.html
 */
public class TestDescriptionInterceptor implements HttpRequestInterceptor {
    private static final String SLING_HEADER_PREFIX = "X-Sling-";

    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        addSlingHeaders(httpRequest);
    }

    /**
     * Adds all MDC key-value pairs as HTTP header where the key starts
     * with 'X-Sling-'
     */
    private static void addSlingHeaders(HttpRequest m){
        Map<?,?> mdc = MDC.getCopyOfContextMap();
        if (mdc != null) {
            for (Map.Entry<?, ?> e : mdc.entrySet()) {
                Object key = e.getKey();
                if (key instanceof String
                        && ((String)key).startsWith(SLING_HEADER_PREFIX)
                        && e.getValue() instanceof String) {
                    m.addHeader((String)key, (String)e.getValue());
                }
            }
        }
    }
}