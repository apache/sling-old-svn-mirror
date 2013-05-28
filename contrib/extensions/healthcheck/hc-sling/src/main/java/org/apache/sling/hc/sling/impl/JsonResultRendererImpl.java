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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
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
        // TODO: trouble with animalsniffer and JSON dependency
        output.write("TODO: this should be JSON - not implemented yet\n\n");
        for(EvaluationResult r : results) {
            output.write(r.getRule().toString());
            output.write("\n");
            
            if(!r.anythingToReport()) {
                output.write("Rule execution successful, nothing to report\n");
            } else {
                output.write("*** WARNING *** Rule has info, warnings or errors, see log for report\n");
            }
            
            for(EvaluationResult.LogMessage msg : r.getLogMessages()) {
                output.write("\t");
                output.write(msg.getLevel().toString());
                output.write("\t");
                output.write(msg.getMessage());
                output.write("\n");
            }
            output.write("\n");
        }
        /*
        final JSONWriter w = new JSONWriter(output);
        try {
            w.object();
            w.endObject();
        } catch (JSONException e) {
            throw new IOException(e.getClass().getSimpleName(), e);
        }
        */
    }
}
