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
public abstract class OutputWriter {

    public static final String KEY_SIZE = "size";

    public static final String KEY_ITEMS = "items";

    public static final String PARAM_SIZE = KEY_SIZE;

    public static final int NB_MAX = 10;

    protected int size;

    protected int max = NB_MAX;

    protected Pipe pipe;

    /**
     *
     * @param request current request
     * @return true if this writer handles that request
     */
    public abstract boolean handleRequest(SlingHttpServletRequest request);

    /**
     * Init the writer, writes beginning of the output
     * @param request request from which writer will output
     * @param response response on which writer will output
     * @throws IOException error handling streams
     * @throws JSONException in case invalid json is written
     */
    public void init(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, JSONException{
        max = request.getParameter(PARAM_SIZE) != null ? Integer.parseInt(request.getParameter(PARAM_SIZE)) : NB_MAX;
        if (max < 0) {
            max = Integer.MAX_VALUE;
        }
        initInternal(request, response);
    }

    /**
     * Init the writer, writes beginning of the output
     * @param request request from which writer will output
     * @param response response on which writer will output
     * @throws IOException error handling streams
     * @throws JSONException in case invalid json is written
     */
    protected abstract void initInternal(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, JSONException;

    /**
     * Write a given resource
     * @param resource resource that will be written
     * @throws JSONException in case write fails
     */
    public void write(Resource resource) throws JSONException{
        if (size++ < max) {
            writeItem(resource);
        }
    }

    /**
     * Write a given resource
     * @param resource resource that will be written
     * @throws JSONException in case write fails
     */
    protected abstract void writeItem(Resource resource) throws JSONException;

    /**
     * writes the end of the output
     * @throws JSONException in case invalid json is written
     */

    public abstract void ends() throws JSONException;

    /**
     *
     * @param pipe
     */
    public void setPipe(Pipe pipe) {
        this.pipe = pipe;
    }
}
