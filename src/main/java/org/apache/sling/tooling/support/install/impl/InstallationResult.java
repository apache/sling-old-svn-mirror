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
package org.apache.sling.tooling.support.install.impl;

import java.io.Writer;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

public class InstallationResult {

    private final boolean status;
    private final String message;

    public InstallationResult(boolean status, String message) {
        this.status = status;
        this.message = message;
    }

    public void render(Writer out) {

        try {
            JSONWriter writer = new JSONWriter(out);
            writer.object();
            writer.key("status").value(status ? "OK" : "FAILURE");
            if (!StringUtils.isEmpty(message)) {
                writer.key("message").value(message);
            }
            writer.endObject();
        } catch (JSONException e) {
            // never happens
            throw new RuntimeException(e);
        }
    }

}
