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
package org.apache.sling.content.jcr;

/**
 * The <code>BaseContent</code> class extends the {@link SimpleContent} class
 * adding support for mapping of nodes of type <code>sling:Content</code>.
 * This base class may be used by all classes supporting nodes whose node type
 * extends the <code>sling:Content</code> node type. There is however no
 * requirement to extend this class.
 * <p>
 * This class is defined with a content mapping for the
 * <code>sling:Content</code> node type mapping the contents of the
 * <code>sling:componentId</code> property to the
 * {@link #getComponentId() component ID} field.
 * <p>
 * Although this class is theoretically a complete implementation of the
 * <code>Content</code> interface, it is still marked <i>abstract</i> as on
 * its own this class has no use.
 * 
 * @ocm.mapped jcrNodeType="sling:Content" discriminator="false"
 */
public abstract class BaseContent extends SimpleContent {

    /**
     * The ID of the Component responsible for handling action and rendering
     * this content instance.
     * 
     * @ocm.field jcrName="sling:componentId"
     */
    private String componentId;

    /**
     * Returns the ID of the <code>Component</code> responsible to handle
     * actions for this content and for redering this content.
     * 
     * @return The ID of the action handling and rendering
     *         <code>Component</code>.
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * Sets the <code>Component</code> to handle actions on behalf of this
     * content and to render this content. This method is mainly used by the
     * mapping functionality to fill the component ID field from the
     * <code>sling:componentID</code> property.
     * <p>
     * This method is not part of the public API of this class and therefore not
     * intended to be used by client code or extending classes. It is handled
     * internally by the Content Management functionality.
     * 
     * @param componentId The ID of the component to set to handle actions and
     *            render this content.
     */
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }
}
