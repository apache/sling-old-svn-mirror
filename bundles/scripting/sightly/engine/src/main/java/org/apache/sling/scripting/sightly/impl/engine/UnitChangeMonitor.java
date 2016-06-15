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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.scripting.sightly.impl.engine.compiled.SourceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(UnitChangeMonitor.class)
public class UnitChangeMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(UnitChangeMonitor.class);

    private Map<String, Long> slyJavaUseMap = new ConcurrentHashMap<String, Long>();
    private ServiceRegistration eventHandlerServiceRegistration;

    @Reference
    private ResourceResolverFactory rrf = null;

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

    @Activate
    protected void activate(ComponentContext componentContext) {
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            StringBuilder eventHandlerFilteredPaths = new StringBuilder("(|");
            String[] searchPaths = adminResolver.getSearchPath();
            for (String sp : searchPaths) {
                // Sightly Java Use-API objects
                eventHandlerFilteredPaths.append("(path=").append(sp).append("**/*.java").append(")");
            }
            eventHandlerFilteredPaths.append(")");
            Dictionary eventHandlerProperties = new Hashtable();
            eventHandlerProperties.put(EventConstants.EVENT_FILTER, eventHandlerFilteredPaths.toString());
            eventHandlerProperties.put(EventConstants.EVENT_TOPIC, new String[]{SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants
                    .TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED});
            eventHandlerServiceRegistration = componentContext.getBundleContext().registerService(
                    EventHandler.class.getName(),
                    new EventHandler() {
                        @Override
                        public void handleEvent(Event event) {
                            processEvent(event);
                        }
                    },
                    eventHandlerProperties
            );
        } catch (LoginException e) {
            LOG.error("Unable to listen for change events.", e);
        } finally {
            if (adminResolver != null) {
                adminResolver.close();
            }
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        if (eventHandlerServiceRegistration != null) {
            eventHandlerServiceRegistration.unregister();
        }
    }

    private void processEvent(Event event) {
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        String topic = event.getTopic();
        LOG.debug("Received event {} for path {}.", topic, path);
        if (path.endsWith(".java")) {
            SourceIdentifier sourceIdentifier = new SourceIdentifier(sightlyEngineConfiguration, path);
            if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(topic) || SlingConstants.TOPIC_RESOURCE_CHANGED.equals(topic)) {
                LOG.debug("Java Use Object {} was {}.", path, topic);
                slyJavaUseMap.put(sourceIdentifier.getFullyQualifiedClassName(), System.currentTimeMillis());
            } else if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(topic)) {
                LOG.debug("Removed Java Use Object {} from UnitChangeMonitor cache.", path);
                slyJavaUseMap.remove(sourceIdentifier.getFullyQualifiedClassName());
            }
        }
    }
}
