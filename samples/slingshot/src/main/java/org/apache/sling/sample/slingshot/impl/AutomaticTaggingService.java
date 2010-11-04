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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.sample.slingshot.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * This is a samle for observation which adds tags to new resources.
 */
@Component(immediate=true)
@Service(value=EventHandler.class)
@Properties({
   @Property(name="service.description",
             value="Apache Sling - Slingshot Tagging Service"),
   @Property(name="event.topics", value=org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED)
})
public class AutomaticTaggingService
    implements EventHandler {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private final Random random = new Random(System.currentTimeMillis());

    public void handleEvent(final Event event) {
        final String path = (String)event.getProperty(org.apache.sling.api.SlingConstants.PROPERTY_PATH);
        if ( path != null && path.startsWith(Constants.ALBUMS_ROOT) ) {
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                final Resource r = resolver.getResource(path);
                if ( r != null && r.isResourceType(Constants.RESOURCETYPE_PHOTO) ) {
                    // to add the mixin node type, we have to adopt to a node
                    final Node node = r.adaptTo(Node.class);
                    if ( node != null && !node.isNodeType("slingshot:Photo")) {
                        node.addMixin("slingshot:Photo");
                        node.getSession().save();
                        final PersistableValueMap pvm = r.adaptTo(PersistableValueMap.class);
                        if ( pvm != null ) {
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
                            pvm.put("slingshot:tags", tags.toArray(new String[tags.size()]));
                            try {
                                pvm.save();
                            } catch (PersistenceException e) {
                                // we just ignore this for now
                            }
                        }
                    }
                }

            } catch (final RepositoryException e) {
                // this should never happen, therefore we ignore
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
