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
package org.apache.sling.scripting.sightly.impl.html.dom.template;

import java.util.ArrayList;
import java.util.List;

public class TemplateElementNode extends TemplateNode {

    private final String name;

    private final boolean hasEndSlash;

    private boolean hasEndElement = false;

    private boolean hasStartElement = false;

    private final List<TemplateAttribute> attributes;

    private final List<TemplateNode> children = new ArrayList<TemplateNode>();

    public TemplateElementNode(final String name,
                               final boolean hasEndSlash,
                               final List<TemplateAttribute> attributes) {
        this.name = name;
        this.hasEndSlash = hasEndSlash;
        this.attributes = attributes;
    }

    public void setHasStartElement() {
        this.hasStartElement = true;
    }

    public void setHasEndElement() {
        this.hasEndElement = true;
    }

    public String getName() {
        return name;
    }

    public boolean isHasEndSlash() {
        return hasEndSlash;
    }

    public boolean isHasStartElement() {
        return hasStartElement;
    }

    public boolean isHasEndElement() {
        return hasEndElement;
    }

    public List<TemplateAttribute> getAttributes() {
        return attributes;
    }

    public void addChild(final TemplateNode node) {
        this.children.add(node);
    }

    public List<TemplateNode> getChildren() {
        return this.children;
    }
}
