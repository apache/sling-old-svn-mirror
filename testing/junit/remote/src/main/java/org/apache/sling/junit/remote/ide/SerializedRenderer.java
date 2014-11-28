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
package org.apache.sling.junit.remote.ide;

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
 *  renders test results in binary form.
 *  Used to send results, and especially Exceptions, as
 *  is to a remote IDE.      
 */
@Component(immediate=false)
@Service
public class SerializedRenderer extends RunListener implements Renderer,RendererFactory {

    public static final String EXTENSION = "serialized";
    private ObjectOutputStream outputStream;
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** @inheritDoc */
    public Renderer createRenderer() { 
        return new SerializedRenderer();
    }
    
    /** @inheritDoc */
    public boolean appliesTo(TestSelector s) {
        return EXTENSION.equals(s.getExtension());
    }
    
    /** @inheritDoc */
    public String getExtension() {
        return EXTENSION;
    }

    /** @inheritDoc */
    public void setup(HttpServletResponse response, String pageTitle) 
    throws IOException, UnsupportedEncodingException {
        response.setContentType("application/x-java-serialized-object");
        outputStream = new ObjectOutputStream(response.getOutputStream());
    }
    
    /** @inheritDoc */
    public void cleanup() {
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            log.warn("Exception in cleanup()", e);
        }
        outputStream = null;
    }

    /** @inheritDoc */
    public RunListener getRunListener() {
        return this;
    }

    /** @inheritDoc */
    public void info(String role, String info) {
    }

    /** @inheritDoc */
    public void link(String info, String url, String method) {
    }

    /** @inheritDoc */
    public void list(String role, Collection<String> data) {
    }

    /** @inheritDoc */
    public void title(int level, String title) {
    }
    
    @Override 
    public void testRunFinished(Result result) throws IOException {
        final ExecutionResult er = new ExecutionResult(result);
        outputStream.writeObject(er);
    }
}