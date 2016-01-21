/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler;

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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;
import org.apache.sling.scripting.sightly.impl.engine.SightlyScriptEngineFactory;
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

    private Map<String, SightlyScriptMetaInfo> slyScriptsMap = new ConcurrentHashMap<String, SightlyScriptMetaInfo>();
    private Map<String, Long> slyJavaUseMap = new ConcurrentHashMap<String, Long>();
    private ServiceRegistration eventHandlerServiceRegistration;

    @Reference
    private ResourceResolverFactory rrf = null;

    @Reference
    private SightlyEngineConfiguration sightlyEngineConfiguration = null;

    /**
     * Returns the last modified date for a Sightly script.
     *
     * @param script the script's full path
     * @return the script's last modified date or 0 if there's no information about the script
     */
    public long getLastModifiedDateForScript(String script) {
        SightlyScriptMetaInfo sightlyScriptMetaInfo = slyScriptsMap.get(script);
        if (sightlyScriptMetaInfo != null) {
            return sightlyScriptMetaInfo.lastModified;
        }
        return 0;
    }

    public String getScriptEncoding(String script) {
        SightlyScriptMetaInfo sightlyScriptMetaInfo = getScript(script);
        if (sightlyScriptMetaInfo != null) {
            return sightlyScriptMetaInfo.encoding;
        }
        return sightlyEngineConfiguration.getEncoding();
    }

    /**
     * Returns the last modified date for a Java Use-API object stored in the repository.
     *
     * @param className the full path of the file defining the Java Use-API object
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
    @SuppressWarnings(value = {"unused", "unchecked"})
    protected void activate(ComponentContext componentContext) {
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            StringBuilder eventHandlerFilteredPaths = new StringBuilder("(|");
            String[] searchPaths = adminResolver.getSearchPath();
            for (String sp : searchPaths) {
                // Sightly script changes
                eventHandlerFilteredPaths.append("(path=").append(sp).append("**/*.").append(SightlyScriptEngineFactory.EXTENSION).append(
                        ")");
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
    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext componentContext) {
        if (eventHandlerServiceRegistration != null) {
            eventHandlerServiceRegistration.unregister();
        }
    }

    private void processEvent(Event event) {
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        String topic = event.getTopic();
        if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(topic) || SlingConstants.TOPIC_RESOURCE_CHANGED.equals(topic)) {
            if (path.endsWith(".java")) {
                slyJavaUseMap.put(Utils.getJavaNameFromPath(path), System.currentTimeMillis());
            } else if (path.endsWith(SightlyScriptEngineFactory.EXTENSION)) {
                ResourceResolver resolver = null;
                String encoding = null;
                try {
                    resolver =  rrf.getAdministrativeResourceResolver(null);
                    ResourceMetadata scriptResourceMetadata = resolver.getResource(path).getResourceMetadata();
                    encoding = scriptResourceMetadata.getCharacterEncoding();
                } catch (LoginException e) {
                    // do nothing; we'll just return the default encoding
                    LOG.warn("Cannot read character encoding value for script " + path);
                } finally {
                    if (resolver != null) {
                        resolver.close();
                    }
                }
                if (StringUtils.isEmpty(encoding)) {
                    encoding = sightlyEngineConfiguration.getEncoding();
                }
                slyScriptsMap.put(path, new SightlyScriptMetaInfo(encoding, System.currentTimeMillis()));
            }
        } else if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(topic)) {
            if (path.endsWith(".java")) {
                slyJavaUseMap.remove(Utils.getJavaNameFromPath(path));
            } else if (path.endsWith(SightlyScriptEngineFactory.EXTENSION)) {
                slyScriptsMap.remove(path);
            }
        }
    }

    private SightlyScriptMetaInfo getScript(String script) {
        SightlyScriptMetaInfo sightlyScriptMetaInfo = null;
        if (StringUtils.isNotEmpty(script)) {
            sightlyScriptMetaInfo = slyScriptsMap.get(script);
            if (sightlyScriptMetaInfo == null) {
                ResourceResolver resolver = null;
                try {
                    resolver = rrf.getAdministrativeResourceResolver(null);
                    Resource scriptResource = resolver.getResource(script);
                    if (scriptResource == null) {
                        return null;
                    }
                    ResourceMetadata scriptResourceMetadata = scriptResource.getResourceMetadata();
                    String encoding = scriptResourceMetadata.getCharacterEncoding();
                    if (StringUtils.isEmpty(encoding)) {
                        encoding = sightlyEngineConfiguration.getEncoding();
                    }
                    sightlyScriptMetaInfo = new SightlyScriptMetaInfo(encoding, scriptResourceMetadata.getModificationTime());
                    slyScriptsMap.put(script, sightlyScriptMetaInfo);
                } catch (LoginException e) {
                    // do nothing; we'll just return the default encoding
                    LOG.warn("Cannot read character encoding value for script " + script);
                } finally {
                    if (resolver != null) {
                        resolver.close();
                    }
                }
            }
        }
        return sightlyScriptMetaInfo;
    }

    private static class SightlyScriptMetaInfo {
        String encoding;
        long lastModified;

        public SightlyScriptMetaInfo(String encoding, long lastModified) {
            this.encoding = encoding;
            this.lastModified = lastModified;
        }
    }

}
