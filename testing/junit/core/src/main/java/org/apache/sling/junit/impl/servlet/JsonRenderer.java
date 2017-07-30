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
package org.apache.sling.junit.impl.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.RendererFactory;
import org.apache.sling.junit.SlingTestContextProvider;
import org.apache.sling.junit.TestSelector;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Json renderer for JUnit servlet */
@Component(immediate=false)
@Service
public class JsonRenderer extends RunListener implements Renderer,RendererFactory {

    public static final String EXTENSION = "json";
    public static final String INFO_TYPE_KEY = "INFO_TYPE";
    public static final String INFO_SUBTYPE_KEY = "INFO_SUBTYPE";
    public static final String TEST_METADATA = "test_metadata";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private JsonGenerator writer;
    
    public Renderer createRenderer() { 
        return new JsonRenderer();
    }

    public boolean appliesTo(TestSelector selector) {
        return EXTENSION.equals(selector.getExtension());
    }

    public String getExtension() {
        return EXTENSION;
    }

    public void setup(HttpServletResponse response, String pageTitle) throws IOException, UnsupportedEncodingException {
        if(writer != null) {
            throw new IllegalStateException("Output Writer already set");
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        writer = Json.createGenerator(response.getWriter());
        try {
            writer.writeStartArray();
        } catch(JsonException jex) {
            throw (IOException)new IOException().initCause(jex);
        }
    }

    public void cleanup() {
        if(writer != null) {
            try {
                writer.writeEnd();
                writer.flush();
            } catch(JsonException jex) {
                log.warn("JsonException in cleanup()", jex);
            }
        }
        writer = null;
    }

    public void info(String cssClass, String info) {
        try {
            startItem("info");
            writer.write(INFO_SUBTYPE_KEY, cssClass);
            writer.write("info", info);
            endItem();
        } catch(JsonException jex) {
            log.warn("JsonException in info()", jex);
        }
    }

    public void list(String cssClass, Collection<String> data) {
        try {
            startItem("list");
            writer.write(INFO_SUBTYPE_KEY, cssClass);
            writer.writeStartArray("data");
            for(String str : data) {
                writer.write(str);
            }
            writer.writeEnd();
            endItem();
        } catch(JsonException jex) {
            log.warn("JsonException in list()", jex);
        }
    }

    public void title(int level, String title) {
        // Titles are not needed in Json
    }
    
    public void link(String info, String url, String method) {
        try {
            startItem("link");
            writer.write("info",info);
            writer.write("method",method);
            writer.write("url",url);
            endItem();
        } catch(JsonException jex) {
            log.warn("JsonException in link()", jex);
        }
    }

    public RunListener getRunListener() {
        return this;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
        startItem("test");
        writer.write("description",description.toString());
    }
    
    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        if(SlingTestContextProvider.hasContext()) {
            outputContextMap(SlingTestContextProvider.getContext().output());
        }
        endItem();
    }

    
    @Override
    public void testFailure(Failure failure) throws Exception {
        writer.write("failure",failure.toString());
        writer.write("trace",failure.getTrace());
    }
    
    @Override
    public void testRunFinished(Result result) throws Exception {
        // Not needed, info is already present in the output
    }
    
    void startItem(String name) throws JsonException {
        writer.writeStartObject();
        writer.write(INFO_TYPE_KEY,name);
    }
    
    void endItem() throws JsonException {
        writer.writeEnd();
    }
    
    void outputContextMap(Map<String, Object> data) throws JsonException {
        writer.writeStartObject(TEST_METADATA);
        try {
            for(Map.Entry<String, Object> e : data.entrySet()) {
                Object value = e.getValue();
                if (value instanceof Long) {
                    writer.write(e.getKey(), (Long)e.getValue());
                }
                else if (value instanceof Integer) {
                    writer.write(e.getKey(), (Integer)e.getValue());
                }
                else if (value instanceof Double) {
                    writer.write(e.getKey(), (Double)e.getValue());
                }
                else if (value instanceof Boolean) {
                    writer.write(e.getKey(), (Boolean)e.getValue());
                }
                else if (value instanceof String) {
                    writer.write(e.getKey(), (String)e.getValue());
                }
                else {
                    throw new IllegalArgumentException("Unexpected value for JSON: " + value);
                }
            }
        } finally {
            writer.writeEnd();
        }
    }
}