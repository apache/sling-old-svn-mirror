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
package org.apache.sling.testing.tools.sling;

import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;

/**
 * Interface used to communicate with a sling instance
 */
public interface SlingInstance {

    /** Start server if needed, and return a RequestBuilder that points to it */
    public RequestBuilder getRequestBuilder();


    /** Start server if needed, and return its base URL */
    public String getServerBaseUrl();


    /** Return username configured for execution of HTTP requests */
    public String getServerUsername();

    /** Return password configured for execution of HTTP requests */
    public String getServerPassword();


    /** Returns a RequestExecutor for this server **/
    public RequestExecutor getRequestExecutor();
}
