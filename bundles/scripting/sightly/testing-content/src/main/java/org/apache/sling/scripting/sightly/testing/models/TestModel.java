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
package org.apache.sling.scripting.sightly.testing.models;

import java.util.Date;
import java.util.Iterator;
import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

@Model(adaptables = Resource.class)
public class TestModel {

    @Inject
    private String text;

    @Inject @Optional
    private String tag;

    @Inject @Default(booleanValues = false)
    private boolean includeChildren;

    private Resource resource;

    public TestModel(Resource resource) {
        this.resource = resource;
    }

    public String getText() {
        return text;
    }

    public String getTag() {
        return tag;
    }

    public boolean getIncludeChildren() {
        return includeChildren;
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

    public Iterator<Resource> getChildren() {
        return resource.listChildren();
    }

    public Date getDate() {
        return new Date();
    }

}
