/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.servlets.post;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Service interface which allows for custom node name generation for * resources.
 *
 */
public interface NodeNameGenerator {

    /**
     * Get the to-be-created node name from the request.
     *
     * @param request request
     * @param parentPath the path to the new node's parent
     * @param requirePrefix if true, ignore parameters which do not being with ./
     * @param defaultNodeNameGenerator the default node name generator
     *
     * @return the node name to be created or null if other NodeNameGenerators should be consulted
     */
    public String getNodeName(SlingHttpServletRequest request, String parentPath, boolean requirePrefix,
            NodeNameGenerator defaultNodeNameGenerator);
}
