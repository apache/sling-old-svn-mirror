/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.scripting.sightly.impl.engine.compiled.SourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service({ResourceBackedPojoChangeMonitor.class, ResourceChangeListener.class})
@Properties({
    @Property(name = ResourceChangeListener.PATHS, value = "**/*.java"),
    @Property(name = ResourceChangeListener.CHANGES, value = {"ADDED", "CHANGED", "REMOVED"})
})
public class ResourceBackedPojoChangeMonitor implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceBackedPojoChangeMonitor.class);

    private Map<String, Long> slyJavaUseMap = new ConcurrentHashMap<>();

    @Reference
    private SightlyEngineConfiguration sightlyEngineConfiguration = null;

    /**
     * Returns the last modified date for a Java Use-API object stored in the repository.
     *
     * @param className the fully qualified class name
     * @return the Java Use-API file's last modified date or 0 if there's no information about this file
     */
    public long getLastModifiedDateForJavaUseObject(String className) {
        if (className == null) {
            return 0;
        }
        Long date = slyJavaUseMap.get(className);
        return date != null ? date : 0;
    }

    public void clearJavaUseObject(String className) {
        if (StringUtils.isNotEmpty(className)) {
            slyJavaUseMap.remove(className);
        }
    }

    @Override
    public void onChange(@Nonnull List<ResourceChange> changes) {
        for (ResourceChange change : changes) {
            String path = change.getPath();
            ResourceChange.ChangeType changeType = change.getType();
            SourceIdentifier sourceIdentifier = new SourceIdentifier(sightlyEngineConfiguration, path);
            switch (changeType) {
                case ADDED:
                case CHANGED:
                    slyJavaUseMap.put(sourceIdentifier.getFullyQualifiedClassName(), System.currentTimeMillis());
                    break;
                case REMOVED:
                    slyJavaUseMap.remove(sourceIdentifier.getFullyQualifiedClassName());
                    break;
                default:
                    break;
            }
            LOG.debug("Java Use Object {} was {}.", path, changeType.toString());
        }
    }
}
