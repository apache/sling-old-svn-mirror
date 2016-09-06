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
     * @param request
     * @return
     */
    boolean handleRequest(SlingHttpServletRequest request);

    /**
     * Init the writer
     * @param response
     */
    void init(SlingHttpServletRequest request, SlingHttpServletResponse response, Pipe pipe) throws IOException, JSONException;

    /**
     * Write a given resource
     * @param resource
     */
    void writeItem(Resource resource) throws JSONException;

    /**
     * ends write
     */
    void ends(int size) throws JSONException;
}