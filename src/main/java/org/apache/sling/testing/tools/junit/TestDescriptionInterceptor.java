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

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.junit.runner.Description;

import java.io.IOException;

/**
 * HttpClient interceptor that propagates the current test name as part HTTP request headers.
 * Headers can then be logged, exported as MDC info etc. by {@link TestNameLoggingFilter}.
 * 
 * Meant to help in correlating the server side logs with the test case being executed.
 *
 * @see MDC http://www.slf4j.org/manual.html
 */
public class TestDescriptionInterceptor implements HttpRequestInterceptor{
    public static final String TEST_NAME_HEADER = "sling.test.name";
    public static final String TEST_CLASS_HEADER = "sling.test.class";

    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
       final Description desc = TestDescriptionRule.getCurrentTestDescription();
        if(desc != null){
            httpRequest.addHeader(TEST_NAME_HEADER,desc.getMethodName());
            httpRequest.addHeader(TEST_CLASS_HEADER,desc.getClassName());
        }
    }
}