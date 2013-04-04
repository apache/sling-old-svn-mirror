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
package org.apache.sling.sample.slingshot.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.sample.slingshot.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample for observation which adds tags to new resources.
 */
@Component(immediate=true)
@Service(value=EventHandler.class)
@Properties({
   @Property(name="service.description",
             value="Apache Sling - Slingshot Tagging Service"),
   @Property(name=EventConstants.EVENT_TOPIC, value=org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED)
})
public class AutomaticTaggingService
    implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private final Random random = new Random(System.currentTimeMillis());

    private final String MIXIN_TYPE_PROPERTY = "jcr:mixinTypes";
    private final String MIXIN_TYPE_PHOTO = "slingshot:Photo";

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final Event event) {
        final String path = (String)event.getProperty(org.apache.sling.api.SlingConstants.PROPERTY_PATH);
        if ( path != null && path.startsWith(Constants.ALBUMS_ROOT) ) {
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                final Resource r = resolver.getResource(path);
                if ( r != null && r.isResourceType(Constants.RESOURCETYPE_PHOTO) ) {
                    final ModifiableValueMap mvm = r.adaptTo(ModifiableValueMap.class);
                    if ( mvm != null ) {
                        String[] types = mvm.get(MIXIN_TYPE_PROPERTY, String[].class);
                        if ( types == null ) {
                            mvm.put(MIXIN_TYPE_PROPERTY, MIXIN_TYPE_PHOTO);
                        } else {
                            String[] newTypes = new String[types.length + 1];
                            System.arraycopy(types, 0, newTypes, 0, types.length);
                            newTypes[types.length] = MIXIN_TYPE_PHOTO;
                            mvm.put(MIXIN_TYPE_PROPERTY, newTypes);
                        }

                        final int tagsValue = this.random.nextInt(8);
                        final List<String> tags = new ArrayList<String>();
                        if ( (tagsValue & 1) == 1 ) {
                            tags.add("ApacheCon");
                        }
                        if ( (tagsValue & 2) == 2 ) {
                            tags.add("Vacation");
                        }
                        if ( (tagsValue & 4) == 4 ) {
                            tags.add("Cool");
                        }
                        mvm.put(Constants.PROPERTY_SLINGSHOT_TAGS, tags.toArray(new String[tags.size()]));
                        try {
                            resolver.commit();
                        } catch (final PersistenceException e) {
                            // we just ignore this for now
                            logger.info("Unable to add tags to photo: " + r.getPath(), e);
                        }
                    }
                }
            } catch (final LoginException e) {
                // this should never happen, therefore we ignore
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
            }
        }
    }
}
