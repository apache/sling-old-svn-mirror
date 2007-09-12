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
package org.apache.sling.core.components;

import java.util.Collections;
import java.util.Enumeration;

import org.apache.sling.component.Component;
import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentExtension;
import org.apache.sling.component.Content;

/**
 * The <code>BaseComponent</code> TODO
 */
public abstract class BaseComponent implements Component {

    private ComponentContext context;
    private String componentId;
    private String contentClassName;

    protected BaseComponent() {
        this(null);
    }

    protected BaseComponent(String componentId) {
        // default the component ID to the fully qualified name of the class
        this.componentId = (componentId != null)
                ? componentId
                : this.getClass().getName();
    }

    public String getContentClassName() {
        return this.contentClassName;
    }

    public Content createContentInstance() {
        // TODO: this aint good :-)
        try {
            return (Content) Class.forName(this.getContentClassName(), true, this.getClass().getClassLoader()).newInstance();
        } catch (Throwable t) {
            // TODO: log error
        }

        // fail with null
        return null;
    }

    public ComponentExtension getExtension(String name) {
        return null;
    }

    public Enumeration<ComponentExtension> getExtensions() {
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    public ComponentContext getComponentContext() {
        return this.context;
    }

    public String getId() {
        return this.componentId;
    }

    public void init(ComponentContext context) {
        this.context = context;
        this.doInit();
    }

    public void destroy() {
        // nothing to do here, overwrite if needed
    }

    protected abstract void doInit();

    protected void setContentClassName(String contentClassName) {
        this.contentClassName = contentClassName;
    }

    protected void setComponentId(String componentId) {
        this.componentId = componentId;
    }
}
