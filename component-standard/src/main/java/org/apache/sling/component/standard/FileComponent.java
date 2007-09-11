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
package org.apache.sling.component.standard;

import java.io.IOException;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestDispatcher;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.core.components.BaseComponent;

/**
 * The <code>FileComponent</code> TODO
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description"
 *          value="Component to handle nt:file content"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 */
public class FileComponent extends BaseComponent {

    public static final String ID = FileComponent.class.getName();

    {
        this.setContentClassName(FileContent.class.getName());
        this.setComponentId(ID);
    }

    public Content createContentInstance() {
        return new FileContent();
    }

    // nothing to do
    protected void doInit() {
    }

    /**
     * @see org.apache.sling.core.component.Component#service(org.apache.sling.core.component.ComponentRequest, org.apache.sling.core.component.ComponentResponse)
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        // just render the child content
        Content jcrContent = request.getContent("jcr:content");
        if (jcrContent != null) {
            ComponentRequestDispatcher crd = this.getComponentContext().getRequestDispatcher(
                jcrContent);
            crd.include(request, response);
        } else {
            throw new ComponentException("No content");
        }
    }
}
