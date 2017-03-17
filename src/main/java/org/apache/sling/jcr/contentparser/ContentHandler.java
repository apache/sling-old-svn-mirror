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
package org.apache.sling.jcr.contentparser;

import java.util.Map;

/**
 * Handler that gets notified while parsing content with {@link ContentParser}.
 * The resources are always reported in order of their paths as found in the content fragment.
 * Parents are always reported before their children.
 */
public interface ContentHandler {

    /**
     * Resource found in parsed content.
     * @param path Path of resource inside the content fragment. The root resource from the content fragment has a path "/".
     * @param properties Resource properties
     */
    void resource(String path, Map<String,Object> properties);
    
}
