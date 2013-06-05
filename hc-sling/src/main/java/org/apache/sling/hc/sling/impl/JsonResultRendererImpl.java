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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.sling.api.JsonResultRenderer;

/** Renders a List of EvaluationResult in JSON. See unit tests
 *  for details.
 */
@Component
@Service(value=JsonResultRenderer.class)
public class JsonResultRendererImpl implements JsonResultRenderer {
    
    @Override
    public void render(List<EvaluationResult> results, Writer output) throws IOException {
        final JSONWriter w = new JSONWriter(output);
        w.setTidy(true);
        try {
            w.object();
            w.key("results");
            w.array();
            for(EvaluationResult r : results) {
                w.object();
                {
                    w.key("rule").value(r.getRule().toString());
                    
                    final Map<String, Object> info = r.getRule().getInfo();
                    if(!info.isEmpty()) {
                        w.key("info").array();
                        for(Map.Entry<String, Object> e : info.entrySet()) {
                            w.object();
                            w.key(e.getKey()).value(e.getValue());
                            w.endObject();
                        }
                        w.endArray();
                    }
                    
                    w.key("tags").array();
                    for(String tag : r.getRule().getTags()) {
                        w.value(tag);
                    }
                    w.endArray();
                    
                    if(r.anythingToReport()) {
                        w.key("log").array();
                        {
                            for(EvaluationResult.LogMessage msg : r.getLogMessages()) {
                                w.object();
                                {
                                    w.key(msg.getLevel().toString()).value(msg.getMessage());
                                }
                                w.endObject();
                            }
                        }
                        w.endArray();
                    }
                }
                w.endObject();
            }
            w.endArray();
            w.endObject();
        } catch (JSONException e) {
            throw new IOException(e.getClass().getSimpleName(), e);
        }
    }
}
