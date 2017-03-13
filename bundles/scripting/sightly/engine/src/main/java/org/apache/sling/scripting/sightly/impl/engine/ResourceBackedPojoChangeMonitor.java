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

import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.scripting.sightly.SightlyException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {ResourceBackedPojoChangeMonitor.class, ResourceChangeListener.class},
        property = {
                ResourceChangeListener.PATHS + "=glob:**/*.java",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED",
        }
)
public class ResourceBackedPojoChangeMonitor implements ResourceChangeListener, ExternalResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceBackedPojoChangeMonitor.class);

    private Map<String, Long> slyJavaUseMap = new ConcurrentHashMap<>();

    @Reference
    private SightlyEngineConfiguration sightlyEngineConfiguration = null;

    /**
     * Records the usage of the Use-object available at the provided {@code path}.
     *
     * @param path      the path of the Use-object
     * @param timestamp the timestamp when the object identified by the resource from {@code path} was last modified
     */
    public void recordLastModifiedTimestamp(String path, long timestamp) {
        if (path == null) {
            throw new SightlyException("Path value cannot be null.");
        }
        slyJavaUseMap.put(path, timestamp);
    }

    /**
     * Returns the last modified date for a Java Use-API object stored in the repository.
     *
     * @param path the {@code Resource} path of the Use-object
     * @return the Java Use-API file's last modified date or 0 if there's no information about this file
     */
    public long getLastModifiedDateForJavaUseObject(String path) {
        if (path == null) {
            return 0;
        }
        Long date = slyJavaUseMap.get(path);
        return date != null ? date : 0;
    }

    @Override
    public void onChange(@Nonnull List<ResourceChange> changes) {
        for (ResourceChange change : changes) {
            String path = change.getPath();
            ResourceChange.ChangeType changeType = change.getType();
            switch (changeType) {
                case CHANGED:
                    if (slyJavaUseMap.containsKey(path)) {
                        slyJavaUseMap.put(path, System.currentTimeMillis());
                    }
                    break;
                case REMOVED:
                    if (slyJavaUseMap.containsKey(path)) {
                        slyJavaUseMap.remove(path);
                    }
                    break;
                default:
                    break;
            }
            LOG.debug("Java Use Object {} was {}.", path, changeType.toString());
        }
    }
}
