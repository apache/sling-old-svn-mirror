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
package org.apache.sling.jcr.contentparser.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.jcr.contentparser.Content;

final class ContentImpl implements Content {
    
    private final String name;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Content> children = new LinkedHashMap<>();
    
    public ContentImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public Map<String, Content> getChildren() {
        return children;
    }

    @Override
    public Content getChild(String path) {
        String name = StringUtils.substringBefore(path, "/");
        Content child = children.get(name);
        if (child == null) {
          return null;
        }
        String remainingPath = StringUtils.substringAfter(path, "/");
        if (StringUtils.isEmpty(remainingPath)) {
          return child;
        }
        else {
          return child.getChild(remainingPath);
        }
    }

}
