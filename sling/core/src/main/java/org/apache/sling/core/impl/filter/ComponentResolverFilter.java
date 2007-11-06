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
package org.apache.sling.core.impl.filter;

import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.component.Component;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.core.components.DefaultComponent;
import org.apache.sling.core.components.ErrorHandlerComponent;
import org.apache.sling.core.impl.ContentData;
import org.apache.sling.core.impl.RequestData;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>ComponentResolverFilter</code> TODO
 *
 * @scr.component immediate="true" label="%resolver.name"
 *      description="%resolver.description"
 * @scr.property name="service.description" value="Component Resolver Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="component" private="true"
 * @scr.property name="filter.order" value="-900" type="Integer" private="true"
 * @scr.service
 * @scr.reference name="Components" interface="org.apache.sling.component.Component"
 *                cardinality="0..n" policy="dynamic"
 */
public class ComponentResolverFilter extends ComponentBindingFilter {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ComponentResolverFilter.class);

    /**
     * @scr.property values.1="/apps/components" values.2="/libs/components"
     *               label="%resolver.path.name"
     *               description="%resolver.path.description"
     */
    public static final String PROP_PATH = "path";

    private String[] path;

    /**
     * @see org.apache.sling.core.component.ComponentFilter#doFilter(org.apache.sling.core.component.ComponentRequest, org.apache.sling.core.component.ComponentResponse, org.apache.sling.core.component.ComponentFilterChain)
     */
    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        ContentData contentData = RequestData.getRequestData(request).getContentData();

        if (contentData != null) {
            // 2.3 check Component
            Content content = contentData.getContent();
            String path = content.getPath();
            Component component = this.resolveComponent(content);
            if (component != null) {

                String compId = component.getId();
                log.debug("Using Component {} for {}", compId, path);
                contentData.setComponent(component);
                filterChain.doFilter(request, response);
                return;

            }
        } else {
            log.error("ComponentResolver: No Content data in request {}",
                request.getRequestURI());
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cannot handle");
    }

    protected boolean accept(Component component) {
        return !(component instanceof ErrorHandlerComponent);
    }

    private Component resolveComponent(Content content) {
        // get the component id
        String compId = content.getComponentId();

        // if none, try the path of the content as its component id,
        // this allows direct addressing of components
        if (compId == null) {
            log.debug("resolveComponent: Content {} has no componentid, trying path", content.getPath());
            compId = content.getPath();
        }

        Component component = this.getComponent(compId);
        if (component != null) {
            return component;
        }

        // if the component ID might be a realtive path name, check with path
        if (!compId.startsWith("/")) {

            // apply any path prefixes
            if (this.path != null) {

                // might be a type name with namespace
                String relId = compId.replace(':', '/');

                for (int i=0; i < this.path.length; i++) {
                    String checkid = this.path[i] + relId;
                    component = this.getComponent(checkid);
                    if (component != null) {
                        return component;
                    }
                }

            }

        } else {
            // absolute path name: remove leading slash for further checks
            compId = compId.substring(1);
        }

        // if the path is mapped from a class name, convert the slashes
        // to dots to get a potentially fully qualified class name
        // again, this allows direct addressing of components
        compId = compId.replace('/', '.');
        component = this.getComponent(compId);
        if (component != null) {
            return component;
        }

        // next we try a class name mapping convention of the content class
        compId = content.getClass().getName();
        component = this.getComponent(compId);
        if (component != null) {
            return component;
        }

        // check whether we have Content suffix to remove
        if (compId.endsWith("Content")) {
            compId = compId.substring(0, compId.length()-"Content".length());
            component = this.getComponent(compId);
            if (component != null) {
                return component;
            }
        }

        // add "Component" suffix and check again
        compId += "Component";
        component = this.getComponent(compId);
        if (component != null) {
            return component;
        }

        // use default component
        component = this.getComponent(DefaultComponent.ID);
        if (component != null) {
            return component;
        }

        // we exhausted all possibilities and finally fail
        log.error("resolveComponent: Could not resolve a component for {}", content.getPath());
        return null;
    }

    //---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        Dictionary props = context.getProperties();

        Object pathObject = props.get(PROP_PATH);
        if (pathObject instanceof String[]) {
            this.path = (String[]) pathObject;
            for (int i=0; i < this.path.length; i++) {
                // ensure leading slash
                if (!this.path[i].startsWith("/")) {
                    this.path[i] = "/" + this.path[i];
                }
                // ensure trailing slash
                if (!this.path[i].endsWith("/")) {
                    this.path[i] += "/";
                }
            }
        } else {
            this.path = null;
        }
    }
}
