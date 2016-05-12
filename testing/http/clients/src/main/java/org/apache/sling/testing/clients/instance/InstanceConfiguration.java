/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.instance;

import java.net.URI;

/**
 * Configuration of a single instance instance.
 */
public class InstanceConfiguration {

    private URI url;
    private final String runmode;

    public InstanceConfiguration(final URI url, final String runmode) {
        this.url = url;
        this.runmode = runmode;
    }

    public URI getUrl() {
        return this.url;
    }

    public String getRunmode() {
        return runmode;
    }
}