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
package org.apache.sling.samples.fling.form;

import org.apache.sling.api.resource.ValueMap;

public class CommentForm extends BaseForm {

    public static final String RESOURCE_TYPE = "fling/form/comment";

    public CommentForm(final ValueMap parameters) {
        populate(parameters);
    }

    private void populate(final ValueMap parameters) {
        put("name", parameters.get("name", String.class));
        put("comment", parameters.get("comment", String.class));
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String toString() {
        return fields.toString();
    }

}
