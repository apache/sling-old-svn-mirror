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
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.RendererFactory;
import org.apache.sling.junit.TestSelector;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Renderer for Sling JUnit server-side testing, which
 *  renders the serialized JUnit Result object.
 */
@Component
@Service
public class JUnitResultRenderer extends RunListener implements Renderer,RendererFactory {

    public static final String EXTENSION = "junit_result";
    private ObjectOutputStream outputStream;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public Renderer createRenderer() { 
        return new JUnitResultRenderer();
    }
    
    public boolean appliesTo(TestSelector s) {
        return EXTENSION.equals(s.getExtension());
    }
    
    public String getExtension() {
        return EXTENSION;
    }

    public void setup(HttpServletResponse response, String pageTitle) 
    throws IOException, UnsupportedEncodingException {
        response.setContentType("application/x-java-serialized-object");
        outputStream = new ObjectOutputStream(response.getOutputStream());
    }
    
    public void cleanup() {
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            log.warn("Exception in cleanup()", e);
        }
        outputStream = null;
    }

    public RunListener getRunListener() {
        return this;
    }

    public void info(String role, String info) {
    }

    public void link(String info, String url, String method) {
    }

    public void list(String role, Collection<String> data) {
    }

    public void title(int level, String title) {
    }
    
    @Override 
    public void testRunFinished(Result result) throws IOException {
        outputStream.writeObject(result);
    }
}