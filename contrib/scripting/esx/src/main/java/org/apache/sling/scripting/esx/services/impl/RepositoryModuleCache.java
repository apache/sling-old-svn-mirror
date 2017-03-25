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
package org.apache.sling.scripting.esx.services.impl;

import java.util.HashMap;
import java.util.Map;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.esx.ScriptModuleCache;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = {EventHandler.class, ScriptModuleCache.class})
@Property(name = org.osgi.service.event.EventConstants.EVENT_TOPIC,
        value = {SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED})
public class RepositoryModuleCache implements ScriptModuleCache {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, ScriptObjectMirror> cache = new HashMap<String, ScriptObjectMirror>();

    @Override
    public void put(String module, ScriptObjectMirror script) {
        log.debug("putting module with absolute path {} in the cache", module);
        cache.put(module, script);
    }

    @Override
    public ScriptObjectMirror get(String module) {
        return cache.get(module);
    }

    @Override
    public ScriptObjectMirror get(Resource resource) {
        return cache.get(resource.getPath());
    }

    @Override
    public boolean flush(String module) {
        Object res = cache.remove(module);
        if (res != null) {
            log.debug("{} flushed from cache", module);
        }
        return (true);
    }

    @Override
    public void handleEvent(Event event) {
        final String eventPath = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        if (cache.remove(eventPath) != null) {
            log.info("{} was removed from cache", eventPath);
        }

    }
}