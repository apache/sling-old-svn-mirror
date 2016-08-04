/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.runtime;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.scripting.sightly.Record;
import org.apache.sling.scripting.sightly.render.AbstractRuntimeObjectModel;

public class SlingRuntimeObjectModel extends AbstractRuntimeObjectModel {

    protected Object getProperty(Object target, Object propertyObj) {
        String property = toString(propertyObj);
        if (StringUtils.isEmpty(property)) {
            throw new IllegalArgumentException("Invalid property name");
        }
        if (target == null) {
            return null;
        }
        Object result = null;
        if (target instanceof Map) {
            result = getMapProperty((Map) target, property);
        }
        if (result == null && target instanceof Record) {
            result = ((Record) target).getProperty(property);
        }
        if (result == null) {
            result = getObjectProperty(target, property);
        }
        if (result == null && target instanceof Adaptable) {
            ValueMap valueMap = ((Adaptable) target).adaptTo(ValueMap.class);
            if (valueMap != null) {
                result = valueMap.get(property);
            }
        }
        return result;
    }

}
