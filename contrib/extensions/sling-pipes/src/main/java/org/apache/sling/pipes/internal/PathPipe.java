/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.pipes.internal;

import java.util.Collections;
import java.util.Iterator;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.apache.sling.query.util.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.jcr.resource.JcrResourceConstants.NT_SLING_FOLDER;

/**
 * creates or get given expression's path and returns corresponding resource
 * this pipe can be configured with the following properties:
 * <ul>
 *     <li><code>nodeType</code> resource type with which the leaf node of the created path will be created</li>
 *     <li><code>intermediateType</code> resource type with which intermediate nodse of the created path will be created</li>
 *     <li><code>autosave</code> flag indicating wether this pipe should triggers a commit at the end of the execution</li>
 * </ul>
 */
public class PathPipe extends BasePipe {

    public static final String RESOURCE_TYPE = RT_PREFIX + "path";
    public static final String PN_RESOURCETYPE = "nodeType";
    public static final String PN_INTERMEDIATE = "intermediateType";
    public static final String PN_AUTOSAVE = "autosave";
    public static final String SLASH = "/";

    String resourceType;
    String intermediateType;
    boolean autosave;

    private final Logger logger = LoggerFactory.getLogger(PathPipe.class);

    public PathPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        resourceType = properties.get(PN_RESOURCETYPE, NT_SLING_FOLDER);
        intermediateType = properties.get(PN_INTERMEDIATE, NT_SLING_FOLDER);
        autosave = properties.get(PN_AUTOSAVE, true);
    }

    @Override
    public boolean modifiesContent() {
        return true;
    }

    @Override
    public Iterator<Resource> getOutput() {
        Iterator<Resource> output = Collections.emptyIterator();
        String expression = getExpr();
        try {
            String path = expression.startsWith(SLASH) ? expression : getInput().getPath() + SLASH + expression;
            output = IteratorUtils.singleElementIterator(ResourceUtil.getOrCreateResource(resolver, path, resourceType, intermediateType, autosave));
        } catch (PersistenceException e){
            logger.error ("Not able to create path {}", expression, e);
        }
        return output;
    }
}
