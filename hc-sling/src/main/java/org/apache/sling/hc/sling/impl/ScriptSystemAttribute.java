/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.sling.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.SystemAttribute;

/** Creates {@link Rule} that executes a Sling script and
 *  returns its output, filtered to omit comments and blank lines 
 */   
class ScriptSystemAttribute implements SystemAttribute {
    
    /** A script must return only this line to be successful, any
     *  other lines besides hash-prefixed comments and empty lines
     *  are considered errors 
     */
    public static final String SUCCESS_STRING = "TEST_PASSED";
    
    private final SlingScript script;
    private final SlingRequestProcessor requestProcessor;
    
    ScriptSystemAttribute(SlingRequestProcessor processor, SlingScript script) {
        this.script = script;
        this.requestProcessor = processor;
    }

    @Override
    public String toString() {
        return script.getScriptResource().getPath();
    }
    
    @Override
    public Object getValue() {
        try {
            final HttpRequest req = new HttpRequest(script.getScriptResource().getPath());
            final HttpResponse resp = new HttpResponse();
            requestProcessor.processRequest(req, resp, script.getScriptResource().getResourceResolver());
            if(resp.getStatus() != HttpServletResponse.SC_OK) {
                return "Unexpected requests status: " + resp.getStatus();
            }
            return filterContent(resp.getContent());
        } catch(Exception e) {
            return e.toString();
        }
    }
    
    static String filterContent(String content) throws IOException {
        final BufferedReader br = new BufferedReader(new StringReader(content));
        String line = null;
        final StringBuilder result = new StringBuilder();
        while( (line = br.readLine()) != null) {
            line = line.trim();
            if(line.length() == 0) {
                // ignore
            } else if(line.startsWith("#")) {
                // ignore
            } else {
                if(result.length() > 0) {
                    result.append('\n');
                }
                result.append(line);
            }
        }
        return result.toString();
    }
}
