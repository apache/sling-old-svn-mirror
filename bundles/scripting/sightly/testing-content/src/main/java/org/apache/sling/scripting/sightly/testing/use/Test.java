/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.testing.use;

import java.util.Iterator;
import javax.script.Bindings;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.pojo.Use;

public class Test implements Use {

    public static final String PROPERTIES = "properties";
    public static final String TEXT = "text";
    public static final String TAG = "tag";
    public static final String INCLUDE_CHIDLREN = "includeChildren";

    private String text = null;

    private String tag = null;

    private boolean includeChildren = false;

    private Iterator<Resource> children = null;

    public void init(Bindings bindings) {
        Resource resource = (Resource)bindings.get(SlingBindings.RESOURCE);
        ValueMap properties = (ValueMap)bindings.get(PROPERTIES);

        if (properties != null) {
            text = properties.get(TEXT, resource.getPath());
            tag = properties.get(TAG, String.class);
            includeChildren = properties.get(INCLUDE_CHIDLREN, false);
            if (includeChildren) {
                children = resource.listChildren();
            }
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