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
package org.apache.sling.models.testmodels.classes.constructorinjection;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.testmodels.classes.ChildModel;

@Model(adaptables = Resource.class)
public class ParentModel {

    @Inject
    public ParentModel(@Named("firstChild") ChildModel firstChild, @Named("secondChild") List<ChildModel> grandChildren,
            @Named("emptyChild") List<ChildModel> emptyGrandChildren) {
        this.firstChild = firstChild;
        this.grandChildren = grandChildren;
        this.emptyGrandChildren = emptyGrandChildren;
    }

    private ChildModel firstChild;

    private List<ChildModel> grandChildren;

    private List<ChildModel> emptyGrandChildren;

    public ChildModel getFirstChild() {
        return firstChild;
    }

    public List<ChildModel> getGrandChildren() {
        return grandChildren;
    }

    public List<ChildModel> getEmptyGrandChildren() {
        return emptyGrandChildren;
    }
}
