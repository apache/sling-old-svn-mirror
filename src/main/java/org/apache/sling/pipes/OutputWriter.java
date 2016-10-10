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
package org.apache.sling.pipes;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;

import java.io.IOException;

/**
 * defines how pipe's output get written to a servlet response
 */
public interface OutputWriter {

    String KEY_SIZE = "size";

    String KEY_ITEMS = "items";

    /**
     *
     * @param request current request
     * @return true if this writer handles that request
     */
    boolean handleRequest(SlingHttpServletRequest request);

    /**
     * Init the writer, writes beginning of the output
     * @param request request from which writer will output
     * @param response response on which writer will output
     * @param pipe pipe whose output will be written
     * @throws IOException error handling streams
     * @throws JSONException in case invalid json is written
     */
    void init(SlingHttpServletRequest request, SlingHttpServletResponse response, Pipe pipe) throws IOException, JSONException;

    /**
     * Write a given resource
     * @param resource resource that will be written
     * @throws JSONException in case write fails
     */
    void writeItem(Resource resource) throws JSONException;

    /**
     * writes the end of the output
     * @param size size of the overall result
     * @throws JSONException in case invalid json is written
     */

    void ends(int size) throws JSONException;
}
