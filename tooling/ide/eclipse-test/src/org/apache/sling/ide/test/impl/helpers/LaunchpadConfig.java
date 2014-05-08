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
package org.apache.sling.ide.test.impl.helpers;

import java.net.URI;
import java.net.URISyntaxException;

public class LaunchpadConfig {

    private static final LaunchpadConfig INSTANCE = new LaunchpadConfig();

    public static LaunchpadConfig getInstance() {
        return INSTANCE;
    }

    /**
     * @return the configured launchpad port
     */
    public int getPort() {
        return Integer.getInteger("launchpad.http.port", 8080);
    }

    public String getUsername() {
        return "admin";
    }

    public String getPassword() {
        return "admin";
    }

    public String getHostname() {
        return "localhost";
    }

    public String getProtocol() {
        return "http";
    }

    public String getContextPath() {
        return "/";
    }

    public String getUrl() {
        try {
            return new URI(getProtocol(), null, getHostname(), getPort(), getContextPath(), null, null)
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
