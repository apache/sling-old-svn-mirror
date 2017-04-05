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
package org.apache.sling.engine.impl;

import org.apache.sling.engine.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The implementation of the settings service has moved to the
 * settings module. This is just a wrapper for compatibility.
 *
 * @deprecated
 */
@Component(service = SlingSettingsService.class)
@Deprecated
public class SlingSettingsServiceImpl
    implements SlingSettingsService {

    @Reference
    private org.apache.sling.settings.SlingSettingsService settingsService;

    /**
     * @see org.apache.sling.engine.SlingSettingsService#getSlingId()
     */
    @Override
    public String getSlingId() {
        return this.settingsService.getSlingId();
    }
}
