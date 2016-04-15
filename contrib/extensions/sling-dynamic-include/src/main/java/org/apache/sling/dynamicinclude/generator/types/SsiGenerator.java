/*-
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.dynamicinclude.generator.types;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.dynamicinclude.generator.IncludeGenerator;

/**
 * Apache SSI include generator
 */
@Component
@Service
public class SsiGenerator implements IncludeGenerator {
    private static final String GENERATOR_NAME = "SSI";

    @Override
    public String getType() {
        return GENERATOR_NAME;
    }

    @Override
    public String getInclude(String url) {
        StringBuffer buf = new StringBuffer();
        buf.append("<!--#include virtual=\"");
        buf.append(escapeForApache(url));
        buf.append("\" -->");
        return buf.toString();
    }

    /**
     * Escapes $ to \$
     * 
     * @param url
     *            url to escape
     * @return escaped url
     */
    private Object escapeForApache(String url) {
        return url.replace("$", "\\$");
    }
}
