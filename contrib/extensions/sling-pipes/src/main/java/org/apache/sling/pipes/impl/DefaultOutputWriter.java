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
package org.apache.sling.pipes.impl;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.pipes.OutputWriter;
import org.apache.sling.pipes.Pipe;

/**
 * default output writer with size and output resources' path
 */
public class DefaultOutputWriter implements OutputWriter {

    protected JSONWriter writer;

    protected Pipe pipe;

    @Override
    public boolean handleRequest(SlingHttpServletRequest request) {
        return true;
    }

    @Override
    public void init(SlingHttpServletRequest request, SlingHttpServletResponse response, Pipe pipe) throws IOException, JSONException {
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json");
        writer = new JSONWriter(response.getWriter());
        this.pipe = pipe;
        writer.object();
        writer.key(KEY_ITEMS);
        writer.array();
    }

    @Override
    public void writeItem(Resource resource) throws JSONException {
        writer.value(resource.getPath());
    }

    @Override
    public void ends(int size) throws JSONException {
        writer.endArray();
        writer.key(KEY_SIZE).value(size);
        writer.endObject();
    }
}
