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

@SuppressWarnings("serial")
public class HttpStatusCodeException extends IOException {

    private final int expectedStatus;
    
    private final int actualStatus;
    
    public HttpStatusCodeException(int expectedStatus, int actualStatus,
            String method, String url) {
        this(expectedStatus, actualStatus, method, url, null);
    }

    public HttpStatusCodeException(int expectedStatus, int actualStatus,
            String method, String url, String content) {
        super("Expected status code " + expectedStatus + " for " + method
                + ", got " + actualStatus + ", URL=" + url 
                + (content != null ? ", Content=[" + content + "]" : "")
                );
        this.expectedStatus = expectedStatus;
        this.actualStatus = actualStatus;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }
    
    public int getActualStatus() {
        return actualStatus;
    }
}
