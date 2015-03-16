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
package apps.sightlyperf.test;

import java.util.Iterator;

import javax.script.Bindings;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.scripting.sightly.pojo.Use;

public class Test implements Use {

    private String text = null;

    private String tag = null;

    private boolean includeChildren = false;

    private Iterator<Resource> children = null;

    public void init(Bindings bindings) {
        Resource resource = (Resource)bindings.get("resource");
        ValueMap properties = (ValueMap)bindings.get("properties");

        if (properties != null) {
            Object text = properties.get("text");
            if (text != null) {
                this.text = text.toString();
            }

            Object tag = properties.get("tag");
            if (tag != null) {
                this.tag = tag.toString();
            }

            Object includeChildren = properties.get("includeChildren");
            if (includeChildren != null) {
                this.includeChildren = Boolean.parseBoolean(includeChildren.toString());
                this.children = resource.listChildren();
            }
        }

        if (this.text == null) {
            this.text = resource.getPath();
        }
    }

    public String getText() {
        return this.text;
    }

    public String getTag() {
        return tag;
    }

    public String getStartTag() {
        if (tag == null) {
            return null;
        }
        return "<" + tag + ">";
    }

    public String getEndTag() {
        if (tag == null) {
            return null;
        }
        return "</" + tag + ">";
    }

    public boolean getIncludeChildren() {
        return includeChildren;
    }

    public Iterator<Resource> getChildren() {
        return this.children;
    }
}