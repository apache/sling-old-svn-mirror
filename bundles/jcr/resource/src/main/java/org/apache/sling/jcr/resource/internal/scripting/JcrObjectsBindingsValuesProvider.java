/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.resource.internal.scripting;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.script.Bindings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.osgi.framework.Constants;

/**
 * BindingsValuesProvider for currentNode and currentSession object.
 */
@Component
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling CurrentNode BindingsValuesProvider"),
        @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation") })
public class JcrObjectsBindingsValuesProvider implements BindingsValuesProvider {

    private static final String PROP_CURRENT_NODE = "currentNode";
    private static final String PROP_CURRENT_SESSION = "currentSession";

    /**
     * {@inheritDoc}
     */
    public void addBindings(final Bindings bindings) {
        final Resource resource = (Resource) bindings.get("resource");
        if (resource != null) {
            final Node node = resource.adaptTo(Node.class);
            if (node != null) {
                bindings.put(PROP_CURRENT_NODE, node);
            }
            if (bindings.get(PROP_CURRENT_SESSION) == null) {
                final Session session = resource.getResourceResolver().adaptTo(
                        Session.class);
                if (session != null) {
                    bindings.put(PROP_CURRENT_SESSION, session);
                }
            }
        }
    }
}
