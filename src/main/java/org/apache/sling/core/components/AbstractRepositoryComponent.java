/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.components;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.sling.component.ComponentExtension;

/**
 * The <code>BaseComponent</code> TODO
 * 
 * @ocm.mapped jcrNodeType="sling:Component" discriminator="false" extend=""
 */
public abstract class AbstractRepositoryComponent extends BaseComponent {

    /** @ocm.field path="true" */
    private String path;

    private Map extensions;

    public ComponentExtension getExtension(String name) {
        return (ComponentExtension) extensions.get(name);
    }

    public Enumeration getExtensions() {
        return new IteratorEnumeration(extensions.values().iterator());
    }

    // ---- mapping support

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @param componentId
     * @ocm.field jcrName="jcr:uuid"
     */
    public void setId(String componentId) {
        super.setComponentId(componentId);
    }

    /**
     * @ocm.field jcrName="sling:contentClass"
     */
    public void setContentClassName(String contentClassName) {
        super.setContentClassName(contentClassName);
    }

    /**
     * @param extensions
     * @ocm.collection jcrName="sling:extensions"
     *                 jcrNodeType="sling:ExtensionList"
     *                 elementClassName="org.apache.sling.core.components.extensions.AbstractExtension"
     */
    public void setExtensionCollection(Collection extensions) {
        Map extensionMap = new HashMap();
        if (extensions != null) {
            for (Iterator ei = extensions.iterator(); ei.hasNext();) {
                ComponentExtension ce = (ComponentExtension) ei.next();
                extensionMap.put(ce.getName(), ce);
            }
        }
        this.extensions = extensionMap;
    }

    public Collection getExtensionCollection() {
        return extensions.values();
    }
}
