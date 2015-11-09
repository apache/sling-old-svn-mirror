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

package org.apache.sling.commons.testing.integration;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.slf4j.MDC;

/**
 * HttpClient extension which also passes test related headers as part
 * of outgoing HTTP request
 */
public class TestInfoPassingClient extends HttpClient {
    //Defined in org.apache.sling.testing.tools.junit.TestLogRule
    private static final String SLING_HEADER_PREFIX = "X-Sling-";

    @Override
    public int executeMethod(HostConfiguration hostconfig, HttpMethod method,
                             HttpState state) throws IOException {
        addSlingHeaders(method);
        return super.executeMethod(hostconfig, method, state);
    }

    /**
     * Adds all MDC key-value pairs as HTTP header where the key starts
     * with 'X-Sling-'
     */
    private static void addSlingHeaders(HttpMethod m){
        Map<?,?> mdc = MDC.getCopyOfContextMap();
        if (mdc != null) {
            for (Map.Entry<?, ?> e : mdc.entrySet()) {
                Object key = e.getKey();
                if (key instanceof String
                        && ((String)key).startsWith(SLING_HEADER_PREFIX)
                        && e.getValue() instanceof String) {
                    m.addRequestHeader((String) key, (String) e.getValue());
                }
            }
        }
    }
}
