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
package org.apache.sling.component.standard;

import java.io.IOException;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestDispatcher;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.core.components.BaseComponent;

/**
 * The <code>ReferenceComponent</code> TODO
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description"
 *          value="Component to handle sling:Reference content"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 */
public class ReferenceComponent extends BaseComponent {

    public static final String ID = ReferenceComponent.class.getName();

    {
        this.setContentClassName(ReferenceContent.class.getName());
        this.setComponentId(ID);
    }

    /**
     * @see org.apache.sling.core.components.BaseComponent#createContentInstance()
     */
    public Content createContentInstance() {
        return new ReferenceContent();
    }

    /**
     * @see org.apache.sling.core.components.BaseComponent#doInit()
     */
    protected void doInit() {
        // nothing to do
    }

    /**
     * @see org.apache.sling.core.component.Component#service(org.apache.sling.core.component.ComponentRequest, org.apache.sling.core.component.ComponentResponse)
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        final ReferenceContent content = (ReferenceContent)request.getContent();
        final String path = content.getReference();

        // just forward to the referenced content
        Content jcrContent = request.getContent(path);
        if (jcrContent != null) {
            ComponentRequestDispatcher crd = this.getComponentContext().getRequestDispatcher(
                jcrContent);
            crd.include(request, response);
        } else {
            throw new ComponentException("No content for path " + path);
        }
    }
}
