/*
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
package org.apache.sling.discovery.commons.providers.spi.base;

import java.net.URL;
import java.util.Set;

import org.apache.sling.settings.SlingSettingsService;

public class DummySlingSettingsService implements SlingSettingsService {

    private String slingId;
    private String slingHome;

    public DummySlingSettingsService(String slingId) {
        this(slingId, "/slingHome/"+slingId);
    }
    
    public DummySlingSettingsService(String slingId, String slingHome) {
        this.slingId = slingId;
        this.slingHome = slingHome;
    }
    
    @Override
    public String getAbsolutePathWithinSlingHome(String relativePath) {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public String getSlingId() {
        return slingId;
    }

    @Override
    public String getSlingHomePath() {
        return slingHome;
    }

    @Override
    public URL getSlingHome() {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public Set<String> getRunModes() {
        throw new IllegalStateException("not yet impl");
    }

}
